/**
 * DCA Per-Doc Apps Script — v3.4 (Fixed)
 *
 * HOW TO DEPLOY (do this for each of the 12 Google Docs):
 * ─────────────────────────────────────────────────────────
 * 1. Open the Google Doc → Extensions → Apps Script
 * 2. Replace ALL existing code with this file's contents
 * 3. Click 💾 Save  (Ctrl+S)
 * 4. Click Deploy → New deployment
 *    - Type:            Web App
 *    - Execute as:      Me  ← IMPORTANT
 *    - Who has access:  Anyone  ← IMPORTANT (not "Anyone with Google Account")
 * 5. Click Deploy → Authorise access → Allow
 * 6. Copy the Web App URL — paste it into the relevant USER_DOC_URL_MAP
 *    entry in SurveyRepository.kt and rebuild the app.
 *
 * HOW TO TEST (before installing the APK):
 * ─────────────────────────────────────────
 * 1. In the Apps Script editor, select function: "testPost" from the toolbar
 * 2. Click ▶ Run  — a test record should appear inside this Google Doc
 *
 * Token (must match app): DCA_SECRET_TOKEN_2026
 */

var TOKEN = "DCA_SECRET_TOKEN_2026";

/* ────────────────────────────────────────────────────────── */
/* CORS pre-flight — some HTTP clients send OPTIONS first     */
/* ────────────────────────────────────────────────────────── */
function doOptions(e) {
  return ContentService
    .createTextOutput("")
    .setMimeType(ContentService.MimeType.TEXT);
}

/* ────────────────────────────────────────────────────────── */
/* Health check — GET request returns {"status":"ok"}         */
/* ────────────────────────────────────────────────────────── */
function doGet(e) {
  var doc = DocumentApp.getActiveDocument();
  return ok({ status: "ok", doc: doc.getName(), id: doc.getId() });
}

/* ────────────────────────────────────────────────────────── */
/* Main POST endpoint                                          */
/* ────────────────────────────────────────────────────────── */
function doPost(e) {
  try {
    /* Parse body */
    if (!e || !e.postData || !e.postData.contents) {
      return err("No POST body received");
    }
    var payload = JSON.parse(e.postData.contents);

    /* Auth */
    if (!payload.token || payload.token !== TOKEN) {
      return err("Unauthorized — wrong token");
    }

    /* Get bound document */
    var doc  = DocumentApp.getActiveDocument();
    var body = doc.getBody();

    /* Timestamp */
    var now     = new Date();
    var dateStr = Utilities.formatDate(now, "Asia/Kolkata", "dd MMM yyyy, hh:mm a 'IST'");

    /* Format answers */
    var answers     = payload.answers || payload.answersJson || {};
    var answerText  = formatAnswers(answers);

    /* ── Append to document ─────────────────────────────── */
    body.appendParagraph(
      "════════════════════════════════════════════════════════════"
    ).setHeading(DocumentApp.ParagraphHeading.NORMAL);

    body.appendParagraph(
      "📋 " + (payload.instrumentType || "Survey") +
      "  |  " + (payload.displayName || payload.surveyorName || payload.userId || "Unknown") +
      "  |  " + (payload.city || "ND") +
      "  |  " + dateStr
    ).setHeading(DocumentApp.ParagraphHeading.HEADING2);

    body.appendParagraph(
      "Record ID : " + (payload.recordId || "—")      + "\n" +
      "User ID   : " + (payload.userId   || "—")      + "\n" +
      "Status    : " + (payload.status   || "approved") + "\n" +
      "GPS       : " + (payload.geoLat   || "0") + ", " + (payload.geoLng || "0") +
                       " (±" + Math.round(payload.geoAccuracy || 0) + " m)\n" +
      "Consent   : " + (payload.consentGiven ? "YES ✓" : "NO ✗")
    ).setHeading(DocumentApp.ParagraphHeading.NORMAL);

    body.appendParagraph("── Survey Answers ──")
        .setHeading(DocumentApp.ParagraphHeading.HEADING3);

    body.appendParagraph(answerText)
        .setHeading(DocumentApp.ParagraphHeading.NORMAL);

    /* NOTE: Do NOT call doc.saveAndClose() here.
       Changes flush automatically when the function returns.
       saveAndClose() causes "Document is closed" errors
       on subsequent operations and is the #1 cause of
       per-doc script failures. */

    var docId  = doc.getId();
    var docUrl = "https://docs.google.com/document/d/" + docId + "/edit";

    return ok({
      success  : true,
      docId    : docId,
      docUrl   : docUrl,
      docName  : doc.getName(),
      recordId : payload.recordId || ""
    });

  } catch (ex) {
    return err(ex.message || String(ex));
  }
}

/* ────────────────────────────────────────────────────────── */
/* Manual test — run this from Apps Script editor to verify   */
/* ────────────────────────────────────────────────────────── */
function testPost() {
  var fakeEvent = {
    postData: {
      contents: JSON.stringify({
        token          : "DCA_SECRET_TOKEN_2026",
        recordId       : "TEST-" + new Date().getTime(),
        userId         : "SC0101",
        displayName    : "Test Surveyor",
        city           : "Nagpur",
        instrumentType : "Urban Mobility Survey",
        geoLat         : "21.1458",
        geoLng         : "79.0882",
        geoAccuracy    : 5,
        geoTimestamp   : new Date().toISOString(),
        consentGiven   : true,
        status         : "approved",
        answers        : {
          "Q1: Mode of transport" : "Public bus",
          "Q2: Daily commute km"  : "12",
          "Q3: Satisfaction (1-5)": "3"
        }
      })
    }
  };

  var result = doPost(fakeEvent);
  Logger.log("Result: " + result.getContent());
}

/* ────────────────────────────────────────────────────────── */
/* Helpers                                                     */
/* ────────────────────────────────────────────────────────── */
function formatAnswers(answers) {
  if (!answers) return "(no answers recorded)";

  /* Already a plain string */
  if (typeof answers === "string") {
    return answers.trim() || "(no answers recorded)";
  }

  /* JSON object */
  if (typeof answers === "object" && !Array.isArray(answers)) {
    var keys = Object.keys(answers);
    if (keys.length === 0) return "(no answers recorded)";
    return keys.map(function(k) {
      return "  Q: " + k + "\n  A: " + (answers[k] !== undefined ? answers[k] : "(blank)");
    }).join("\n\n");
  }

  return String(answers);
}

function ok(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function err(msg) {
  return ContentService
    .createTextOutput(JSON.stringify({ success: false, error: msg }))
    .setMimeType(ContentService.MimeType.JSON);
}
