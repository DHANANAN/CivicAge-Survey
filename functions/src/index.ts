import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { google, drive_v3 } from 'googleapis';
import * as crypto from 'crypto';
import axios from 'axios';

admin.initializeApp();
const db = admin.firestore();

// Environment configuration & deployed Apps Script Web App URL
const APPS_SCRIPT_WEB_APP_URL = 'https://script.google.com/macros/s/AKfycbxJl6uhLpqFa-QkHuc9GFCrwTKaN_ju9JHbA-sy7O22pRrO9VzOJtsNqI4ZMdsrlyhb/exec';
const DCA_SECRET_TOKEN = 'DCA_SECRET_TOKEN_2026';
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
 * Send POST Payload to Apps Script Web App Endpoint
 */
export async function postToAppsScript(payload: Record<string, any>): Promise<{ driveFileId?: string; drivePath?: string }> {
  try {
    console.log(`[AppsScriptSync] Posting record ${payload.recordId} to Apps Script Web App...`);
    const response = await axios.post(APPS_SCRIPT_WEB_APP_URL, payload, {
      headers: { 'Content-Type': 'application/json' },
      timeout: 30000,
    });

    if (response.data && response.data.status === 'success') {
      console.log(`[AppsScriptSync] Successfully posted to Apps Script:`, response.data);
      return {
        driveFileId: response.data.driveFileId,
        drivePath: response.data.drivePath,
      };
    } else {
      console.warn(`[AppsScriptSync] Non-success response from Apps Script:`, response.data);
      return {};
    }
  } catch (err: any) {
    console.error(`[AppsScriptSync] HTTP POST to Apps Script failed:`, err.message || err);
    return {};
  }
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
  folderName: string,
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

    console.log(`[RecordsSync] New record created: ${recordId}. Syncing via Apps Script & Google Drive...`);

    const recordPayload = {
      token: DCA_SECRET_TOKEN,
      recordId: recordId,
      userId: recordData.userId || recordData.createdBy || 'anonymous_user',
      displayName: recordData.displayName || recordData.surveyorName || 'Surveyor',
      city: recordData.city || 'ND',
      instrumentType: recordData.instrumentType || 'General_Survey',
      geoLat: recordData.geoLat || (recordData.geo ? recordData.geo.lat : 0),
      geoLng: recordData.geoLng || (recordData.geo ? recordData.geo.lng : 0),
      geoAccuracy: recordData.geoAccuracy || (recordData.geo ? recordData.geo.accuracy : 0),
      geoTimestamp: recordData.geoTimestamp || (recordData.geo ? recordData.geo.timestamp : Date.now()),
      consentGiven: recordData.consentGiven || false,
      status: recordData.status || 'completed',
      answers: recordData.answers || recordData,
    };

    // 1. Post to Apps Script Web App Endpoint
    const appsScriptResult = await postToAppsScript(recordPayload);

    let driveFileId = appsScriptResult.driveFileId || '';
    let drivePath = appsScriptResult.drivePath || '';

    // 2. Direct Google Drive API Fallback / Backup Sync
    if (!driveFileId || !drivePath) {
      try {
        const drive = getDriveClient();
        const createdAt = recordData.createdAt ? new Date(recordData.createdAt) : new Date();
        const year = createdAt.getFullYear().toString();
        const city = recordPayload.city;
        const userId = recordPayload.userId;
        const displayName = recordPayload.displayName.replace(/[^a-zA-Z0-9_-]/g, '_');
        const userFolder = `${userId}_${displayName}`;
        const instrumentType = recordPayload.instrumentType;

        const rootFolderId = ROOT_FOLDER_ID || (await getOrCreateFolder(drive, 'DCA', 'root'));
        const yearFolderId = await getOrCreateFolder(drive, year, rootFolderId);
        const cityFolderId = await getOrCreateFolder(drive, city, yearFolderId);
        const userFolderId = await getOrCreateFolder(drive, userFolder, cityFolderId);
        const typeFolderId = await getOrCreateFolder(drive, instrumentType, userFolderId);
        const targetFolderId = await getOrCreateFolder(drive, recordId, typeFolderId);

        drivePath = `DCA/${year}/${city}/${userFolder}/${instrumentType}/${recordId}`;

        const jsonContent = JSON.stringify(recordData, null, 2);
        const csvContent = generateSummaryCsv(recordData.answers || recordData);
        const checksum = crypto.createHash('sha256').update(jsonContent).digest('hex');

        const metadataContent = JSON.stringify(
          {
            recordId,
            uploadedAt: new Date().toISOString(),
            uploadedBy: recordData.surveyorEmail || userId,
            targetDriveOwner: TARGET_DRIVE_OWNER,
            geo: {
              lat: recordPayload.geoLat,
              lng: recordPayload.geoLng,
              accuracy: recordPayload.geoAccuracy,
              timestamp: recordPayload.geoTimestamp,
            },
            consentGiven: recordPayload.consentGiven,
            status: recordPayload.status,
            sha256Checksum: checksum,
            version: '2.6',
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

        driveFileId = jsonFile.data.id!;
      } catch (err: any) {
        console.error(`[RecordsSync] Drive API fallback failed for ${recordId}:`, err);
      }
    }

    // 3. Update Firestore Document with Drive references
    await snapshot.ref.update({
      driveFileId: driveFileId || 'SYNCED_APPS_SCRIPT',
      drivePath: drivePath || `DCA/2026/${recordPayload.city}/${recordPayload.userId}/${recordPayload.instrumentType}/${recordId}`,
      pushedToDriveAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // 4. Write audit log entry
    await db.collection('drivePushLogs').add({
      recordId,
      pushedBy: recordPayload.userId,
      pushedAt: admin.firestore.FieldValue.serverTimestamp(),
      status: 'success',
      driveFileId,
      drivePath,
      webAppUrl: APPS_SCRIPT_WEB_APP_URL,
    });

    return { success: true, driveFileId, drivePath };
  });
