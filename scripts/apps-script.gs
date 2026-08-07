/**
 * DCA Google Apps Script - Cloud Sync API v2.9
 * Sheet: https://docs.google.com/spreadsheets/d/1natwaPyfZlB9uTHO9D8xmhiky6o90Yk6bGSoct9aKlw
 * Deploy: Execute as Me | Access: Anyone
 *
 * PASTE THIS into Apps Script editor → Save (Ctrl+S) → Deploy → Manage deployments → New Version
 */

const SHEET_ID     = "1natwaPyfZlB9uTHO9D8xmhiky6o90Yk6bGSoct9aKlw";
const SECRET_TOKEN = "DCA_SECRET_TOKEN_2026";
const DRIVE_ROOT   = "DCA_Field_Records";

function doPost(e) {
  try {
    var payload = JSON.parse(e.postData.contents);

    if (payload.token !== SECRET_TOKEN) {
      return json({ error: "Unauthorized - invalid token" });
    }

    // 1. Write row to DCA_Records sheet
    var ss    = SpreadsheetApp.openById(SHEET_ID);
    var sheet = ss.getSheetByName("DCA_Records");
    if (!sheet) {
      sheet = ss.insertSheet("DCA_Records");
      sheet.appendRow([
        "recordId","userId","displayName","city","instrumentType",
        "geoLat","geoLng","geoAccuracy","geoTimestamp","consentGiven",
        "status","answers","createdAt","updatedAt"
      ]);
    }

    var answersStr = typeof payload.answers === "object"
      ? JSON.stringify(payload.answers)
      : (payload.answers || "");

    sheet.appendRow([
      payload.recordId       || "",
      payload.userId         || "",
      payload.displayName    || "",
      payload.city           || "ND",
      payload.instrumentType || "General_Survey",
      payload.geoLat         || 0,
      payload.geoLng         || 0,
      payload.geoAccuracy    || 0,
      payload.geoTimestamp   || new Date().getTime(),
      payload.consentGiven   ? "YES" : "NO",
      payload.status         || "submitted",
      answersStr,
      new Date().toISOString(),
      new Date().toISOString()
    ]);

    // 2. Create Drive folder + files
    var driveResult = createDriveEntry(payload);

    return json({
      success:       true,
      recordId:      payload.recordId,
      sheetRow:      sheet.getLastRow(),
      driveFolder:   driveResult.path,
      driveFolderId: driveResult.folderId
    });

  } catch (err) {
    return json({ error: err.toString() });
  }
}

function createDriveEntry(payload) {
  try {
    var rootIt = DriveApp.getFoldersByName(DRIVE_ROOT);
    var root   = rootIt.hasNext() ? rootIt.next() : DriveApp.createFolder(DRIVE_ROOT);

    var year      = new Date().getFullYear().toString();
    var city      = clean(payload.city           || "ND");
    var userDir   = clean((payload.userId || "user") + "_" + (payload.displayName || "unknown"));
    var instrType = clean(payload.instrumentType || "General_Survey");
    var recId     = clean(payload.recordId       || ("REC-" + Date.now()));

    var folder = getOrCreate(getOrCreate(getOrCreate(getOrCreate(getOrCreate(root, year), city), userDir), instrType), recId);

    folder.createFile(recId + "_payload.json", JSON.stringify(payload, null, 2), MimeType.PLAIN_TEXT);

    var answers  = (typeof payload.answers === "object" && payload.answers) ? payload.answers : {};
    var csvLines = ["Question ID,Response"];
    for (var k in answers) {
      if (answers.hasOwnProperty(k)) {
        csvLines.push('"' + k + '","' + String(answers[k]).replace(/"/g, '""') + '"');
      }
    }
    folder.createFile(recId + "_answers.csv", csvLines.join("\n"), MimeType.CSV);

    return { folderId: folder.getId(), path: DRIVE_ROOT + "/" + year + "/" + city + "/" + userDir + "/" + instrType + "/" + recId };
  } catch (e2) {
    return { folderId: "", path: "error: " + e2.toString() };
  }
}

function doGet(e) {
  return json({ status: "DCA Cloud Sync API v2.9", sheetId: SHEET_ID });
}

function getOrCreate(parent, name) {
  var it = parent.getFoldersByName(name);
  return it.hasNext() ? it.next() : parent.createFolder(name);
}

function clean(s) {
  return String(s).replace(/[^a-zA-Z0-9_\-]/g, "_").substring(0, 50);
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}


function doPost(e) {
  try {
    const contents = JSON.parse(e.postData.contents);

    // Verify secret signature
    if (contents.secretToken !== SHARED_SECRET) {
      return ContentService.createTextOutput(JSON.stringify({
        status: "error",
        code: "ERR_NOT_AUTHORIZED",
        message: "Invalid secret token signature"
      })).setMimeType(ContentService.MimeType.JSON);
    }

    const instrument = contents.instrument;
    const year = new Date(instrument.createdAt || Date.now()).getFullYear().toString();
    const city = instrument.city || "ND";
    const userId = instrument.createdBy || "anonymous";
    const displayName = (instrument.displayName || "Unknown_User").replace(/[^a-zA-Z0-9_-]/g, "_");
    const userFolder = `${userId}_${displayName}`;
    const instrumentType = instrument.instrumentType || "General_Survey";
    const instrumentId = instrument.surveyUid || ("INST_" + Date.now());

    // 1. Get or create root DCA folder
    const rootFolders = DriveApp.getFoldersByName("DCA");
    const rootFolder = rootFolders.hasNext() ? rootFolders.next() : DriveApp.createFolder("DCA");

    // 2. Build folder hierarchy
    const yearFolder = getOrCreateSubFolder(rootFolder, year);
    const cityFolder = getOrCreateSubFolder(yearFolder, city);
    const uFolder = getOrCreateSubFolder(cityFolder, userFolder);
    const typeFolder = getOrCreateSubFolder(uFolder, instrumentType);
    const targetFolder = getOrCreateSubFolder(typeFolder, instrumentId);

    // 3. Create payload.json
    const jsonContent = JSON.stringify(instrument, null, 2);
    targetFolder.createFile("payload.json", jsonContent, MimeType.PLAIN_TEXT);

    // 4. Create summary.csv
    const csvContent = generateCsv(instrument.answers || instrument);
    targetFolder.createFile("summary.csv", csvContent, MimeType.CSV);

    // 5. Create metadata.json
    const metadata = {
      instrumentId: instrumentId,
      uploadedAt: new Date().toISOString(),
      uploadedBy: instrument.approvedBy || "Master_AppsScript",
      geo: instrument.geo || null,
      version: "2.2-AppsScript"
    };
    const metaFile = targetFolder.createFile("metadata.json", JSON.stringify(metadata, null, 2), MimeType.PLAIN_TEXT);

    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      driveFileId: metaFile.getId(),
      drivePath: "DCA/" + year + "/" + city + "/" + userFolder + "/" + instrumentType + "/" + instrumentId
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      code: "ERR_APPS_SCRIPT_FAILED",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function getOrCreateSubFolder(parent, name) {
  const folders = parent.getFoldersByName(name);
  if (folders.hasNext()) {
    return folders.next();
  }
  return parent.createFolder(name);
}

function generateCsv(payload) {
  var lines = ["Question ID,Response Value"];
  for (var key in payload) {
    if (payload.hasOwnProperty(key)) {
      var val = payload[key];
      var strVal = typeof val === 'object' ? JSON.stringify(val) : String(val);
      lines.push('"' + key + '","' + strVal.replace(/"/g, '""') + '"');
    }
  }
  return lines.join("\n");
}
