/**
 * DCA Apps Script Endpoint for Google Sheets (DCA_Records) + Google Drive Integration
 * Target Account Owner: scscpdata@gmail.com / scscdatap2026@gmail.com
 *
 * HOW TO USE THIS SCRIPT:
 * 1. Open your Google Sheet ("DCA_Records" or any sheet created under scscpdata@gmail.com).
 * 2. Click Extensions -> Apps Script.
 * 3. Delete any code in Code.gs, paste this complete script below, and click Save (disk icon).
 * 4. Click Deploy -> New deployment -> Select type: Web app.
 *    - Description: DCA Records Cloud Sync API
 *    - Execute as: Me (your Google account)
 *    - Who has access: Anyone
 * 5. Click Deploy, Authorize access, and copy the Web App URL!
 */

const SPREADSHEET_NAME = "DCA_Records";
const ROOT_FOLDER_NAME = "DCA";
const SECRET_AUTH_TOKEN = "DCA_SECRET_TOKEN_2026"; // Optional auth token check

/**
 * Handle GET requests (ping / health check)
 */
function doGet(e) {
  return responseJson({
    status: "online",
    service: "DCA Records Cloud Sync API",
    timestamp: new Date().toISOString(),
    spreadsheet: SPREADSHEET_NAME
  });
}

/**
 * Handle POST requests (incoming survey records)
 */
function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return responseJson({ status: "error", message: "Empty post body" }, 400);
    }

    const rawContent = e.postData.contents;
    const payload = JSON.parse(rawContent);
    const record = payload.record || payload;

    // Optional token validation (allows requests with matching token or open token)
    if (payload.token && payload.token !== SECRET_AUTH_TOKEN) {
      return responseJson({ status: "error", message: "Unauthorized token" }, 401);
    }

    // 1. Process Google Drive subfolders & file uploads
    const driveResult = syncToGoogleDrive(record);

    // 2. Append Row to Google Sheet (DCA_Records)
    const sheetResult = appendToDcaSheet(record, driveResult);

    return responseJson({
      status: "success",
      recordId: record.recordId || record.surveyUid || "REC_" + Date.now(),
      driveFileId: driveResult.fileId,
      drivePath: driveResult.drivePath,
      sheetRowIndex: sheetResult.rowIndex
    });

  } catch (err) {
    Logger.log("Error in doPost: " + err.toString());
    return responseJson({
      status: "error",
      message: err.toString(),
      stack: err.stack || ""
    }, 500);
  }
}

/**
 * Sync record JSON, CSV, and metadata.json to Google Drive folder hierarchy:
 * DCA/{YYYY}/{city}/{userId}_{displayName}/{instrumentType}/{recordId}/
 */
function syncToGoogleDrive(record) {
  const year = record.year || new Date().getFullYear().toString();
  const city = (record.city || "ND").replace(/[^a-zA-Z0-9_-]/g, "_");
  const userId = (record.userId || record.createdBy || "UNKNOWN_USER").replace(/[^a-zA-Z0-9_-]/g, "_");
  const displayName = (record.displayName || record.surveyorName || "Surveyor").replace(/[^a-zA-Z0-9_-]/g, "_");
  const instrumentType = (record.instrumentType || "General_Survey").replace(/[^a-zA-Z0-9_-]/g, "_");
  const recordId = (record.recordId || record.surveyUid || "REC_" + Date.now()).replace(/[^a-zA-Z0-9_-]/g, "_");

  const userFolder = `${userId}_${displayName}`;

  // Get or Create Root Folder "DCA" in Drive
  const rootFolder = getOrCreateSubfolder(DriveApp.getRootFolder(), ROOT_FOLDER_NAME);
  const yearFolder = getOrCreateSubfolder(rootFolder, year);
  const cityFolder = getOrCreateSubfolder(yearFolder, city);
  const userSubFolder = getOrCreateSubfolder(cityFolder, userFolder);
  const instFolder = getOrCreateSubfolder(userSubFolder, instrumentType);
  const recordFolder = getOrCreateSubfolder(instFolder, recordId);

  const drivePath = `${ROOT_FOLDER_NAME}/${year}/${city}/${userFolder}/${instrumentType}/${recordId}`;

  // 1. Upload payload.json
  const payloadFile = createOrOverwriteTextFile(recordFolder, "payload.json", JSON.stringify(record, null, 2), "application/json");

  // 2. Upload summary.csv
  const csvContent = generateCsvSummary(record);
  createOrOverwriteTextFile(recordFolder, "summary.csv", csvContent, "text/csv");

  // 3. Upload metadata.json
  const metadata = {
    recordId: recordId,
    uploadedAt: new Date().toISOString(),
    uploadedBy: record.surveyorEmail || record.userId || userId,
    geo: {
      lat: record.geoLat || (record.geo ? record.geo.lat : 0),
      lng: record.geoLng || (record.geo ? record.geo.lng : 0),
      accuracy: record.geoAccuracy || (record.geo ? record.geo.accuracy : 0),
      timestamp: record.geoTimestamp || (record.geo ? record.geo.timestamp : Date.now())
    },
    consentGiven: record.consentGiven || false,
    status: record.status || "completed",
    sha256Checksum: computeChecksum(JSON.stringify(record)),
    version: "2.5"
  };
  createOrOverwriteTextFile(recordFolder, "metadata.json", JSON.stringify(metadata, null, 2), "application/json");

  return {
    fileId: payloadFile.getId(),
    drivePath: drivePath
  };
}

/**
 * Append row to Google Sheet (DCA_Records)
 */
function appendToDcaSheet(record, driveResult) {
  let ss;
  try {
    ss = SpreadsheetApp.getActiveSpreadsheet();
  } catch (e) {
    // If running un-bound, search by name or create
    const files = DriveApp.getFilesByName(SPREADSHEET_NAME);
    if (files.hasNext()) {
      ss = SpreadsheetApp.open(files.next());
    } else {
      ss = SpreadsheetApp.create(SPREADSHEET_NAME);
    }
  }

  if (!ss) {
    ss = SpreadsheetApp.create(SPREADSHEET_NAME);
  }

  let sheet = ss.getSheetByName(SPREADSHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(SPREADSHEET_NAME);
  }

  // Write header row if empty
  if (sheet.getLastRow() === 0) {
    const headers = [
      "recordId",
      "userId",
      "displayName",
      "geoLat",
      "geoLng",
      "geoAccuracy",
      "geoTimestamp",
      "consentGiven",
      "status",
      "driveFileId",
      "drivePath"
    ];
    sheet.appendRow(headers);
    sheet.getRange(1, 1, 1, headers.length).setFontWeight("bold").setBackground("#E8DEF8");
  }

  const geoLat = record.geoLat || (record.geo ? record.geo.lat : 0);
  const geoLng = record.geoLng || (record.geo ? record.geo.lng : 0);
  const geoAccuracy = record.geoAccuracy || (record.geo ? record.geo.accuracy : 0);
  const geoTimestamp = record.geoTimestamp || (record.geo ? record.geo.timestamp : Date.now());

  const rowData = [
    record.recordId || record.surveyUid || "",
    record.userId || record.createdBy || "",
    record.displayName || record.surveyorName || "",
    geoLat,
    geoLng,
    geoAccuracy,
    new Date(geoTimestamp).toISOString(),
    record.consentGiven || false,
    record.status || "completed",
    driveResult.fileId || "",
    driveResult.drivePath || ""
  ];

  sheet.appendRow(rowData);
  return { rowIndex: sheet.getLastRow() };
}

/**
 * Helper to get existing subfolder or create new one
 */
function getOrCreateSubfolder(parentFolder, folderName) {
  const sanitized = folderName.replace(/[/\\?%*:|"<>]/g, "_").trim();
  const folders = parentFolder.getFoldersByName(sanitized);
  if (folders.hasNext()) {
    return folders.next();
  } else {
    return parentFolder.createFolder(sanitized);
  }
}

/**
 * Overwrite or create file inside target folder
 */
function createOrOverwriteTextFile(folder, filename, content, mimeType) {
  const existingFiles = folder.getFilesByName(filename);
  while (existingFiles.hasNext()) {
    existingFiles.next().setTrashed(true);
  }
  return folder.createFile(filename, content, mimeType);
}

/**
 * Generate CSV text representation of survey answers
 */
function generateCsvSummary(record) {
  let lines = ["questionId,questionText,answerValue"];
  const answers = record.answers || record.answersJson || record;

  if (typeof answers === "string") {
    try {
      const parsed = JSON.parse(answers);
      for (const key in parsed) {
        lines.push(`"${key}","${key}","${String(parsed[key]).replace(/"/g, '""')}"`);
      }
    } catch (e) {
      lines.push('"raw_answers","raw","' + String(answers).replace(/"/g, '""') + '"');
    }
  } else if (typeof answers === "object") {
    for (const key in answers) {
      const val = typeof answers[key] === "object" ? JSON.stringify(answers[key]) : String(answers[key]);
      lines.push(`"${key}","${key}","${val.replace(/"/g, '""')}"`);
    }
  }

  return lines.join("\n");
}

/**
 * SHA-256 Checksum computation
 */
function computeChecksum(str) {
  const rawHash = Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, str);
  let txt = "";
  for (let i = 0; i < rawHash.length; i++) {
    let byteVal = rawHash[i];
    if (byteVal < 0) byteVal += 256;
    let byteStr = byteVal.toString(16);
    if (byteStr.length === 1) byteStr = "0" + byteStr;
    txt += byteStr;
  }
  return txt;
}

/**
 * Helper to build JSON responses
 */
function responseJson(obj, statusCode) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
