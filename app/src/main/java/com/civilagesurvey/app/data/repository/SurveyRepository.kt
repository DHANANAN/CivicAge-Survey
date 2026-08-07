package com.civilagesurvey.app.data.repository

import com.civilagesurvey.app.data.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SurveyRepository(private val surveyDao: SurveyDao) {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        const val APPS_SCRIPT_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbw-V2Mkj_HVV08L6kjCGGl0HLwJ-z-INEI4XI6tx4N8fO3nGX51-gV20yDpGgECSj4R/exec"
        const val DCA_SECRET_TOKEN = "DCA_SECRET_TOKEN_2026"
    }

    val allSurveys: Flow<List<SurveyEntity>> = surveyDao.getAllSurveys()
    val allPushRequests: Flow<List<PushRequestEntity>> = surveyDao.getAllPushRequests()
    val pendingSyncCount: Flow<Int> = surveyDao.getPendingSyncCount()
    val recentSyncLogs: Flow<List<SyncLogEntity>> = surveyDao.getRecentSyncLogs()
    val allQuestions: Flow<List<FormQuestionEntity>> = surveyDao.getAllQuestions()
    val allGoogleFormSources: Flow<List<GoogleFormSourceEntity>> = surveyDao.getAllGoogleFormSources()

    suspend fun getSurveyById(uid: String): SurveyEntity? = surveyDao.getSurveyById(uid)

    suspend fun insertSurvey(survey: SurveyEntity) {
        // Geo accuracy is enforced at the UI intake layer before session start.
        // We do NOT block DRAFT/COMPLETED inserts here — the timestamp was captured
        // at intake (which may be minutes before finalization), not at insert time.
        surveyDao.insertSurvey(survey)

        // Sync record to Firestore records/{recordId} on completion
        if (survey.status == "COMPLETED" || survey.status == "completed") {
            kotlin.runCatching {
                syncRecordToFirestore(survey)
            }
        }
    }

    suspend fun deleteSurvey(survey: SurveyEntity) = surveyDao.deleteSurvey(survey)

    suspend fun clearAllSurveys() = surveyDao.clearAllSurveys()

    suspend fun getPendingSyncSurveys(): List<SurveyEntity> = surveyDao.getPendingSyncSurveys()

    suspend fun getLastUserProfile(): UserProfileEntity? = surveyDao.getLastUserProfile()

    suspend fun upsertUserProfile(profile: UserProfileEntity) = surveyDao.upsertUserProfile(profile)

    suspend fun insertSyncLog(log: SyncLogEntity) = surveyDao.insertSyncLog(log)

    suspend fun clearSyncLogs() = surveyDao.clearSyncLogs()

    suspend fun insertQuestions(questions: List<FormQuestionEntity>) = surveyDao.insertQuestions(questions)

    suspend fun clearAllQuestions() = surveyDao.clearAllQuestions()

    suspend fun createPushRequest(request: PushRequestEntity): Boolean {
        surveyDao.insertPushRequest(request)
        return try {
            firestore.collection("pushRequests")
                .document(request.requestId)
                .set(request)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateRecordStatus(
        surveyUid: String,
        newStatus: String,
        qaNotes: String = "",
        approvedBy: String = ""
    ): Boolean {
        val existing = surveyDao.getSurveyById(surveyUid) ?: return false
        val updated = existing.copy(
            status = newStatus,
            qaNotes = qaNotes,
            approvedBy = if (newStatus == "approved") approvedBy else existing.approvedBy,
            approvedAt = if (newStatus == "approved") System.currentTimeMillis() else existing.approvedAt,
            updatedAt = System.currentTimeMillis()
        )
        surveyDao.insertSurvey(updated)

        // Trigger immediate Drive push via Apps Script if approved
        if (newStatus == "approved") {
            kotlin.runCatching {
                postRecordToAppsScript(updated)
            }
        }

        return try {
            val updateMap = mutableMapOf<String, Any>(
                "status" to newStatus,
                "qaNotes" to qaNotes,
                "updatedAt" to System.currentTimeMillis()
            )
            if (newStatus == "approved") {
                updateMap["approvedBy"] = approvedBy
                updateMap["approvedAt"] = System.currentTimeMillis()
            }
            firestore.collection("records")
                .document(surveyUid)
                .set(updateMap, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateInstrumentStatus(
        surveyUid: String,
        newStatus: String,
        qaNotes: String = "",
        approvedBy: String = ""
    ): Boolean = updateRecordStatus(surveyUid, newStatus, qaNotes, approvedBy)

    suspend fun syncRecordToFirestore(survey: SurveyEntity): Boolean {
        return try {
            val recordMap = mutableMapOf<String, Any>(
                "recordId" to survey.surveyUid,
                "userId" to survey.surveyorId,
                "displayName" to survey.surveyorName,
                "city" to survey.city,
                "instrumentType" to survey.instrumentType,
                "geoLat" to survey.latitude,
                "geoLng" to survey.longitude,
                "geoAccuracy" to survey.gpsAccuracy,
                "geoTimestamp" to survey.geoTimestamp,
                "consentGiven" to survey.consentGiven,
                "status" to survey.status,
                "answers" to (survey.answersJson ?: ""),
                "createdAt" to survey.createdAt,
                "updatedAt" to survey.updatedAt
            )
            firestore.collection("records")
                .document(survey.surveyUid)
                .set(recordMap, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun postRecordToAppsScript(survey: SurveyEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val answersObj = try {
                if (!survey.answersJson.isNullOrBlank()) JSONObject(survey.answersJson) else JSONObject()
            } catch (e: Exception) {
                JSONObject().put("rawAnswers", survey.answersJson ?: "")
            }

            val payload = JSONObject()
                .put("token", DCA_SECRET_TOKEN)
                .put("recordId", survey.surveyUid)
                .put("userId", survey.surveyorId)
                .put("displayName", survey.surveyorName)
                .put("city", survey.city.ifBlank { "ND" })
                .put("instrumentType", survey.instrumentType.ifBlank { "General_Survey" })
                .put("geoLat", survey.latitude)
                .put("geoLng", survey.longitude)
                .put("geoAccuracy", survey.gpsAccuracy)
                .put("geoTimestamp", survey.geoTimestamp)
                .put("consentGiven", survey.consentGiven)
                .put("status", survey.status)
                .put("answers", answersObj)

            val url = URL(APPS_SCRIPT_WEB_APP_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                instanceFollowRedirects = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 15000
                readTimeout = 15000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode in 200..309
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveConsentRecord(surveyUid: String, consent: Map<String, Any>): Boolean {
        return try {
            firestore.collection("consentRecords")
                .document(surveyUid)
                .set(consent)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
