/**
 * DCA Cloud Sync - Google Doc Appender v3.3
 * Routing: SC0101–SC0112 → each user's own dedicated Google Doc
 *          All other IDs  → common fallback Doc
 *
 * HOW TO DEPLOY:
 * 1. Open https://script.google.com and open your SCSCD project
 * 2. Select ALL existing code (Ctrl+A) and REPLACE with this file
 * 3. Save (Ctrl+S)
 * 4. Click Deploy → Manage deployments → pencil icon → New Version → Deploy
 * 5. Copy the Web App URL → paste into SurveyRepository.kt as APPS_SCRIPT_WEB_APP_URL
 *
 * Token used by the mobile app: DCA_SECRET_TOKEN_2026
 */

// ─── Common / fallback doc (used for SC0113+ and any unrecognized IDs) ─────────
const COMMON_DOC_ID  = "10U1MCP3_neriSMV47ch7OAD15Ev2FgtQjLW8svfXraQ";
const COMMON_DOC_URL = "https://docs.google.com/document/d/10U1MCP3_neriSMV47ch7OAD15Ev2FgtQjLW8svfXraQ/edit";
const SECRET_TOKEN   = "DCA_SECRET_TOKEN_2026";

// ─── Per-user Doc routing map (SC0101 – SC0112) ───────────────────────────────
const USER_DOC_MAP = {
  "SC0101": { id: "1wdTMizfwXXqYpsy5NPxYCug-I7vRACsWZZZ7KmtWF3w",
               url: "https://docs.google.com/document/d/1wdTMizfwXXqYpsy5NPxYCug-I7vRACsWZZZ7KmtWF3w/edit" },
  "SC0102": { id: "1Munad_OGvWbRuav4VfeK9KKXHVEYBAkAhpo0hRfVGzo",
               url: "https://docs.google.com/document/d/1Munad_OGvWbRuav4VfeK9KKXHVEYBAkAhpo0hRfVGzo/edit" },
  "SC0103": { id: "1WINw7vf7QfoufrYBVpM8200xX1R7u2QOIMcByX6yhok",
               url: "https://docs.google.com/document/d/1WINw7vf7QfoufrYBVpM8200xX1R7u2QOIMcByX6yhok/edit" },
  "SC0104": { id: "1Lb7_4ZhSGTjIiaonoiCXF6IYd4stNHPYEqCRtHCGZ6M",
               url: "https://docs.google.com/document/d/1Lb7_4ZhSGTjIiaonoiCXF6IYd4stNHPYEqCRtHCGZ6M/edit" },
  "SC0105": { id: "1wXeElRpfh8scbRIgAGuqxhHt-gM6a_CViqM-05TDhaw",
               url: "https://docs.google.com/document/d/1wXeElRpfh8scbRIgAGuqxhHt-gM6a_CViqM-05TDhaw/edit" },
  "SC0106": { id: "1uay6xdP1luqggV141ywnYy9ibAXIGkPrfCYmu0ENnvY",
               url: "https://docs.google.com/document/d/1uay6xdP1luqggV141ywnYy9ibAXIGkPrfCYmu0ENnvY/edit" },
  "SC0107": { id: "1QGHiZyEmALLSYfCPbO8smupd3_FKKTFGcx0Tfm8YEzA",
               url: "https://docs.google.com/document/d/1QGHiZyEmALLSYfCPbO8smupd3_FKKTFGcx0Tfm8YEzA/edit" },
  "SC0108": { id: "1TaWWC9xLOY1pJW4QXSjXOA9Ml89CKzD1VS_THRQAKZk",
               url: "https://docs.google.com/document/d/1TaWWC9xLOY1pJW4QXSjXOA9Ml89CKzD1VS_THRQAKZk/edit" },
  "SC0109": { id: "1F5HJ0rUOunJqOb7dipPyUFSyt2L0lgtjZHFxbtTahxA",
               url: "https://docs.google.com/document/d/1F5HJ0rUOunJqOb7dipPyUFSyt2L0lgtjZHFxbtTahxA/edit" },
  "SC0110": { id: "1LfTkNS2UEVNGQMkv1FPrp4uuPK3NxOBLFhJuiD64i_M",
               url: "https://docs.google.com/document/d/1LfTkNS2UEVNGQMkv1FPrp4uuPK3NxOBLFhJuiD64i_M/edit" },
  "SC0111": { id: "1rXfwAA5EacdrUr_ygMo4M9-n3ugydPYkkQ3IO_lAyR0",
               url: "https://docs.google.com/document/d/1rXfwAA5EacdrUr_ygMo4M9-n3ugydPYkkQ3IO_lAyR0/edit" },
  "SC0112": { id: "1RkN84SXU1_HH_Cny_KSi4-_lOy5FwmRWp7095KbdgoQ",
               url: "https://docs.google.com/document/d/1RkN84SXU1_HH_Cny_KSi4-_lOy5FwmRWp7095KbdgoQ/edit" }
};

// ─── Entry point ───────────────────────────────────────────────────────────────
function doPost(e) {
  try {
    var payload = JSON.parse(e.postData.contents);

    if (payload.token !== SECRET_TOKEN) {
      return json({ error: "Unauthorized" });
    }

    // Resolve the target doc for this user
    var userId   = (payload.userId || "").toUpperCase().trim();
    var target   = USER_DOC_MAP[userId] || null;
    var docId    = target ? target.id  : COMMON_DOC_ID;
    var docUrl   = target ? target.url : COMMON_DOC_URL;

    appendRecordToDoc(docId, payload);

    return json({
      success:  true,
      docUrl:   docUrl,
      routed:   target ? ("dedicated/" + userId) : "common",
      recordId: payload.recordId
    });

  } catch (err) {
    return json({ error: err.toString() });
  }
}

// ─── Core: open doc and append formatted record ────────────────────────────────
function appendRecordToDoc(docId, payload) {
  var doc  = DocumentApp.openById(docId);
  var body = doc.getBody();

  var now     = new Date();
  var dateStr = Utilities.formatDate(now, "Asia/Kolkata", "dd-MMM-yyyy HH:mm:ss");
  var divider = "════════════════════════════════════════════════════════════";

  body.appendParagraph(divider)
      .setHeading(DocumentApp.ParagraphHeading.NORMAL);

  // Header: instrument type | surveyor name | city | timestamp
  var hdr = body.appendParagraph(
    "📋 " + (payload.instrumentType || "Survey") +
    "  |  " + (payload.displayName || payload.surveyorName || payload.userId || "Unknown") +
    "  |  " + (payload.city || "ND") +
    "  |  " + dateStr
  );
  hdr.setHeading(DocumentApp.ParagraphHeading.HEADING2);

  // Meta block
  body.appendParagraph(
    "Record ID: " + (payload.recordId || "—") + "\n" +
    "User ID:   " + (payload.userId   || "—") + "\n" +
    "Surveyor:  " + (payload.surveyorName || payload.displayName || payload.userId || "—") + "\n" +
    "Status:    " + (payload.status || "approved") + "\n" +
    "GPS:       " + (payload.geoLat || "0") + ", " + (payload.geoLng || "0") +
                    " (±" + Math.round(payload.geoAccuracy || 0) + "m)\n" +
    "Consent:   " + (payload.consentGiven ? "YES ✓" : "NO ✗")
  ).setHeading(DocumentApp.ParagraphHeading.NORMAL);

  // Answers / responses
  var answers = payload.answers;
  if (answers && typeof answers === "object" && Object.keys(answers).length > 0) {
    body.appendParagraph("── Responses ──")
        .setHeading(DocumentApp.ParagraphHeading.HEADING3);
    var keys = Object.keys(answers);
    for (var i = 0; i < keys.length; i++) {
      var k   = keys[i];
      var val = answers[k];
      if (val !== null && val !== undefined && String(val).trim() !== "") {
        body.appendParagraph("Q: " + k + "\nA: " + String(val))
            .setHeading(DocumentApp.ParagraphHeading.NORMAL);
      }
    }
  } else if (typeof answers === "string" && answers.length > 0) {
    // Raw JSON string fallback
    body.appendParagraph("── Responses ──")
        .setHeading(DocumentApp.ParagraphHeading.HEADING3);
    body.appendParagraph(answers);
  }

  body.appendParagraph(" ");
  doc.saveAndClose();
}

// ─── Health-check GET ─────────────────────────────────────────────────────────
function doGet(e) {
  return json({
    status:  "DCA Doc Appender v3.3",
    routing: "SC0101-SC0112 → dedicated docs | others → common doc",
    commonDocId: COMMON_DOC_ID
  });
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
function json(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
