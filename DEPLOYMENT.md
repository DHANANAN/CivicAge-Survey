# DCA (Smart Cities Responsible Citizens Initiative) Deployment & Operations Guide

## 1. Overview
The DCA Mobile Survey application features a streamlined **Records-only cloud sync flow**. When a survey record is completed, it is written to Firestore `records/{recordId}` and automatically synced to **Google Sheets (`DCA_Records`)** and **Google Drive** subfolders (`DCA/{YYYY}/{city}/{userId}_{displayName}/{instrumentType}/{recordId}/`) owned by `scscpdata@gmail.com`.

---

## 2. Google Sheets & Apps Script Setup (Chrome Walkthrough)

### 2.1 Google Sheets Creation
1. Open Google Chrome while logged into `scscpdata@gmail.com`.
2. Navigate to [Google Sheets](https://sheets.google.com).
3. Create a **New Blank Spreadsheet**.
4. Rename the spreadsheet to `DCA_Records`.
5. Add the following header row in Row 1 (Columns A to K):
   - `recordId`
   - `userId`
   - `displayName`
   - `geoLat`
   - `geoLng`
   - `geoAccuracy`
   - `geoTimestamp`
   - `consentGiven`
   - `status`
   - `driveFileId`
   - `drivePath`

### 2.2 Apps Script Web App Deployment
1. In the `DCA_Records` Google Sheet, click **Extensions** -> **Apps Script**.
2. Replace all existing code with the contents of `scripts/apps-script-records.gs`.
3. Click **Save** (disk icon).
4. Click **Deploy** -> **New Deployment**.
5. Select type: **Web app**.
   - **Description**: `DCA Records Cloud Sync API`
   - **Execute as**: `Me (scscpdata@gmail.com)`
   - **Who has access**: `Anyone`
6. Click **Deploy** and authorize permissions when prompted.
7. Copy the resulting **Web App URL** (e.g. `https://script.google.com/macros/s/.../exec`).

---

## 3. Firebase & GCP Infrastructure Setup

### 3.1 Cloud Functions Deployment
1. Navigate to the `functions/` directory:
   ```bash
   cd functions
   npm install
   npm run build
   ```
2. Deploy the `onRecordCreated` trigger:
   ```bash
   firebase deploy --only functions
   ```

### 3.2 Google Cloud Service Account IAM & Drive Delegation
1. Open **Google Cloud Console** -> **IAM & Admin** -> **Service Accounts**.
2. Select the service account for project `scscdata-51bb8`.
3. Ensure **Google Drive API** is enabled.
4. Grant the Service Account the **Editor** role.
5. In Google Drive under `scscpdata@gmail.com`, create or select the root folder `DCA` and share it with the Service Account email address as **Editor**.

### 3.3 Target Drive Folder Hierarchy
The Cloud Function automatically creates and maintains the following directory structure:
```
Google Drive/
└── DCA/
    └── {YYYY}/
        └── {city}/
            └── {userId}_{displayName}/
                └── {instrumentType}/
                    └── {recordId}/
                        ├── payload.json
                        ├── summary.csv
                        └── metadata.json
```

---

## 4. Firestore Security Rules Deployment
Deploy the rules enforcing strict geo validation (`accuracy <= 100m`) and consent:
```bash
firebase deploy --only firestore:rules
```

---

## 5. Operational Runbook: Drive Failure Recovery & Retries

### 5.1 Automatic Exponential Backoff
The Cloud Function executes exponential backoff retries for status codes 429 and 5xx:
- Initial delay: 1000ms
- Max attempts: 4
- Multiplier: $2^n$

### 5.2 Manual Backfill Trigger
If a sync fails permanently:
1. Check Firestore `drivePushLogs` collection for error details.
2. Touch the document in `records/{recordId}` to trigger re-sync:
   ```javascript
   db.collection("records").doc(recordId).update({ updatedAt: Date.now() });
   ```

---

## 6. Audit & Acceptance Checklist

- [x] Google Sheet `DCA_Records` created with specified 11 columns.
- [x] Apps Script `doPost` endpoint deployed under `scscpdata@gmail.com`.
- [x] Firebase Cloud Function `onRecordCreated` deployed on `records/{recordId}`.
- [x] Firestore security rules block submissions without geo accuracy <= 100m.
- [x] Master Control Panel protected under `Settings -> Advanced` with Biometric unlock.
- [x] Swipe navigation configured for app screen navigation.
