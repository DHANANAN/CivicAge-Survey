/**
 * CivicAge Survey Web Application - API Client
 * Manages communication with Google Apps Script backend and cloud synchronisation.
 * Seamlessly interfaces with the Double-Doc routing engine.
 */

const API = {
  // Post a survey record to Apps Script
  async postRecord(survey, isApproved = false, qaNotes = '', approvedBy = '') {
    try {
      // Parse answers map
      let answersObj = {};
      if (typeof survey.answers === 'object' && survey.answers !== null) {
        answersObj = survey.answers;
      } else if (survey.answersJson) {
        try {
          answersObj = JSON.parse(survey.answersJson);
        } catch (e) {
          answersObj = { raw: survey.answersJson };
        }
      }

      // Include qualitative notes and interview transcripts
      if (survey.transcriptText && survey.transcriptText.trim()) {
        answersObj['Interview Notes & Transcript'] = survey.transcriptText.trim();
      }
      if (survey.manualSummary && survey.manualSummary.trim()) {
        answersObj['Field Summary'] = survey.manualSummary.trim();
      }

      // Resolve surveyor ID
      let resolvedUserId = (survey.surveyorId || survey.createdBy || '').trim().toUpperCase();
      if (!resolvedUserId) {
        const match = (survey.surveyUid || '').match(/SC01[0-1][0-9]/i);
        if (match) resolvedUserId = match[0].toUpperCase();
      }

      const status = isApproved ? 'approved' : (survey.status || 'submitted');

      const payload = {
        token: CONFIG.SECRET_TOKEN,
        recordId: survey.surveyUid,
        surveyUid: survey.surveyUid,
        userId: resolvedUserId,
        surveyorId: resolvedUserId,
        createdBy: survey.createdBy || resolvedUserId,
        displayName: survey.surveyorName || `Field Investigator ${resolvedUserId}`,
        city: survey.city || 'ND',
        manualLocality: survey.manualLocality || '',
        instrumentType: survey.instrumentType || 'Survey',
        geoLat: survey.latitude || 0,
        geoLng: survey.longitude || 0,
        geoAccuracy: survey.gpsAccuracy || 0,
        geoTimestamp: survey.geoTimestamp || Date.now(),
        consentGiven: survey.consentGiven !== false,
        status: status,
        approvedBy: approvedBy || survey.approvedBy || '',
        approvedAt: isApproved ? Date.now() : (survey.approvedAt || 0),
        qaNotes: qaNotes || survey.qaNotes || '',
        participantName: survey.participantName || 'Anonymous',
        participantAge: survey.participantAge || '',
        participantGender: survey.participantGender || '',
        participantPhone: survey.participantPhone || '',
        transcriptText: survey.transcriptText || '',
        manualSummary: survey.manualSummary || '',
        answers: answersObj
      };

      // Use text/plain to avoid browser CORS preflight blocks with Google Apps Script
      const response = await fetch(CONFIG.APPS_SCRIPT_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain;charset=utf-8'
        },
        body: JSON.stringify(payload),
        redirect: 'follow'
      });

      if (!response.ok) {
        throw new Error(`HTTP Error: ${response.status} ${response.statusText}`);
      }

      const result = await response.json();
      return result;

    } catch (err) {
      console.error('API.postRecord failed:', err);
      return {
        success: false,
        error: err.message || 'Network connection failed'
      };
    }
  },

  // Submit survey for Master Approval (does not clutter Google Docs with unapproved drafts)
  async submitSurvey(survey) {
    const updatedSurvey = {
      ...survey,
      status: 'submitted',
      isSynced: false,
      submittedAt: Date.now()
    };

    const result = await this.postRecord(updatedSurvey, false);

    if (result && result.success !== false) {
      updatedSurvey.isSynced = true;
      await DB.saveSurvey(updatedSurvey);
      return { success: true, survey: updatedSurvey, result };
    } else {
      // Save locally as submitted even if offline so user does not lose progress
      await DB.saveSurvey(updatedSurvey);
      return { success: false, survey: updatedSurvey, error: result.error || 'Submission failed. Saved locally.' };
    }
  },

  // Master Approval: Routes Full Survey to Field Doc and Audit to Master Doc
  async approveSurvey(surveyUid, qaNotes = '', approvedBy = 'Master Administrator') {
    const survey = await DB.getSurvey(surveyUid);
    if (!survey) {
      return { success: false, error: 'Survey not found in local records' };
    }

    const updatedSurvey = {
      ...survey,
      status: 'approved',
      qaNotes: qaNotes || 'Approved by Master',
      approvedBy: approvedBy,
      approvedAt: Date.now(),
      isSynced: false
    };

    const result = await this.postRecord(updatedSurvey, true, qaNotes, approvedBy);

    const docUrl = CONFIG.getDocUrlForSurveyor(updatedSurvey.surveyorId);

    if (result && result.success !== false) {
      updatedSurvey.isSynced = true;
      await DB.saveSurvey(updatedSurvey);
      return {
        success: true,
        survey: updatedSurvey,
        docUrl: docUrl,
        fieldDocId: result.fieldDocId,
        masterDocId: result.masterDocId,
        fieldDocWritten: result.fieldDocWritten,
        masterDocWritten: result.masterDocWritten
      };
    } else {
      await DB.saveSurvey(updatedSurvey);
      return {
        success: false,
        survey: updatedSurvey,
        docUrl: docUrl,
        error: result.error || 'Cloud sync failed. Status updated locally.'
      };
    }
  },

  // Master Rejection with QA Notes
  async rejectSurvey(surveyUid, qaNotes) {
    const survey = await DB.getSurvey(surveyUid);
    if (!survey) return { success: false, error: 'Record not found' };

    const updatedSurvey = {
      ...survey,
      status: 'rejected',
      qaNotes: qaNotes || 'Rejected by QA Review',
      approvedBy: '',
      updatedAt: Date.now()
    };

    const result = await this.postRecord(updatedSurvey, false, qaNotes, '');
    if (result && result.success !== false) {
      updatedSurvey.isSynced = true;
    }
    await DB.saveSurvey(updatedSurvey);
    return { success: true, survey: updatedSurvey };
  },

  // Fetch all cloud memory records from Apps Script
  async fetchCloudRecords() {
    try {
      const url = `${CONFIG.APPS_SCRIPT_URL}?action=getRecords&token=${encodeURIComponent(CONFIG.SECRET_TOKEN)}`;
      const response = await fetch(url, { method: 'GET', redirect: 'follow' });
      if (!response.ok) {
        throw new Error(`HTTP Error: ${response.status}`);
      }
      const data = await response.json();
      if (data && data.success && Array.isArray(data.records)) {
        const syncedCount = await DB.upsertSurveys(data.records);
        return { success: true, count: data.count || data.records.length, syncedCount, records: data.records };
      }
      return { success: false, error: data.error || 'Invalid server response' };
    } catch (err) {
      console.warn('API.fetchCloudRecords error (offline or network blocked):', err);
      return { success: false, error: err.message };
    }
  }
};
if (typeof window !== 'undefined') window.API = API;

