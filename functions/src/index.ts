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
      // Retryable status codes (e.g. Rate Limit 429, Server Error 5xx)
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
 * Initialize Google Drive Client using Service Account ADC or keyfile credentials
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
 * Firestore Trigger: Fires when an instrument's status transitions to 'approved'.
 */
export const onInstrumentApproved = functions.firestore
  .document('instruments/{instrumentId}')
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();
    const instrumentId = context.params.instrumentId;

    // Trigger only when transitioning into 'approved' status
    if (beforeData.status === 'approved' || afterData.status !== 'approved') {
      return null;
    }

    console.log(`[DriveSync] Instrument ${instrumentId} approved. Initiating Google Drive pipeline...`);

    const drive = getDriveClient();
    const createdAt = afterData.createdAt ? new Date(afterData.createdAt) : new Date();
    const year = createdAt.getFullYear().toString();
    const city = afterData.city || 'ND';
    const userId = afterData.createdBy || 'anonymous_user';
    
    // Fetch user details for folder naming
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
      // 1. Build idempotent hierarchy: DCA/{YYYY}/{city}/{userId}_{displayName}/{instrumentType}/{instrumentId}/
      const rootFolderId = ROOT_FOLDER_ID || (await getOrCreateFolder(drive, 'DCA', 'root'));
      const yearFolderId = await getOrCreateFolder(drive, year, rootFolderId);
      const cityFolderId = await getOrCreateFolder(drive, city, yearFolderId);
      const userFolderId = await getOrCreateFolder(drive, sanitizedUserFolder, cityFolderId);
      const typeFolderId = await getOrCreateFolder(drive, instrumentType, userFolderId);
      const targetFolderId = await getOrCreateFolder(drive, instrumentId, typeFolderId);

      const drivePath = `DCA/${year}/${city}/${sanitizedUserFolder}/${instrumentType}/${instrumentId}`;

      // 2. Prepare file contents
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

      // 5. Upload metadata.json for audit clarity
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

      // 6. Update Firestore document with drive File ID and full path
      await change.after.ref.update({
        driveFileId,
        drivePath,
        pushedToDriveAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // 7. Write audit log to drivePushLogs
      await db.collection('drivePushLogs').add({
        instrumentId,
        pushedBy: afterData.approvedBy || userId,
        pushedAt: admin.firestore.FieldValue.serverTimestamp(),
        status: 'success',
        driveFileId,
        drivePath,
        checksum,
      });

      console.log(`[DriveSync] Successfully pushed instrument ${instrumentId} to Google Drive at ${drivePath} (File ID: ${driveFileId})`);
      return { success: true, driveFileId, drivePath };
    } catch (error: any) {
      console.error(`[DriveSync] Failed to push instrument ${instrumentId} to Drive:`, error);

      // Write error log to drivePushLogs
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
