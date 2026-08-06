package com.civilagesurvey.app.data.repository

import com.civilagesurvey.app.data.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class SurveyRepository(private val surveyDao: SurveyDao) {
    private val firestore = FirebaseFirestore.getInstance()

    val allSurveys: Flow<List<SurveyEntity>> = surveyDao.getAllSurveys()
    val allPushRequests: Flow<List<PushRequestEntity>> = surveyDao.getAllPushRequests()
    val pendingSyncCount: Flow<Int> = surveyDao.getPendingSyncCount()
    val recentSyncLogs: Flow<List<SyncLogEntity>> = surveyDao.getRecentSyncLogs()
    val allQuestions: Flow<List<FormQuestionEntity>> = surveyDao.getAllQuestions()
    val allGoogleFormSources: Flow<List<GoogleFormSourceEntity>> = surveyDao.getAllGoogleFormSources()

    suspend fun getSurveyById(uid: String): SurveyEntity? = surveyDao.getSurveyById(uid)

    suspend fun insertSurvey(survey: SurveyEntity) {
        surveyDao.insertSurvey(survey)
        if (survey.status == "approved" || survey.status == "pending_qa") {
            kotlin.runCatching {
                syncSurveyToFirestore(survey)
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

    suspend fun updateInstrumentStatus(
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
            firestore.collection("instruments")
                .document(surveyUid)
                .set(updateMap, com.google.firebase.firestore.SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncSurveyToFirestore(survey: SurveyEntity): Boolean {
        return try {
            firestore.collection("instruments")
                .document(survey.surveyUid)
                .set(survey)
                .await()
            true
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
