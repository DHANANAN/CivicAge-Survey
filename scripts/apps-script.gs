/**
 * DCA Cloud Sync - Google Doc Appender v3.1
 * Doc: https://docs.google.com/document/d/10U1MCP3_neriSMV47ch7OAD15Ev2FgtQjLW8svfXraQ
 * Deploy: Execute as Me | Access: Anyone
 *
 * HOW TO DEPLOY:
 * 1. Paste entire file into Apps Script editor (Ctrl+A, then paste)
 * 2. Save (Ctrl+S)
 * 3. Deploy → Manage deployments → Edit pencil → New Version → Deploy
 * 4. Copy the Web App URL and update APPS_SCRIPT_WEB_APP_URL in SurveyRepository.kt
 */

const DOC_ID       = "10U1MCP3_neriSMV47ch7OAD15Ev2FgtQjLW8svfXraQ";
const SECRET_TOKEN = "DCA_SECRET_TOKEN_2026";
const DOC_URL      = "https://docs.google.com/document/d/10U1MCP3_neriSMV47ch7OAD15Ev2FgtQjLW8svfXraQ/edit";

function doPost(e) {
  try {
    var payload = JSON.parse(e.postData.contents);

    if (payload.token !== SECRET_TOKEN) {
      return json({ error: "Unauthorized" });
    }

    var doc  = DocumentApp.openById(DOC_ID);
    var body = doc.getBody();

    var now     = new Date();
    var dateStr = Utilities.formatDate(now, "Asia/Kolkata", "dd-MMM-yyyy HH:mm:ss");
    var divider = "════════════════════════════════════════════════════════════";

    body.appendParagraph(divider)
        .setHeading(DocumentApp.ParagraphHeading.NORMAL);

    // Record header
    var hdr = body.appendParagraph(
      "📋 " + (payload.instrumentType || "Survey") +
      "  |  " + (payload.displayName || payload.userId || "Unknown") +
      "  |  " + (payload.city || "ND") +
      "  |  " + dateStr
    );
    hdr.setHeading(DocumentApp.ParagraphHeading.HEADING2);

    // Meta info
    body.appendParagraph(
      "Record ID: " + (payload.recordId || "—") + "\n" +
      "Surveyor:  " + (payload.surveyorName || payload.userId || "—") + "\n" +
      "Status:    " + (payload.status || "approved") + "\n" +
      "GPS:       " + (payload.geoLat || "0") + ", " + (payload.geoLng || "0") +
                      " (±" + Math.round(payload.geoAccuracy || 0) + "m)\n" +
      "Consent:   " + (payload.consentGiven ? "YES ✓" : "NO ✗")
    ).setHeading(DocumentApp.ParagraphHeading.NORMAL);

    // Answers / responses
    var answers = payload.answers;
    if (answers && typeof answers === "object") {
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
      body.appendParagraph("── Responses ──")
          .setHeading(DocumentApp.ParagraphHeading.HEADING3);
      body.appendParagraph(answers);
    }

    body.appendParagraph(" ");
    doc.saveAndClose();

    return json({ success: true, docUrl: DOC_URL, recordId: payload.recordId });

  } catch (err) {
    return json({ error: err.toString() });
  }
}

function doGet(e) {
  return json({ status: "DCA Doc Appender v3.1", docId: DOC_ID });
}

function json(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
