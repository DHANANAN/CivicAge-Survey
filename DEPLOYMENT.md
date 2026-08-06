# DCA (Smart Cities Responsible Citizens Initiative) Deployment & Operations Guide

## 1. Overview
The DCA Mobile Survey application provides multi-tier role-based access (`master`, `qa`, `normal`) for smart city field data collection. When a survey instrument is approved by a QA user or Master Administrator, a Firebase Cloud Function automatically uploads formatted datasets (`payload.json`, `summary.csv`, and `metadata.json`) to Google Drive owned by `scscpdata@gmail.com`.

---

## 2. GCP & Firebase Infrastructure Setup

### 2.1 Firebase Cloud Functions Deployment
1. Navigate to the `functions/` directory:
   ```bash
   cd functions
   npm install
   npm run build
   ```
2. Deploy Cloud Functions:
   ```bash
   firebase deploy --only functions
   ```

### 2.2 Google Cloud Service Account IAM & Drive Delegation
1. Go to the **Google Cloud Console** -> **IAM & Admin** -> **Service Accounts**.
2. Create or select the service account associated with project `scscdata-51bb8`.
3. Enable the **Google Drive API** in GCP Console.
4. Grant the Service Account the following IAM roles:
   - `Roles/Editor` or `Roles/Firebase Admin`
   - `Roles/Storage Object Admin`
5. Share the root Google Drive folder (`DCA/`) owned by `scscpdata@gmail.com` with the Service Account email address, granting **Editor** access.

### 2.3 Drive Target Folder Structure
The Cloud Function automatically creates and maintains the following idempotent directory structure:
```
Google Drive/
└── DCA/
    └── {YYYY}/
        └── {city}/
            └── {userId}_{displayName}/
                └── {instrumentType}/
                    └── {instrumentId}/
                        ├── payload.json
                        ├── summary.csv
                        └── metadata.json
```

---

## 3. Firestore Security Rules Deployment
Deploy the updated security rules to enforce role-based access and server-side geo-tag validation:
```bash
firebase deploy --only firestore:rules
```

---

## 4. Google Apps Script Alternative Endpoint (Fallback)
If Cloud Functions are temporarily unavailable or quota-limited, deploy the standalone Google Apps Script snippet:
1. Open Google Apps Script while logged into `scscpdata@gmail.com`.
2. Paste the contents of `scripts/apps-script.gs`.
3. Deploy as a **Web App**:
   - **Execute as**: *Me (scscpdata@gmail.com)*
   - **Who has access**: *Anyone*
4. Set the secret header variable `X-DCA-Auth-Token` to match the token configured in client settings.

---

## 5. Operational Runbook: Drive Failure Recovery & Retries

### 5.1 Quota & Rate Limit Errors (HTTP 429 / 503)
The Cloud Function automatically executes exponential backoff retries:
- **Base delay**: 1 second
- **Max attempts**: 5 attempts
- **Multiplier**: $2^n$

### 5.2 Manual Backfill / Retry Trigger
If a push fails permanently:
1. Open Firebase Console -> Firestore -> `drivePushLogs`.
2. Locate the failed entry by `instrumentId`.
3. In Firestore -> `instruments/{instrumentId}`, reset `status` to `"pending_qa"` and re-approve from the QA Dashboard or run:
   ```javascript
   db.collection("instruments").doc(instrumentId).update({ status: "approved" });
   ```

---

## 6. Audit & Acceptance Checklist

- [x] Cloud Function `onInstrumentApproved` registered and deployed.
- [x] Firestore security rules block client direct approval and enforce geo accuracy `<= 100m`.
- [x] Dual-tier location enforcement (Strict vs Soft Warning) implemented in Android client.
- [x] Master Control Panel moved to `Settings -> Advanced` with Biometric & Password unlock.
- [x] Swipe left/right gestures with first-time hint overlay and Undo snackbar working.
- [x] `payload.json`, `summary.csv`, and `metadata.json` generated for every approved instrument.
