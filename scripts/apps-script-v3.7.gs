/**
 * DCA Cloud Sync - Google Doc Appender v3.7 (Safe Batch & Master-Controlled Routing)
 * Project: Smart Cities, Silent Citizens (ICSSR 2025-26)
 * Token: DCA_SECRET_TOKEN_2026
 * 
 * FEATURES:
 * 1. Safe Execution: Handles individual submissions AND batch syncs from Master Reviewer.
 * 2. Smart Routing: 
 *    - Unapproved submissions -> COMMON_DOC_ID (Master 'SCSC Data' Doc)
 *    - Master-Approved submissions -> Dedicated Surveyor Doc (USER_DOC_MAP[userId])
 * 3. Anti-Throttle Protection: Built-in 200ms delay between batch items to prevent Google Docs rate-limiting.
 */

// REPLACE WITH YOUR NEW MASTER GOOGLE DOC ID
const COMMON_DOC_ID = 'YOUR_NEW_MASTER_DOC_ID_HERE';
const SECRET_TOKEN  = 'DCA_SECRET_TOKEN_2026';

// Map of Surveyor IDs (SC0101 - SC0125) to dedicated Google Doc IDs
// Create these as needed in your new Google Drive and paste IDs here:
const USER_DOC_MAP = {
  'SC0101': '',
  'SC0102': '',
  'SC0103': '',
  'SC0104': '',
  'SC0105': '',
  'SC0106': '',
  'SC0107': '',
  'SC0108': '',
  'SC0109': '',
  'SC0110': '',
  'SC0111': '',
  'SC0112': '',
  'SC0113': '',
  'SC0114': '',
  'SC0115': '',
  'SC0116': '',
  'SC0117': '',
  'SC0118': '',
  'SC0119': '',
  'SC0120': '',
  'SC0121': '',
  'SC0122': '',
  'SC0123': '',
  'SC0124': '',
  'SC0125': ''
};

function doOptions(e) {
  return ContentService.createTextOutput('').setMimeType(ContentService.MimeType.TEXT);
}

function doGet(e) {
  if (e && e.parameter && e.parameter.action === 'getRecords') {
    var token = e.parameter.token;
    if (token !== SECRET_TOKEN) return json({ success: false, error: 'Unauthorized' });
    var records = getMemoryRecords();
    return json({ success: true, count: records.length, records: records });
  }
  return json({ status: 'DCA v3.7 OK', mode: 'Firestore-First Safe Sync' });
}

function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return json({ success: false, error: 'Empty body' });
    }
    
    var payload = JSON.parse(e.postData.contents);
    if (!payload.token || payload.token !== SECRET_TOKEN) {
      return json({ success: false, error: 'Unauthorized' });
    }

    // Support BATCH sync (array of records) or SINGLE record
    if (Array.isArray(payload.records)) {
      var results = [];
      for (var i = 0; i < payload.records.length; i++) {
        var rec = payload.records[i];
        rec.token = SECRET_TOKEN;
        var res = processSingleRecord(rec);
        results.push(res);
        Utilities.sleep(200); // 200ms pause to protect Google Docs rate limit
      }
      return json({ success: true, batchCount: results.length, results: results });
    } else {
      var res = processSingleRecord(payload);
      return json(res);
    }
  } catch (ex) {
    return json({ success: false, error: ex.toString() });
  }
}

function processSingleRecord(p) {
  var uid = (p.userId || '').toUpperCase().trim();
  var isApproved = (p.status || '').toLowerCase() === 'approved';
  
  // Routing:
  // Approved -> Dedicated surveyor doc if available, otherwise Master Doc.
  // Pending / Unapproved -> Master Doc (COMMON_DOC_ID).
  var targetDocId = isApproved ? (USER_DOC_MAP[uid] || COMMON_DOC_ID) : COMMON_DOC_ID;

  if (!targetDocId || targetDocId === 'YOUR_NEW_MASTER_DOC_ID_HERE') {
    return { success: false, recordId: p.recordId, error: 'Target Doc ID not configured' };
  }

  writeToDoc(targetDocId, p);
  saveMemoryRecord(p);
  return { success: true, docId: targetDocId, recordId: p.recordId || '', routed: uid, approved: isApproved };
}

function saveMemoryRecord(p) {
  try {
    var props = PropertiesService.getScriptProperties();
    var existingJson = props.getProperty('DCA_RECORDS') || '[]';
    var list = JSON.parse(existingJson);
    list.unshift({
      recordId: p.recordId || ('REC-' + Date.now()),
      userId: p.userId || '',
      displayName: p.displayName || '',
      city: p.city || '',
      instrumentType: p.instrumentType || '',
      status: p.status || 'draft',
      approved: (p.status || '').toLowerCase() === 'approved',
      timestamp: new Date().toISOString()
    });
    if (list.length > 200) list = list.slice(0, 200);
    props.setProperty('DCA_RECORDS', JSON.stringify(list));
  } catch (e) {
    Logger.log('saveMemoryRecord failed: ' + e);
  }
}

function getMemoryRecords() {
  try {
    var props = PropertiesService.getScriptProperties();
    var existingJson = props.getProperty('DCA_RECORDS') || '[]';
    return JSON.parse(existingJson);
  } catch (e) {
    return [];
  }
}

function writeToDoc(docId, p) {
  var doc = DocumentApp.openById(docId);
  var body = doc.getBody();
  var ts = Utilities.formatDate(new Date(), 'Asia/Kolkata', "dd-MMM-yyyy HH:mm:ss 'IST'");
  
  body.appendParagraph('================================================').setHeading(DocumentApp.ParagraphHeading.NORMAL);
  body.appendParagraph('[' + (p.instrumentType || 'Survey') + ']  ' + (p.displayName || p.userId || 'Unknown') + '  |  ' + (p.city || 'ND') + '  |  ' + ts)
      .setHeading(DocumentApp.ParagraphHeading.HEADING2);
  body.appendParagraph(
    'Record ID: ' + (p.recordId || '-') +
    '\nUser ID:   ' + (p.userId || '-') +
    '\nGPS:       ' + (p.geoLat || '0') + ', ' + (p.geoLng || '0') + ' (+/-' + Math.round(p.geoAccuracy || 0) + 'm)' +
    '\nConsent:   ' + (p.consentGiven ? 'YES' : 'NO') +
    '\nStatus:    ' + (p.status || 'draft')
  ).setHeading(DocumentApp.ParagraphHeading.NORMAL);

  var ans = p.answers;
  if (ans) {
    body.appendParagraph('-- Responses --').setHeading(DocumentApp.ParagraphHeading.HEADING3);
    if (typeof ans === 'object' && !Array.isArray(ans)) {
      var keys = Object.keys(ans);
      for (var i = 0; i < keys.length; i++) {
        var v = ans[keys[i]];
        if (v !== null && v !== undefined && String(v).trim() !== '') {
          body.appendParagraph('Q: ' + keys[i] + '\nA: ' + String(v)).setHeading(DocumentApp.ParagraphHeading.NORMAL);
        }
      }
    } else if (typeof ans === 'string' && ans.trim().length > 0) {
      body.appendParagraph(ans);
    }
  }
  body.appendParagraph(' ');
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
