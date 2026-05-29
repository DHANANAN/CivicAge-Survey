package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {
    @Query("SELECT * FROM surveys ORDER BY createdAt DESC")
    fun getAllSurveys(): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE surveyUid = :surveyUid LIMIT 1")
    suspend fun getSurveyById(surveyUid: String): SurveyEntity?

    @Query("SELECT * FROM surveys WHERE createdBy = :surveyorId ORDER BY createdAt DESC")
    fun getSurveysBySurveyor(surveyorId: String): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE isSynced = 0 OR firebaseSyncStatus IN ('pending', 'failed') OR uploadStatus IN ('pending', 'failed') ORDER BY updatedAt ASC")
    suspend fun getPendingSyncSurveys(): List<SurveyEntity>

    @Query("SELECT COUNT(*) FROM surveys WHERE isSynced = 0 OR firebaseSyncStatus IN ('pending', 'failed') OR uploadStatus IN ('pending', 'failed')")
    fun getPendingSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvey(survey: SurveyEntity)

    @Update
    suspend fun updateSurvey(survey: SurveyEntity)

    @Delete
    suspend fun deleteSurvey(survey: SurveyEntity)

    // Form Question Operations
    @Query("SELECT * FROM form_questions ORDER BY questionId ASC")
    fun getAllQuestions(): Flow<List<FormQuestionEntity>>

    @Query("SELECT * FROM form_questions WHERE formId = :formId ORDER BY questionId ASC")
    fun getQuestionsForForm(formId: String): Flow<List<FormQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<FormQuestionEntity>)

    @Query("DELETE FROM form_questions")
    suspend fun clearAllQuestions()

    @Query("DELETE FROM form_questions WHERE formId = :formId")
    suspend fun clearQuestionsByFormId(formId: String)

    // Local user profile operations
    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getUserProfile(uid: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles ORDER BY lastLoginAt DESC LIMIT 1")
    suspend fun getLastUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    // Local sync log operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity)

    @Query("SELECT * FROM sync_logs ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentSyncLogs(limit: Int = 50): Flow<List<SyncLogEntity>>

    @Query("DELETE FROM surveys")
    suspend fun clearAllSurveys()

    @Query("DELETE FROM sync_logs")
    suspend fun clearSyncLogs()
}
