/**
 * ============================================================================
 * DCA Cloud Sync - Google Apps Script v4.0 (12 Surveyors + Master Doc)
 * Project: Smart Cities, Silent Citizens (ICSSR Major Research Project 2025-26)
 * Token: DCA_SECRET_TOKEN_2026
 * 
 * ROUTING SPECIFICATION:
 * - Master Approval (status: 'approved'):
 *     1. Full Instrument + All Responses / Transcripts written to Field Doc (SC0101 to SC0112).
 *     2. Official Approval Audit Log written to Master Central Doc (COMMON_DOC_ID).
 * - Surveyor Submission (status: 'submitted' / 'COMPLETED'):
 *     - Stored in Memory Buffer for Master Control app review.
 *     - Does NOT clutter Master Doc with unapproved drafts (prevents double copies).
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
    status: 'DCA v4.0 Active',
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
  // Robustly resolve surveyor ID
  var uid = (p.userId || p.surveyorId || p.createdBy || '').toUpperCase().trim();
  if (!USER_DOC_MAP[uid]) {
    var match = (p.recordId || p.surveyUid || '').match(/SC01[0-1][0-9]/i);
    if (match) uid = match[0].toUpperCase();
  }

  var isApproved = (p.status || '').toLowerCase() === 'approved';
  var fieldDocId = USER_DOC_MAP[uid] || null;
  var masterDocId = COMMON_DOC_ID;

  var fieldDocWritten = false;
  var masterDocWritten = false;
  var writeError = null;

  if (isApproved) {
    // 1. Write FULL survey response into surveyor's dedicated Field Document (SC0101 to SC0112)
    if (fieldDocId) {
      try {
        writeFullSurveyToDoc(fieldDocId, p, true);
        fieldDocWritten = true;
      } catch (ex) {
        writeError = 'Field Doc write error: ' + ex.toString();
      }
    }

    // 2. Write official Approval Audit into Master Central Document (COMMON_DOC_ID)
    try {
      writeApprovalAuditToMasterDoc(masterDocId, p, fieldDocId);
      masterDocWritten = true;
    } catch (ex2) {
      writeError = (writeError ? (writeError + ' | ') : '') + 'Master Doc write error: ' + ex2.toString();
    }

    // Fallback: If dedicated doc failed or is unmapped, ensure full survey is written to master doc
    if (!fieldDocWritten && !masterDocWritten) {
      try {
        writeFullSurveyToDoc(masterDocId, p, true);
        masterDocWritten = true;
      } catch (ex3) {
        writeError = (writeError ? (writeError + ' | ') : '') + 'Fallback write error: ' + ex3.toString();
      }
    }
  } else {
    // SUBMISSION (Pending Approval):
    // Saved to memory buffer for real-time Master Board inspection.
    // Does NOT write full draft copies to Google Docs to avoid duplicate copies.
  }

  saveMemoryRecord(p, isApproved, fieldDocId || masterDocId);

  return {
    success: isApproved ? (fieldDocWritten || masterDocWritten) : true,
    recordId: p.recordId || p.surveyUid || '',
    userId: uid,
    status: p.status || 'draft',
    approved: isApproved,
    fieldDocId: fieldDocId,
    masterDocId: masterDocId,
    fieldDocWritten: fieldDocWritten,
    masterDocWritten: masterDocWritten,
    error: writeError
  };
}

// ── Document Formatter: Field Document (Full Survey with Responses) ──────────

function writeFullSurveyToDoc(docId, p, isApproved) {
  var doc = DocumentApp.openById(docId);
  var body = doc.getBody();
  var ts = Utilities.formatDate(new Date(), 'Asia/Kolkata', "dd-MMM-yyyy HH:mm:ss 'IST'");

  var badge = isApproved ? ' [APPROVED BY MASTER]' : ' [SUBMITTED - PENDING APPROVAL]';
  var header = '[' + (p.instrumentType || 'Survey') + '] ' + (p.displayName || p.userId || 'Field Investigator') + ' | ' + (p.city || 'ND') + ' | ' + ts + badge;

  body.appendParagraph('================================================================')
      .setHeading(DocumentApp.ParagraphHeading.NORMAL);

  body.appendParagraph(header).setHeading(DocumentApp.ParagraphHeading.HEADING2);

  var meta = 
    'Record ID:    ' + (p.recordId || p.surveyUid || '-') +
    '\nSurveyor:     ' + (p.userId || '-') + ' (' + (p.displayName || 'Field Investigator') + ')' +
    '\nParticipant:  ' + (p.participantName || 'Anonymous') + (p.participantAge ? (' (Age: ' + p.participantAge + ')') : '') +
    '\nCity/Locale:  ' + (p.city || 'ND') + (p.manualLocality ? (' - ' + p.manualLocality) : '') +
    '\nGPS Lat/Lng:  ' + (p.geoLat || '0') + ', ' + (p.geoLng || '0') + ' (+/-' + Math.round(p.geoAccuracy || 0) + 'm)' +
    '\nConsent:      ' + (p.consentGiven ? 'YES - Consented & Signed' : 'NO') +
    '\nStatus:       ' + (p.status || 'approved') +
    (p.approvedBy ? ('\nApproved By:  ' + p.approvedBy) : '') +
    (p.qaNotes ? ('\nQA Notes:     ' + p.qaNotes) : '');

  body.appendParagraph(meta).setHeading(DocumentApp.ParagraphHeading.NORMAL);

  // 1. Output Structured Answers / Themes
  var ans = p.answers;
  var hasAnswers = false;

  if (ans && typeof ans === 'object' && !Array.isArray(ans)) {
    var keys = Object.keys(ans);
    // Filter out internal system keys like __intake_...
    var visibleKeys = keys.filter(function(k) { return !k.startsWith('__intake_'); });
    if (visibleKeys.length > 0) {
      hasAnswers = true;
      body.appendParagraph('-- Responses & Observations --').setHeading(DocumentApp.ParagraphHeading.HEADING3);
      for (var i = 0; i < visibleKeys.length; i++) {
        var k = visibleKeys[i];
        var v = ans[k];
        if (v !== null && v !== undefined && String(v).trim() !== '') {
          body.appendParagraph('Q: ' + k + '\nA: ' + String(v)).setHeading(DocumentApp.ParagraphHeading.NORMAL);
        }
      }
    }
  } else if (typeof ans === 'string' && ans.trim().length > 0) {
    hasAnswers = true;
    body.appendParagraph('-- Responses --').setHeading(DocumentApp.ParagraphHeading.HEADING3);
    body.appendParagraph(ans).setHeading(DocumentApp.ParagraphHeading.NORMAL);
  }

  // 2. Output Interview Transcript if available
  if (p.transcriptText && String(p.transcriptText).trim().length > 0) {
    var transcriptAlreadyPrinted = ans && ans['Interview Notes & Transcript'] && 
        String(ans['Interview Notes & Transcript']).trim() === String(p.transcriptText).trim();
    if (!transcriptAlreadyPrinted) {
      body.appendParagraph('-- Interview Transcript & Field Notes --').setHeading(DocumentApp.ParagraphHeading.HEADING3);
      body.appendParagraph(String(p.transcriptText).trim()).setHeading(DocumentApp.ParagraphHeading.NORMAL);
    }
  }

  // 3. Output Field Summary if available
  if (p.manualSummary && String(p.manualSummary).trim().length > 0) {
    body.appendParagraph('-- Field Summary & Observations --').setHeading(DocumentApp.ParagraphHeading.HEADING3);
    body.appendParagraph(String(p.manualSummary).trim()).setHeading(DocumentApp.ParagraphHeading.NORMAL);
  }

  body.appendParagraph(' ');
}

// ── Document Formatter: Master Document (Approval Audit Entry) ───────────────

function writeApprovalAuditToMasterDoc(docId, p, fieldDocId) {
  var doc = DocumentApp.openById(docId);
  var body = doc.getBody();
  var ts = Utilities.formatDate(new Date(), 'Asia/Kolkata', "dd-MMM-yyyy HH:mm:ss 'IST'");

  var header = '[APPROVAL AUDIT] [' + (p.instrumentType || 'Survey') + '] Surveyor: ' + (p.userId || '-') + ' (' + (p.displayName || 'Field Investigator') + ') | ' + (p.city || 'ND') + ' | ' + ts + ' [APPROVED BY MASTER]';

  body.appendParagraph('================================================================')
      .setHeading(DocumentApp.ParagraphHeading.NORMAL);

  body.appendParagraph(header).setHeading(DocumentApp.ParagraphHeading.HEADING2);

  var fieldDocUrl = fieldDocId ? ('https://docs.google.com/document/d/' + fieldDocId + '/edit') : 'Common Document';

  var audit = 
    'Record ID:       ' + (p.recordId || p.surveyUid || '-') +
    '\nSurveyor ID:     ' + (p.userId || '-') +
    '\nSurveyor Name:   ' + (p.displayName || 'Field Investigator') +
    '\nInstrument:      ' + (p.instrumentType || 'Survey') +
    '\nCity:            ' + (p.city || 'ND') + (p.manualLocality ? (' - ' + p.manualLocality) : '') +
    '\nParticipant:     ' + (p.participantName || 'Anonymous') + (p.participantAge ? (' (Age: ' + p.participantAge + ')') : '') +
    '\nGPS Coordinates: ' + (p.geoLat || '0') + ', ' + (p.geoLng || '0') + ' (+/-' + Math.round(p.geoAccuracy || 0) + 'm)' +
    '\nConsent Given:   ' + (p.consentGiven ? 'YES - Consented & Signed' : 'NO') +
    '\nStatus:          APPROVED' +
    '\nApproved By:     ' + (p.approvedBy || 'Master Administrator') +
    '\nApproved At:     ' + ts +
    (p.qaNotes ? ('\nQA Review Notes: ' + p.qaNotes) : '') +
    '\nField Doc Link:  ' + fieldDocUrl;

  body.appendParagraph(audit).setHeading(DocumentApp.ParagraphHeading.NORMAL);
  body.appendParagraph(' ');
}

// ── In-Memory Ring Buffer ───────────────────────────────────────────────────

function saveMemoryRecord(p, isApproved, docId) {
  try {
    var props = PropertiesService.getScriptProperties();
    var list = JSON.parse(props.getProperty('DCA_RECORDS_V40') || '[]');
    list.unshift({
      recordId: p.recordId || p.surveyUid || ('REC-' + Date.now()),
      surveyUid: p.recordId || p.surveyUid || '',
      userId: p.userId || '',
      surveyorId: p.userId || '',
      displayName: p.displayName || '',
      surveyorName: p.displayName || '',
      city: p.city || '',
      instrumentType: p.instrumentType || '',
      status: p.status || 'submitted',
      approved: isApproved,
      approvedBy: p.approvedBy || '',
      qaNotes: p.qaNotes || '',
      docId: docId,
      participantName: p.participantName || '',
      answersJson: (typeof p.answers === 'object') ? JSON.stringify(p.answers) : (p.answers || '{}'),
      transcriptText: p.transcriptText || '',
      manualSummary: p.manualSummary || '',
      timestamp: new Date().toISOString()
    });
    if (list.length > 200) list = list.slice(0, 200);
    props.setProperty('DCA_RECORDS_V40', JSON.stringify(list));
  } catch (e) {}
}

function getMemoryRecords() {
  try {
    var props = PropertiesService.getScriptProperties();
    return JSON.parse(props.getProperty('DCA_RECORDS_V40') || '[]');
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
    recordId: 'TEST-V40-' + Date.now(),
    userId: 'SC0101',
    displayName: 'Test Investigator SC0101',
    city: 'Nagpur',
    instrumentType: 'SSI-NGO-v1.0',
    geoLat: '21.1458',
    geoLng: '79.0882',
    geoAccuracy: 10,
    consentGiven: true,
    status: 'approved',
    approvedBy: 'Master Administrator',
    qaNotes: 'Verified and approved',
    answers: { 
      '[Theme T1] NGO Advocacy, Case Support, and Field Experience': 'Organization operates 5 field teams providing digital case support for seniors 75+.' 
    },
    transcriptText: 'Full transcript of NGO interview conducted at Central Nagpur branch.',
    manualSummary: 'Critical digital exclusion identified in bus pass renewal system.'
  };
  var res = processRecord(p);
  Logger.log('Result: ' + JSON.stringify(res));
}
