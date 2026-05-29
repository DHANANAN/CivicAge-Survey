package com.nlud.civilagesurvey.data.repository

import com.example.data.database.FormQuestionEntity
import com.example.data.database.SurveyDao
import com.example.data.database.SurveyEntity
import com.example.data.database.SyncLogEntity
import com.example.data.database.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class SurveyRepository(private val surveyDao: SurveyDao) {

    val allSurveys: Flow<List<SurveyEntity>> = surveyDao.getAllSurveys()
    val allQuestions: Flow<List<FormQuestionEntity>> = surveyDao.getAllQuestions()
    val pendingSyncCount: Flow<Int> = surveyDao.getPendingSyncCount()
    val recentSyncLogs: Flow<List<SyncLogEntity>> = surveyDao.getRecentSyncLogs()

    fun getSurveysBySurveyor(surveyorId: String): Flow<List<SurveyEntity>> {
        return surveyDao.getSurveysBySurveyor(surveyorId)
    }

    suspend fun getSurveyById(surveyUid: String): SurveyEntity? {
        return surveyDao.getSurveyById(surveyUid)
    }

    suspend fun insertSurvey(survey: SurveyEntity) {
        surveyDao.insertSurvey(survey)
    }

    suspend fun updateSurvey(survey: SurveyEntity) {
        surveyDao.updateSurvey(survey)
    }

    suspend fun deleteSurvey(survey: SurveyEntity) {
        surveyDao.deleteSurvey(survey)
    }

    suspend fun getPendingSyncSurveys(): List<SurveyEntity> {
        return surveyDao.getPendingSyncSurveys()
    }

    suspend fun clearAllSurveys() {
        surveyDao.clearAllSurveys()
    }

    // Question Operations
    fun getQuestionsForForm(formId: String): Flow<List<FormQuestionEntity>> {
        return surveyDao.getQuestionsForForm(formId)
    }

    suspend fun insertQuestions(questions: List<FormQuestionEntity>) {
        surveyDao.insertQuestions(questions)
    }

    suspend fun clearQuestionsByFormId(formId: String) {
        surveyDao.clearQuestionsByFormId(formId)
    }

    suspend fun clearAllQuestions() {
        surveyDao.clearAllQuestions()
    }

    suspend fun getUserProfile(uid: String): UserProfileEntity? {
        return surveyDao.getUserProfile(uid)
    }

    suspend fun getLastUserProfile(): UserProfileEntity? {
        return surveyDao.getLastUserProfile()
    }

    suspend fun upsertUserProfile(profile: UserProfileEntity) {
        surveyDao.upsertUserProfile(profile)
    }

    suspend fun insertSyncLog(log: SyncLogEntity) {
        surveyDao.insertSyncLog(log)
    }

    suspend fun clearSyncLogs() {
        surveyDao.clearSyncLogs()
    }
}
