/**
 * ============================================================================
 * DCA Cloud Sync - Google Apps Script v3.8 (12 Surveyors + Master Doc)
 * Project: Smart Cities, Silent Citizens (ICSSR Major Research Project 2025-26)
 * Token: DCA_SECRET_TOKEN_2026
 * ============================================================================
 */

// ── Master Central Document (SCSC Data) ──────────────────────────────────────
const COMMON_DOC_ID = '1pqmSuvsDRqnEQ-Yxw4Mh27-gYAEguwyLdrR6WvCCRU8'; 

const SECRET_TOKEN  = 'DCA_SECRET_TOKEN_2026';

// ── 12 Dedicated Field Investigator Documents (SC0101 to SC0112) ─────────────
const USER_DOC_MAP = {
  'SC0101': '1f6YxXWVt35bMnJsNzuHwzObN3N87WKsC38DAvIw4eCk',
  'SC0102': '1BMMXDiXvr95y17S_zvIsjI98Bg5FnY7Ks3OCj6mLlPo',
  'SC0103': '15wneFqNQz3OPL_tgMfnUY1JB_cFna6aNwzqxndRGaNs',
  'SC0104': '1YQccrd6KBvwUpa4fXRY9UpY_-XkKiNHkktGT-9FPHdU',
  'SC0105': '1hdtjT4itfIoHE8IUkYG7t4P6sZqt9g96Uinn_jcRNBk',
  'SC0106': '1VpSDVbP1Gw7KnWbb6FI4DGLycOprmv1xTz2OW9nS8lc',
  'SC0107': '1pJa9BoLLdc6Px0cmg02Zscm0k-ANaOjp0buY0LwopuM',
  'SC0108': '1ANCJrhiSAouc98XXhQR2waDEAV7rgHHxI1TX48Ddx1A',
  'SC0109': '1mL4maU2XkhKfTBeEXW2pElS_8ZTTzLFGsIHpHMfQ6LU',
  'SC0110': '1U_wBWwYESM4MZsi0c-Ifbbl66dyZ5sTCF34_ZteG6Hc',
  'SC0111': '1B6d4Chay-Q0Dxu6y9CD0gQsqo2LfcQhAR-Em0HBfQ4Q',
  'SC0112': '1qUnuEa_EjtEqXMQ88dlCOSSPgt9RfdF_vZPU50taV3g'
};

// ── Web App Entrypoints ─────────────────────────────────────────────────────

function doOptions(e) {
  return ContentService.createTextOutput('').setMimeType(ContentService.MimeType.TEXT);
}

function doGet(e) {
  var action = (e && e.parameter && e.parameter.action) || 'status';
  var token  = (e && e.parameter && e.parameter.token) || '';

  if (action === 'getRecords') {
    if (token !== SECRET_TOKEN) return json({ success: false, error: 'Unauthorized' });
    var records = getMemoryRecords();
    return json({ success: true, count: records.length, records: records });
  }

  return json({
    status: 'DCA v3.8 Active',
    masterDocConfigured: true,
    userDocsCount: Object.keys(USER_DOC_MAP).length,
    timestamp: new Date().toISOString()
  });
}

function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return json({ success: false, error: 'Empty body' });
    }

    var payload = JSON.parse(e.postData.contents);
    if (!payload.token || payload.token !== SECRET_TOKEN) {
      return json({ success: false, error: 'Unauthorized: Invalid token' });
    }

    // Support single record or batch of records
    if (Array.isArray(payload.records)) {
      var results = [];
      for (var i = 0; i < payload.records.length; i++) {
        var rec = payload.records[i];
        rec.token = SECRET_TOKEN;
        results.push(processRecord(rec));
        Utilities.sleep(200); // 200ms anti-throttle pause
      }
      return json({ success: true, batchCount: results.length, results: results });
    } else {
      return json(processRecord(payload));
    }

  } catch (err) {
    return json({ success: false, error: err.toString() });
  }
}

// ── Routing Logic ───────────────────────────────────────────────────────────

function processRecord(p) {
  var uid = (p.userId || '').toUpperCase().trim();
  var isApproved = (p.status || '').toLowerCase() === 'approved';

  // ROUTING:
  // - Approved by Master: Routes to surveyor's dedicated Doc (SC0101 to SC0112).
  // - Submit / Pending: Routes to the Master Document.
  var targetDocId = COMMON_DOC_ID;
  var routeType = 'master';

  if (isApproved && USER_DOC_MAP[uid]) {
    targetDocId = USER_DOC_MAP[uid];
    routeType = 'dedicated_' + uid;
  }

  var writeSuccess = false;
  var writeError = null;

  try {
    writeToDoc(targetDocId, p, isApproved);
    writeSuccess = true;
  } catch (ex) {
    writeError = ex.toString();
    // Fallback: If dedicated doc failed, try writing to master doc
    if (targetDocId !== COMMON_DOC_ID) {
      try {
        writeToDoc(COMMON_DOC_ID, p, isApproved);
        targetDocId = COMMON_DOC_ID;
        writeSuccess = true;
      } catch (ex2) {
        writeError = ex2.toString();
      }
    }
  }

  saveMemoryRecord(p, isApproved, targetDocId);

  return {
    success: writeSuccess,
    recordId: p.recordId || '',
    userId: uid,
    status: p.status || 'draft',
    approved: isApproved,
    docId: targetDocId,
    route: routeType,
    error: writeError
  };
}

// ── Document Formatter ──────────────────────────────────────────────────────

function writeToDoc(docId, p, isApproved) {
  var doc = DocumentApp.openById(docId);
  var body = doc.getBody();
  var ts = Utilities.formatDate(new Date(), 'Asia/Kolkata', "dd-MMM-yyyy HH:mm:ss 'IST'");

  var badge = isApproved ? ' [APPROVED BY MASTER]' : ' [SUBMITTED - PENDING APPROVAL]';
  var header = '[' + (p.instrumentType || 'Survey') + '] ' + (p.displayName || p.userId || 'Unknown') + ' | ' + (p.city || 'ND') + ' | ' + ts + badge;

  body.appendParagraph('================================================================')
      .setHeading(DocumentApp.ParagraphHeading.NORMAL);

  body.appendParagraph(header).setHeading(DocumentApp.ParagraphHeading.HEADING2);

  var meta = 
    'Record ID:   ' + (p.recordId || '-') +
    '\nSurveyor:    ' + (p.userId || '-') + ' (' + (p.displayName || 'Field Investigator') + ')' +
    '\nCity:        ' + (p.city || 'ND') +
    '\nGPS Lat/Lng: ' + (p.geoLat || '0') + ', ' + (p.geoLng || '0') + ' (+/-' + Math.round(p.geoAccuracy || 0) + 'm)' +
    '\nConsent:     ' + (p.consentGiven ? 'YES - Consented & Signed' : 'NO') +
    '\nStatus:      ' + (p.status || 'submitted') +
    (p.approvedBy ? ('\nApproved By: ' + p.approvedBy) : '');

  body.appendParagraph(meta).setHeading(DocumentApp.ParagraphHeading.NORMAL);

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

// ── In-Memory Ring Buffer ───────────────────────────────────────────────────

function saveMemoryRecord(p, isApproved, docId) {
  try {
    var props = PropertiesService.getScriptProperties();
    var list = JSON.parse(props.getProperty('DCA_RECORDS_V38') || '[]');
    list.unshift({
      recordId: p.recordId || ('REC-' + Date.now()),
      userId: p.userId || '',
      displayName: p.displayName || '',
      city: p.city || '',
      instrumentType: p.instrumentType || '',
      status: p.status || 'submitted',
      approved: isApproved,
      docId: docId,
      timestamp: new Date().toISOString()
    });
    if (list.length > 200) list = list.slice(0, 200);
    props.setProperty('DCA_RECORDS_V38', JSON.stringify(list));
  } catch (e) {}
}

function getMemoryRecords() {
  try {
    var props = PropertiesService.getScriptProperties();
    return JSON.parse(props.getProperty('DCA_RECORDS_V38') || '[]');
  } catch (e) {
    return [];
  }
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

// ── Test Function ───────────────────────────────────────────────────────────
function testWrite() {
  var p = {
    token: SECRET_TOKEN,
    recordId: 'TEST-V38-' + Date.now(),
    userId: 'SC0101',
    displayName: 'Test Investigator',
    city: 'Nagpur',
    instrumentType: 'Test Instrument',
    geoLat: '21.1458',
    geoLng: '79.0882',
    geoAccuracy: 10,
    consentGiven: true,
    status: 'submitted',
    answers: { 'Question 1': 'Response A', 'Question 2': 'Response B' }
  };
  var res = processRecord(p);
  Logger.log('Result: ' + JSON.stringify(res));
}
