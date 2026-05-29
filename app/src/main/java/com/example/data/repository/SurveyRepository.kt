package com.example.data.repository

import com.example.data.database.FormQuestionEntity
import com.example.data.database.SurveyDao
import com.example.data.database.SurveyEntity
import kotlinx.coroutines.flow.Flow

class SurveyRepository(private val surveyDao: SurveyDao) {

    val allSurveys: Flow<List<SurveyEntity>> = surveyDao.getAllSurveys()
    val allQuestions: Flow<List<FormQuestionEntity>> = surveyDao.getAllQuestions()

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
}
