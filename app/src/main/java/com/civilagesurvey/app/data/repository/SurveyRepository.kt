package com.civilagesurvey.app.data.repository

import com.civilagesurvey.app.data.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class SurveyRepository(private val surveyDao: SurveyDao) {
    private val firestore = FirebaseFirestore.getInstance()

    val allSurveys: Flow<List<SurveyEntity>> = surveyDao.getAllSurveys()
    val pendingSyncCount: Flow<Int> = surveyDao.getPendingSyncCount()
    val recentSyncLogs: Flow<List<SyncLogEntity>> = surveyDao.getRecentSyncLogs()
    val allQuestions: Flow<List<FormQuestionEntity>> = surveyDao.getAllQuestions()
    val allGoogleFormSources: Flow<List<GoogleFormSourceEntity>> = surveyDao.getAllGoogleFormSources()

    suspend fun getSurveyById(uid: String): SurveyEntity? = surveyDao.getSurveyById(uid)

    suspend fun insertSurvey(survey: SurveyEntity) = surveyDao.insertSurvey(survey)

    suspend fun deleteSurvey(survey: SurveyEntity) = surveyDao.deleteSurvey(survey)

    suspend fun clearAllSurveys() = surveyDao.clearAllSurveys()

    suspend fun getPendingSyncSurveys(): List<SurveyEntity> = surveyDao.getPendingSyncSurveys()

    suspend fun getLastUserProfile(): UserProfileEntity? = surveyDao.getLastUserProfile()

    suspend fun upsertUserProfile(profile: UserProfileEntity) = surveyDao.upsertUserProfile(profile)

    suspend fun insertSyncLog(log: SyncLogEntity) = surveyDao.insertSyncLog(log)

    suspend fun clearSyncLogs() = surveyDao.clearSyncLogs()

    suspend fun insertQuestions(questions: List<FormQuestionEntity>) = surveyDao.insertQuestions(questions)

    suspend fun clearAllQuestions() = surveyDao.clearAllQuestions()

    suspend fun syncSurveyToFirestore(survey: SurveyEntity): Boolean {
        return try {
            firestore.collection("surveyResponses")
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
