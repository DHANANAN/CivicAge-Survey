/**
 * DCA Per-Doc Apps Script — v3.4
 * Deploy this script INSIDE each individual Google Doc
 * (Extensions → Apps Script, while the target doc is open).
 *
 * Each of the 12 docs (SC0101–SC0112) gets its own copy.
 * The script appends survey records directly into that doc.
 *
 * Token: DCA_SECRET_TOKEN_2026
 * Master password (for reference only): SCSCD@2026
 */

var EXPECTED_TOKEN = "DCA_SECRET_TOKEN_2026";

function doGet(e) {
  var doc = DocumentApp.getActiveDocument();
  return ContentService
    .createTextOutput(JSON.stringify({ status: "ok", doc: doc.getName() }))
    .setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  try {
    var payload = JSON.parse(e.postData.contents);
    if (payload.token !== EXPECTED_TOKEN) {
      return jsonResponse({ success: false, error: "Unauthorized" });
    }
    var doc  = DocumentApp.getActiveDocument();
    var body = doc.getBody();
    var now  = new Date();
    var dateStr = Utilities.formatDate(now, "Asia/Kolkata", "dd MMM yyyy, hh:mm a 'IST'");

    var answers = payload.answers || {};
    var answerLines = "";
    if (typeof answers === "object" && !Array.isArray(answers)) {
      var keys = Object.keys(answers);
      answerLines = keys.length > 0
        ? keys.map(function(k) { return "  Q: " + k + "\n  A: " + answers[k]; }).join("\n\n")
        : "(no answers recorded)";
    } else {
      answerLines = String(answers) || "(no answers recorded)";
    }

    body.appendParagraph("════════════════════════════════════════════════════════════")
        .setHeading(DocumentApp.ParagraphHeading.NORMAL);

    var hdr = body.appendParagraph(
      "📋 " + (payload.instrumentType || "Survey") +
      "  |  " + (payload.displayName || payload.userId || "Unknown") +
      "  |  " + dateStr
    );
    hdr.setHeading(DocumentApp.ParagraphHeading.HEADING2);

    body.appendParagraph(
      "Record ID: " + (payload.recordId || "—") + "\n" +
      "User ID:   " + (payload.userId   || "—") + "\n" +
      "GPS:       " + (payload.geoLat || "0") + ", " + (payload.geoLng || "0") + " (±" + Math.round(payload.geoAccuracy || 0) + "m)\n" +
      "Consent:   " + (payload.consentGiven ? "YES ✓" : "NO ✗")
    );

    body.appendParagraph("── Survey Answers ──").setHeading(DocumentApp.ParagraphHeading.HEADING3);
    body.appendParagraph(answerLines).setHeading(DocumentApp.ParagraphHeading.NORMAL);

    doc.saveAndClose();

    return jsonResponse({
      success: true,
      docId: doc.getId(),
      docUrl: "https://docs.google.com/document/d/" + doc.getId() + "/edit",
      recordId: payload.recordId
    });
  } catch (err) {
    return jsonResponse({ success: false, error: err.message });
  }
}

function jsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
