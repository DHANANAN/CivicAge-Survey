/**
 * DCA Apps Script Endpoint for Google Sheets (DCA_Records) + Google Drive Integration
 * Account Owner: scscpdata@gmail.com
 */

const SECRET_AUTH_TOKEN = "DCA_SECRET_TOKEN_2026";
const ROOT_FOLDER_NAME = "DCA";
const SPREADSHEET_NAME = "DCA_Records";

function doPost(e) {
  try {
    const authHeader = e.parameter.token || (e.postData ? JSON.parse(e.postData.contents).token : "");
    if (authHeader !== SECRET_AUTH_TOKEN) {
      return responseJson({ status: "error", message: "Unauthorized token" }, 401);
    }

    const data = JSON.parse(e.postData.contents);
    const record = data.record || data;

    // 1. Process Google Drive Folders and File Uploads
    const driveResult = syncToGoogleDrive(record);

    // 2. Append Row to Google Sheet (DCA_Records)
    const sheetResult = appendToDcaSheet(record, driveResult);

    return responseJson({
      status: "success",
      recordId: record.recordId || record.surveyUid,
      driveFileId: driveResult.fileId,
      drivePath: driveResult.drivePath,
      sheetRowIndex: sheetResult.rowIndex
    });

  } catch (err) {
    return responseJson({ status: "error", message: err.toString() }, 500);
  }
}

function syncToGoogleDrive(record) {
  const year = record.year || new Date().getFullYear().toString();
  const city = record.city || "ND";
  const userId = record.userId || record.createdBy || "UNKNOWN_USER";
  const displayName = (record.displayName || record.surveyorName || "Surveyor").replace(/[^a-zA-Z0-9_-]/g, "_");
  const instrumentType = record.instrumentType || "General_Survey";
  const recordId = record.recordId || record.surveyUid || "REC_" + Date.now();

  const userFolder = `${userId}_${displayName}`;

  // Get or Create Root Folder "DCA"
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
    uploadedBy: record.surveyorEmail || userId,
    geo: {
      lat: record.geoLat || record.latitude || 0,
      lng: record.geoLng || record.longitude || 0,
      accuracy: record.geoAccuracy || record.gpsAccuracy || 0,
      timestamp: record.geoTimestamp || record.consentTimestamp || Date.now()
    },
    consentGiven: record.consentGiven || false,
    status: record.status || "completed",
    sha256Checksum: computeChecksum(JSON.stringify(record)),
    version: "2.2"
  };
  createOrOverwriteTextFile(recordFolder, "metadata.json", JSON.stringify(metadata, null, 2), "application/json");

  return {
    fileId: payloadFile.getId(),
    drivePath: drivePath
  };
}

function appendToDcaSheet(record, driveResult) {
  let ss;
  const files = DriveApp.getFilesByName(SPREADSHEET_NAME);
  if (files.hasNext()) {
    ss = SpreadsheetApp.open(files.next());
  } else {
    ss = SpreadsheetApp.create(SPREADSHEET_NAME);
  }

  let sheet = ss.getActiveSheet();
  if (sheet.getLastRow() === 0) {
    sheet.appendRow([
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
    ]);
    sheet.getRange(1, 1, 1, 11).setFontWeight("bold").setBackground("#E8DEF8");
  }

  const rowData = [
    record.recordId || record.surveyUid || "",
    record.userId || record.createdBy || "",
    record.displayName || record.surveyorName || "",
    record.geoLat || record.latitude || 0,
    record.geoLng || record.longitude || 0,
    record.geoAccuracy || record.gpsAccuracy || 0,
    new Date(record.geoTimestamp || Date.now()).toISOString(),
    record.consentGiven || false,
    record.status || "completed",
    driveResult.fileId || "",
    driveResult.drivePath || ""
  ];

  sheet.appendRow(rowData);
  return { rowIndex: sheet.getLastRow() };
}

function getOrCreateSubfolder(parentFolder, folderName) {
  const folders = parentFolder.getFoldersByName(folderName);
  if (folders.hasNext()) {
    return folders.next();
  } else {
    return parentFolder.createFolder(folderName);
  }
}

function createOrOverwriteTextFile(folder, filename, content, mimeType) {
  const existingFiles = folder.getFilesByName(filename);
  while (existingFiles.hasNext()) {
    existingFiles.next().setTrashed(true);
  }
  return folder.createFile(filename, content, mimeType);
}

function generateCsvSummary(record) {
  let lines = ["questionId,questionText,answerValue"];
  if (record.answersJson) {
    try {
      const answers = JSON.parse(record.answersJson);
      for (const key in answers) {
        lines.push(`"${key}","${key}","${String(answers[key]).replace(/"/g, '""')}"`);
      }
    } catch (e) {
      lines.push('"raw_answers","raw","' + String(record.answersJson).replace(/"/g, '""') + '"');
    }
  }
  return lines.join("\n");
}

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

function responseJson(obj, statusCode) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
