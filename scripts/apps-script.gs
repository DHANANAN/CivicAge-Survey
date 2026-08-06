/**
 * DCA Google Apps Script Fallback Web App
 * Target Owner: scscpdata@gmail.com
 *
 * Deploy as Web App with:
 * - Execute as: Me (scscpdata@gmail.com)
 * - Who has access: Anyone with link (Requests must contain secret token header/field)
 */

const SHARED_SECRET = "DCA_SCSC_2026_SECRET_KEY";

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
