package com.civilagesurvey.app.data.repository

import com.civilagesurvey.app.data.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class SurveyRepository(private val surveyDao: SurveyDao) {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        const val APPS_SCRIPT_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbwQ0C5ZiRV8FEJjnNlYMYvhqA91YLu35U8uzYI2zISm7PL8hQFX7D48BT2zboJmjQfLLg/exec"
        const val DCA_SECRET_TOKEN = "DCA_SECRET_TOKEN_2026"
        // Fallback / common doc (SC0113+ and unrecognised IDs)
        const val GOOGLE_DOC_URL = "https://docs.google.com/document/d/10U1MCP3_neriSMV47ch7OAD15Ev2FgtQjLW8svfXraQ/edit"

        /** Per-user dedicated docs for SC0101–SC0112 (matches Apps Script routing). */
        val USER_DOC_URL_MAP: Map<String, String> = mapOf(
            "SC0101" to "https://docs.google.com/document/d/1wdTMizfwXXqYpsy5NPxYCug-I7vRACsWZZZ7KmtWF3w/edit",
            "SC0102" to "https://docs.google.com/document/d/1Munad_OGvWbRuav4VfeK9KKXHVEYBAkAhpo0hRfVGzo/edit",
            "SC0103" to "https://docs.google.com/document/d/1WINw7vf7QfoufrYBVpM8200xX1R7u2QOIMcByX6yhok/edit",
            "SC0104" to "https://docs.google.com/document/d/1Lb7_4ZhSGTjIiaonoiCXF6IYd4stNHPYEqCRtHCGZ6M/edit",
            "SC0105" to "https://docs.google.com/document/d/1wXeElRpfh8scbRIgAGuqxhHt-gM6a_CViqM-05TDhaw/edit",
            "SC0106" to "https://docs.google.com/document/d/1uay6xdP1luqggV141ywnYy9ibAXIGkPrfCYmu0ENnvY/edit",
            "SC0107" to "https://docs.google.com/document/d/1QGHiZyEmALLSYfCPbO8smupd3_FKKTFGcx0Tfm8YEzA/edit",
            "SC0108" to "https://docs.google.com/document/d/1TaWWC9xLOY1pJW4QXSjXOA9Ml89CKzD1VS_THRQAKZk/edit",
            "SC0109" to "https://docs.google.com/document/d/1F5HJ0rUOunJqOb7dipPyUFSyt2L0lgtjZHFxbtTahxA/edit",
            "SC0110" to "https://docs.google.com/document/d/1LfTkNS2UEVNGQMkv1FPrp4uuPK3NxOBLFhJuiD64i_M/edit",
            "SC0111" to "https://docs.google.com/document/d/1rXfwAA5EacdrUr_ygMo4M9-n3ugydPYkkQ3IO_lAyR0/edit",
            "SC0112" to "https://docs.google.com/document/d/1RkN84SXU1_HH_Cny_KSi4-_lOy5FwmRWp7095KbdgoQ/edit"
        )

        /** Returns the target Google Doc URL for the given surveyor ID. */
        fun docUrlForUser(userId: String): String =
            USER_DOC_URL_MAP[userId.trim().uppercase()] ?: GOOGLE_DOC_URL
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
                "surveyUid" to survey.surveyUid,
                "recordId" to survey.surveyUid,
                "surveyorId" to survey.surveyorId,
                "userId" to survey.surveyorId,
                "surveyorName" to survey.surveyorName,
                "displayName" to survey.surveyorName,
                "participantName" to survey.participantName,
                "city" to survey.city,
                "instrumentType" to survey.instrumentType,
                "latitude" to survey.latitude,
                "longitude" to survey.longitude,
                "geoLat" to survey.latitude,
                "geoLng" to survey.longitude,
                "gpsAccuracy" to survey.gpsAccuracy,
                "geoAccuracy" to survey.gpsAccuracy,
                "geoTimestamp" to survey.geoTimestamp,
                "consentGiven" to survey.consentGiven,
                "status" to survey.status,
                "answersJson" to (survey.answersJson ?: ""),
                "answers" to (survey.answersJson ?: ""),
                "createdAt" to survey.createdAt,
                "updatedAt" to survey.updatedAt,
                "isSynced" to survey.isSynced,
                "createdBy" to survey.createdBy
            )
            // Write to surveyResponses — picked up by Master real-time listener
            firestore.collection("surveyResponses")
                .document(survey.surveyUid)
                .set(recordMap, SetOptions.merge())
                .await()
            // Also update records collection for approval queries
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

            val body = payload.toString()

            /**
             * Apps Script Web Apps always return HTTP 302 → real execution URL.
             * Android's HttpURLConnection drops the POST body on auto-redirect.
             * Fix: disable instanceFollowRedirects, read Location header from 302,
             * then POST again to that URL with the full body.
             */
            fun openPost(targetUrl: String): HttpURLConnection =
                (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    instanceFollowRedirects = false // MUST be false
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 20000
                    readTimeout    = 20000
                }

            // First POST → likely a 302
            var conn = openPost(APPS_SCRIPT_WEB_APP_URL)
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            var code = conn.responseCode

            if (code in 300..399) {
                // Follow the redirect, re-sending the POST body
                val location = conn.getHeaderField("Location") ?: ""
                conn.disconnect()
                if (location.isNotBlank()) {
                    conn = openPost(location)
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    code = conn.responseCode
                }
            }

            val resp = try {
                (conn.inputStream ?: conn.errorStream)?.bufferedReader()?.readText() ?: ""
            } catch (e: Exception) { "" }
            conn.disconnect()

            android.util.Log.d("DCA_POST", "HTTP $code → $resp")
            code in 200..299


        } catch (e: Exception) {
            android.util.Log.e("DCA_POST", "postRecordToAppsScript failed: ${e.message}")
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
