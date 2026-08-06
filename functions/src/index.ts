import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { google, drive_v3 } from 'googleapis';
import * as crypto from 'crypto';

admin.initializeApp();
const db = admin.firestore();

// Environment configuration
const ROOT_FOLDER_ID = process.env.GOOGLE_DRIVE_ROOT_FOLDER_ID || '';
const TARGET_DRIVE_OWNER = 'scscpdata@gmail.com';

/**
 * Exponential backoff helper for resilient API requests
 */
export async function withExponentialBackoff<T>(
  fn: () => Promise<T>,
  maxRetries: number = 4,
  initialDelayMs: number = 1000
): Promise<T> {
  let delay = initialDelayMs;
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await fn();
    } catch (err: any) {
      if (attempt === maxRetries) {
        throw err;
      }
      const statusCode = err?.response?.status || err?.code;
      if (statusCode === 429 || (statusCode >= 500 && statusCode < 600) || err.code === 'ECONNRESET') {
        console.warn(`[DriveSync] Attempt ${attempt} failed with status ${statusCode}. Retrying in ${delay}ms...`);
        await new Promise((resolve) => setTimeout(resolve, delay));
        delay *= 2;
      } else {
        throw err;
      }
    }
  }
  throw new Error('Max retries exceeded');
}

/**
 * Initialize Google Drive Client using Service Account ADC credentials
 */
export function getDriveClient(): drive_v3.Drive {
  const auth = new google.auth.GoogleAuth({
    scopes: ['https://www.googleapis.com/auth/drive.file', 'https://www.googleapis.com/auth/drive'],
  });
  return google.drive({ version: 'v3', auth });
}

/**
 * Ensures a folder exists under parentFolderId, creating it idempotently if missing.
 */
export async function getOrCreateFolder(
  drive: drive_v3.Drive,
  folderName: String,
  parentFolderId: string
): Promise<string> {
  const sanitizedName = folderName.replace(/['"]/g, '').replace(/[/\\?%*:|"<>]/g, '_').trim();
  const query = `'${parentFolderId}' in parents and name = '${sanitizedName}' and mimeType = 'application/vnd.google-apps.folder' and trashed = false`;

  const response = await withExponentialBackoff(() =>
    drive.files.list({
      q: query,
      fields: 'files(id, name)',
      spaces: 'drive',
    })
  );

  if (response.data.files && response.data.files.length > 0) {
    return response.data.files[0].id!;
  }

  const createResponse = await withExponentialBackoff(() =>
    drive.files.create({
      requestBody: {
        name: sanitizedName,
        mimeType: 'application/vnd.google-apps.folder',
        parents: [parentFolderId],
      },
      fields: 'id',
    })
  );

  return createResponse.data.id!;
}

/**
 * Format payload into a readable CSV string
 */
export function generateSummaryCsv(payload: Record<string, any>): string {
  const lines: string[] = ['Question ID,Question Prompt,Response Value'];

  for (const [key, value] of Object.entries(payload)) {
    const sanitizedKey = `"${key.replace(/"/g, '""')}"`;
    const valString = typeof value === 'object' ? JSON.stringify(value) : String(value ?? '');
    const sanitizedVal = `"${valString.replace(/"/g, '""')}"`;
    lines.push(`${sanitizedKey},${sanitizedKey},${sanitizedVal}`);
  }

  return lines.join('\n');
}

/**
 * Firestore Trigger for Records-Only Flow: Fires when a document is created in records/{recordId}
 */
export const onRecordCreated = functions.firestore
  .document('records/{recordId}')
  .onCreate(async (snapshot, context) => {
    const recordData = snapshot.data();
    const recordId = context.params.recordId;

    console.log(`[RecordsSync] New record created: ${recordId}. Syncing to Google Drive...`);

    const drive = getDriveClient();
    const createdAt = recordData.createdAt ? new Date(recordData.createdAt) : new Date();
    const year = createdAt.getFullYear().toString();
    const city = recordData.city || 'ND';
    const userId = recordData.userId || recordData.createdBy || 'anonymous_user';
    const displayName = (recordData.displayName || recordData.surveyorName || 'Surveyor').replace(/[^a-zA-Z0-9_-]/g, '_');
    const userFolder = `${userId}_${displayName}`;
    const instrumentType = recordData.instrumentType || 'General_Survey';

    try {
      // 1. Build hierarchy: DCA/{YYYY}/{city}/{userId}_{displayName}/{instrumentType}/{recordId}/
      const rootFolderId = ROOT_FOLDER_ID || (await getOrCreateFolder(drive, 'DCA', 'root'));
      const yearFolderId = await getOrCreateFolder(drive, year, rootFolderId);
      const cityFolderId = await getOrCreateFolder(drive, city, yearFolderId);
      const userFolderId = await getOrCreateFolder(drive, userFolder, cityFolderId);
      const typeFolderId = await getOrCreateFolder(drive, instrumentType, userFolderId);
      const targetFolderId = await getOrCreateFolder(drive, recordId, typeFolderId);

      const drivePath = `DCA/${year}/${city}/${userFolder}/${instrumentType}/${recordId}`;

      // 2. Prepare file contents
      const jsonContent = JSON.stringify(recordData, null, 2);
      const csvContent = generateSummaryCsv(recordData.answers || recordData);
      const checksum = crypto.createHash('sha256').update(jsonContent).digest('hex');

      const metadataContent = JSON.stringify(
        {
          recordId,
          uploadedAt: new Date().toISOString(),
          uploadedBy: recordData.surveyorEmail || userId,
          targetDriveOwner: TARGET_DRIVE_OWNER,
          geo: recordData.geo || {
            lat: recordData.geoLat || 0,
            lng: recordData.geoLng || 0,
            accuracy: recordData.geoAccuracy || 0,
            timestamp: recordData.geoTimestamp || Date.now(),
          },
          consentGiven: recordData.consentGiven || false,
          status: recordData.status || 'completed',
          sha256Checksum: checksum,
          version: '2.2',
        },
        null,
        2
      );

      // 3. Upload JSON Payload
      const jsonFile = await withExponentialBackoff(() =>
        drive.files.create({
          requestBody: {
            name: 'payload.json',
            mimeType: 'application/json',
            parents: [targetFolderId],
          },
          media: {
            mimeType: 'application/json',
            body: jsonContent,
          },
          fields: 'id',
        })
      );

      // 4. Upload CSV Summary
      await withExponentialBackoff(() =>
        drive.files.create({
          requestBody: {
            name: 'summary.csv',
            mimeType: 'text/csv',
            parents: [targetFolderId],
          },
          media: {
            mimeType: 'text/csv',
            body: csvContent,
          },
          fields: 'id',
        })
      );

      // 5. Upload metadata.json
      await withExponentialBackoff(() =>
        drive.files.create({
          requestBody: {
            name: 'metadata.json',
            mimeType: 'application/json',
            parents: [targetFolderId],
          },
          media: {
            mimeType: 'application/json',
            body: metadataContent,
          },
          fields: 'id',
        })
      );

      const driveFileId = jsonFile.data.id!;

      // 6. Update Firestore document
      await snapshot.ref.update({
        driveFileId,
        drivePath,
        pushedToDriveAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // 7. Write audit log
      await db.collection('drivePushLogs').add({
        recordId,
        pushedBy: userId,
        pushedAt: admin.firestore.FieldValue.serverTimestamp(),
        status: 'success',
        driveFileId,
        drivePath,
        checksum,
      });

      console.log(`[RecordsSync] Successfully pushed record ${recordId} to Drive at ${drivePath}`);
      return { success: true, driveFileId, drivePath };
    } catch (error: any) {
      console.error(`[RecordsSync] Failed to push record ${recordId} to Drive:`, error);
      await db.collection('drivePushLogs').add({
        recordId,
        pushedBy: userId,
        pushedAt: admin.firestore.FieldValue.serverTimestamp(),
        status: 'failed',
        error: error.message || String(error),
        code: error.code || 'ERR_DRIVE_PUSH_FAILED',
      });
      throw error;
    }
  });

/**
 * Legacy Firestore Trigger for instruments/{instrumentId}
 */
export const onInstrumentApproved = functions.firestore
  .document('instruments/{instrumentId}')
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();
    const instrumentId = context.params.instrumentId;

    if (beforeData.status === 'approved' || afterData.status !== 'approved') {
      return null;
    }

    const drive = getDriveClient();
    const createdAt = afterData.createdAt ? new Date(afterData.createdAt) : new Date();
    const year = createdAt.getFullYear().toString();
    const city = afterData.city || 'ND';
    const userId = afterData.createdBy || 'anonymous_user';
    
    let displayName = 'Unknown_User';
    try {
      const userSnap = await db.collection('users').doc(userId).get();
      if (userSnap.exists && userSnap.data()?.displayName) {
        displayName = userSnap.data()?.displayName;
      }
    } catch (e) {
      console.warn(`[DriveSync] Could not fetch user display name for ${userId}:`, e);
    }

    const sanitizedUserFolder = `${userId}_${displayName.replace(/[^a-zA-Z0-9_-]/g, '_')}`;
    const instrumentType = afterData.instrumentType || 'General_Survey';

    try {
      const rootFolderId = ROOT_FOLDER_ID || (await getOrCreateFolder(drive, 'DCA', 'root'));
      const yearFolderId = await getOrCreateFolder(drive, year, rootFolderId);
      const cityFolderId = await getOrCreateFolder(drive, city, yearFolderId);
      const userFolderId = await getOrCreateFolder(drive, sanitizedUserFolder, cityFolderId);
      const typeFolderId = await getOrCreateFolder(drive, instrumentType, userFolderId);
      const targetFolderId = await getOrCreateFolder(drive, instrumentId, typeFolderId);

      const drivePath = `DCA/${year}/${city}/${sanitizedUserFolder}/${instrumentType}/${instrumentId}`;
      const rawPayload = afterData.answers || afterData;
      const jsonContent = JSON.stringify(afterData, null, 2);
      const csvContent = generateSummaryCsv(rawPayload);
      const checksum = crypto.createHash('sha256').update(jsonContent).digest('hex');

      const metadataContent = JSON.stringify(
        {
          instrumentId,
          uploadedAt: new Date().toISOString(),
          uploadedBy: afterData.approvedBy || 'System_Master',
          targetDriveOwner: TARGET_DRIVE_OWNER,
          geo: afterData.geo || null,
          geoTier: afterData.geoTier || 'strict',
          sha256Checksum: checksum,
          qaNotes: afterData.qaNotes || '',
          version: '2.2',
        },
        null,
        2
      );

      const jsonFile = await withExponentialBackoff(() =>
        drive.files.create({
          requestBody: {
            name: 'payload.json',
            mimeType: 'application/json',
            parents: [targetFolderId],
          },
          media: {
            mimeType: 'application/json',
            body: jsonContent,
          },
          fields: 'id',
        })
      );

      await withExponentialBackoff(() =>
        drive.files.create({
          requestBody: {
            name: 'summary.csv',
            mimeType: 'text/csv',
            parents: [targetFolderId],
          },
          media: {
            mimeType: 'text/csv',
            body: csvContent,
          },
          fields: 'id',
        })
      );

      await withExponentialBackoff(() =>
        drive.files.create({
          requestBody: {
            name: 'metadata.json',
            mimeType: 'application/json',
            parents: [targetFolderId],
          },
          media: {
            mimeType: 'application/json',
            body: metadataContent,
          },
          fields: 'id',
        })
      );

      const driveFileId = jsonFile.data.id!;

      await change.after.ref.update({
        driveFileId,
        drivePath,
        pushedToDriveAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      await db.collection('drivePushLogs').add({
        instrumentId,
        pushedBy: afterData.approvedBy || userId,
        pushedAt: admin.firestore.FieldValue.serverTimestamp(),
        status: 'success',
        driveFileId,
        drivePath,
        checksum,
      });

      return { success: true, driveFileId, drivePath };
    } catch (error: any) {
      console.error(`[DriveSync] Failed to push instrument ${instrumentId} to Drive:`, error);
      await db.collection('drivePushLogs').add({
        instrumentId,
        pushedBy: afterData.approvedBy || userId,
        pushedAt: admin.firestore.FieldValue.serverTimestamp(),
        status: 'failed',
        error: error.message || String(error),
        code: error.code || 'ERR_DRIVE_PUSH_FAILED',
      });
      throw error;
    }
  });
