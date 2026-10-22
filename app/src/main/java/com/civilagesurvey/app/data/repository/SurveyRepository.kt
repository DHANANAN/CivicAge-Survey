package com.civilagesurvey.app.data.repository

import com.civilagesurvey.app.data.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class SurveyRepository(private val surveyDao: SurveyDao) {
    private val firestore = FirebaseFirestore.getInstance()

    init {
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously()
            }
        } catch (e: Exception) {
            android.util.Log.e("DCA_SYNC", "init auth error: ${e.message}")
        }
    }

    suspend fun ensureFirebaseAuth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser != null) return@withContext true
            val task = auth.signInAnonymously().await()
            val uid = task.user?.uid
            android.util.Log.d("DCA_SYNC", "Firebase Anonymous Auth success: $uid")
            uid != null
        } catch (e: Exception) {
            android.util.Log.e("DCA_SYNC", "ensureFirebaseAuth error: ${e.message}")
            false
        }
    }

    companion object {
        const val APPS_SCRIPT_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbxJl6uhLpqFa-QkHuc9GFCrwTKaN_ju9JHbA-sy7O22pRrO9VzOJtsNqI4ZMdsrlyhb/exec"
        const val DCA_SECRET_TOKEN = "DCA_SECRET_TOKEN_2026"

        // Fallback / common doc (SC0113+ and unrecognised IDs)
        const val GOOGLE_DOC_URL = "https://docs.google.com/document/d/1pqmSuvsDRqnEQ-Yxw4Mh27-gYAEguwyLdrR6WvCCRU8/edit"

        /** Per-user dedicated docs for SC0101–SC0112 (matches Apps Script routing). */
        val USER_DOC_URL_MAP: Map<String, String> = mapOf(
            "SC0101" to "https://docs.google.com/document/d/1f6YxXWVt35bMnJsNzuHwzObN3N87WKsC38DAvIw4eCk/edit",
            "SC0102" to "https://docs.google.com/document/d/1BMMXDiXvr95y17S_zvIsjI98Bg5FnY7Ks3OCj6mLlPo/edit",
            "SC0103" to "https://docs.google.com/document/d/15wneFqNQz3OPL_tgMfnUY1JB_cFna6aNwzqxndRGaNs/edit",
            "SC0104" to "https://docs.google.com/document/d/1YQccrd6KBvwUpa4fXRY9UpY_-XkKiNHkktGT-9FPHdU/edit",
            "SC0105" to "https://docs.google.com/document/d/1hdtjT4itfIoHE8IUkYG7t4P6sZqt9g96Uinn_jcRNBk/edit",
            "SC0106" to "https://docs.google.com/document/d/1VpSDVbP1Gw7KnWbb6FI4DGLycOprmv1xTz2OW9nS8lc/edit",
            "SC0107" to "https://docs.google.com/document/d/1pJa9BoLLdc6Px0cmg02Zscm0k-ANaOjp0buY0LwopuM/edit",
            "SC0108" to "https://docs.google.com/document/d/1ANCJrhiSAouc98XXhQR2waDEAV7rgHHxI1TX48Ddx1A/edit",
            "SC0109" to "https://docs.google.com/document/d/1mL4maU2XkhKfTBeEXW2pElS_8ZTTzLFGsIHpHMfQ6LU/edit",
            "SC0110" to "https://docs.google.com/document/d/1U_wBWwYESM4MZsi0c-Ifbbl66dyZ5sTCF34_ZteG6Hc/edit",
            "SC0111" to "https://docs.google.com/document/d/1B6d4Chay-Q0Dxu6y9CD0gQsqo2LfcQhAR-Em0HBfQ4Q/edit",
            "SC0112" to "https://docs.google.com/document/d/1qUnuEa_EjtEqXMQ88dlCOSSPgt9RfdF_vZPU50taV3g/edit"
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
        ensureFirebaseAuth()
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
            android.util.Log.d("DCA_SYNC", "syncRecordToFirestore success for record ${survey.surveyUid}")
            true
        } catch (e: Exception) {
            android.util.Log.e("DCA_SYNC", "syncRecordToFirestore failed: ${e.message}")
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

            val body = payload.toString().toByteArray(Charsets.UTF_8)

            var currentConn = (URL(APPS_SCRIPT_WEB_APP_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 25000
                readTimeout    = 25000
            }

            currentConn.outputStream.use { it.write(body) }
            var code = currentConn.responseCode
            var location = currentConn.getHeaderField("Location")
            var redirectCount = 0

            // Follow HTTP redirects (Google Apps Script redirects with 302 to script.googleusercontent.com)
            while ((code in 300..399) && !location.isNullOrBlank() && redirectCount < 5) {
                currentConn.disconnect()
                redirectCount++
                android.util.Log.d("DCA_POST", "Redirect #$redirectCount (HTTP $code) to $location")

                val nextUrl = URL(location)
                currentConn = (nextUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 25000
                    readTimeout    = 25000
                }
                code = currentConn.responseCode
                location = currentConn.getHeaderField("Location")
            }

            val resp = try {
                (if (code in 200..299) currentConn.inputStream else currentConn.errorStream)
                    ?.bufferedReader()?.readText() ?: ""
            } catch (e: Exception) { "" }
            currentConn.disconnect()

            android.util.Log.d("DCA_POST", "HTTP $code (after $redirectCount redirects) → $resp")

            if (code in 200..299) {
                val json = kotlin.runCatching { JSONObject(resp) }.getOrNull()
                if (json != null) {
                    if (json.has("success")) json.optBoolean("success", true) else true
                } else {
                    true
                }
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("DCA_POST", "postRecordToAppsScript failed: ${e.message}", e)
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

    private var realTimeListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    /** Starts real-time cloud data tunnel listener for survey responses. Automatically upserts received surveys into Room. */
    fun startRealtimeFirestoreSync(onRecordReceived: (SurveyEntity) -> Unit = {}) {
        stopRealtimeFirestoreSync()
        try {
            realTimeListenerRegistration = firestore.collection("surveyResponses")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        val entity = parseSurveyFromMap(data)
                        if (entity != null) {
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                surveyDao.insertSurvey(entity)
                                onRecordReceived(entity)
                            }
                        }
                    }
                }
        } catch (ex: Exception) {
            android.util.Log.e("DCA_SYNC", "Failed to initialize Firestore listener: ${ex.message}")
        }
    }

    /** Stops active real-time Firestore listener. */
    fun stopRealtimeFirestoreSync() {
        try {
            realTimeListenerRegistration?.remove()
        } catch (e: Exception) {}
        realTimeListenerRegistration = null
    }

    /** Fetches all cloud records from both Firestore and Google Apps Script memory to update Master Board. */
    suspend fun fetchRemoteSurveysFromFirestore(): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            ensureFirebaseAuth()
            val snapshot = firestore.collection("surveyResponses").get().await()
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val entity = parseSurveyFromMap(data)
                if (entity != null) {
                    surveyDao.insertSurvey(entity)
                    count++
                }
            }
            val recordsSnapshot = firestore.collection("records").get().await()
            for (doc in recordsSnapshot.documents) {
                val data = doc.data ?: continue
                val entity = parseSurveyFromMap(data)
                if (entity != null) {
                    surveyDao.insertSurvey(entity)
                    count++
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DCA_SYNC", "fetchRemoteSurveysFromFirestore error: ${e.message}")
        }
        val appsScriptCount = fetchRemoteSurveysFromAppsScript()
        return@withContext (count + appsScriptCount)
    }

    /** Dual-Tunnel Backup: Fetches all survey records directly from Google Apps Script Web App memory. */
    suspend fun fetchRemoteSurveysFromAppsScript(): Int = withContext(Dispatchers.IO) {
        try {
            var count = 0
            val syncUrl = "$APPS_SCRIPT_WEB_APP_URL?action=getRecords&token=$DCA_SECRET_TOKEN"
            var currentConn = (URL(syncUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 20000
                readTimeout = 20000
            }
            var code = currentConn.responseCode
            var location = currentConn.getHeaderField("Location")
            var redirectCount = 0

            while ((code in 300..399) && !location.isNullOrBlank() && redirectCount < 5) {
                currentConn.disconnect()
                redirectCount++
                val nextUrl = URL(location)
                currentConn = (nextUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    connectTimeout = 20000
                    readTimeout = 20000
                }
                code = currentConn.responseCode
                location = currentConn.getHeaderField("Location")
            }

            if (code in 200..299) {
                val jsonText = currentConn.inputStream.bufferedReader().readText()
                val jsonObj = JSONObject(jsonText)
                if (jsonObj.optBoolean("success")) {
                    val arr = jsonObj.optJSONArray("records")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val rec = arr.getJSONObject(i)
                            val map = mutableMapOf<String, Any>()
                            val keys = rec.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                val valObj = rec.get(k)
                                if (valObj != JSONObject.NULL) {
                                    map[k] = valObj
                                }
                            }
                            val entity = parseSurveyFromMap(map)
                            if (entity != null) {
                                surveyDao.insertSurvey(entity)
                                count++
                            }
                        }
                    }
                }
            }
            currentConn.disconnect()
            android.util.Log.d("DCA_SYNC", "fetchRemoteSurveysFromAppsScript fetched $count records")
            count
        } catch (e: Exception) {
            android.util.Log.e("DCA_SYNC", "fetchRemoteSurveysFromAppsScript failed: ${e.message}")
            0
        }
    }

    /** Converts Firestore document data map to SurveyEntity object. */
    fun parseSurveyFromMap(data: Map<String, Any>): SurveyEntity? {
        val surveyUid = (data["surveyUid"] ?: data["recordId"]) as? String ?: return null
        if (surveyUid.isBlank()) return null

        return SurveyEntity(
            surveyUid = surveyUid,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: (data["geoTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            createdBy = (data["createdBy"] as? String) ?: (data["surveyorId"] as? String) ?: "",
            status = (data["status"] as? String) ?: "COMPLETED",
            consentGiven = (data["consentGiven"] as? Boolean) ?: true,
            participantName = (data["participantName"] as? String) ?: "",
            participantAge = (data["participantAge"] as? Number)?.toInt() ?: 0,
            participantAgeGroup = (data["participantAgeGroup"] as? String) ?: "",
            participantGender = (data["participantGender"] as? String) ?: "",
            participantPhone = (data["participantPhone"] as? String) ?: "",
            respondentName = (data["respondentName"] as? String) ?: (data["displayName"] as? String) ?: "",
            respondentAge = (data["respondentAge"] as? Number)?.toInt() ?: 0,
            latitude = (data["latitude"] as? Number)?.toDouble() ?: (data["geoLat"] as? Number)?.toDouble() ?: 0.0,
            longitude = (data["longitude"] as? Number)?.toDouble() ?: (data["geoLng"] as? Number)?.toDouble() ?: 0.0,
            gpsAccuracy = (data["gpsAccuracy"] as? Number)?.toFloat() ?: (data["geoAccuracy"] as? Number)?.toFloat() ?: 0f,
            geoTimestamp = (data["geoTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            manualLocality = (data["manualLocality"] as? String) ?: "",
            city = (data["city"] as? String) ?: "ND",
            instrumentType = (data["instrumentType"] as? String) ?: "General_Survey",
            answersJson = (data["answersJson"] as? String) ?: (data["answers"] as? String) ?: "{}",
            transcriptText = (data["transcriptText"] as? String) ?: "",
            manualSummary = (data["manualSummary"] as? String) ?: "",
            issueCategory = (data["issueCategory"] as? String) ?: "Other",
            manualUrgency = (data["manualUrgency"] as? String) ?: "Low",
            isSynced = true,
            lastSyncedAt = System.currentTimeMillis(),
            surveyorId = (data["surveyorId"] as? String) ?: (data["userId"] as? String) ?: "",
            surveyorName = (data["surveyorName"] as? String) ?: (data["displayName"] as? String) ?: ""
        )
    }
}
