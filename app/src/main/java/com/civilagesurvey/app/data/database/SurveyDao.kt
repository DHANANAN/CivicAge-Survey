package com.civilagesurvey.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {
    @Query("SELECT * FROM surveys ORDER BY createdAt DESC")
    fun getAllSurveys(): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE surveyUid = :uid")
    suspend fun getSurveyById(uid: String): SurveyEntity?

    @Query("SELECT * FROM surveys WHERE firebaseSyncStatus = 'pending' OR uploadStatus = 'pending'")
    suspend fun getPendingSyncSurveys(): List<SurveyEntity>

    @Query("SELECT COUNT(*) FROM surveys WHERE firebaseSyncStatus = 'pending' OR uploadStatus = 'pending'")
    fun getPendingSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvey(survey: SurveyEntity)

    @Delete
    suspend fun deleteSurvey(survey: SurveyEntity)

    @Query("DELETE FROM surveys")
    suspend fun clearAllSurveys()

    // Push Requests
    @Query("SELECT * FROM push_requests ORDER BY requestedAt DESC")
    fun getAllPushRequests(): Flow<List<PushRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPushRequest(request: PushRequestEntity)

    // User Profile
    @Query("SELECT * FROM user_profiles ORDER BY lastLoginAt DESC LIMIT 1")
    suspend fun getLastUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    // Sync Logs
    @Query("SELECT * FROM sync_logs ORDER BY createdAt DESC LIMIT 50")
    fun getRecentSyncLogs(): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity)

    @Query("DELETE FROM sync_logs")
    suspend fun clearSyncLogs()

    // Questions
    @Query("SELECT * FROM form_questions")
    fun getAllQuestions(): Flow<List<FormQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<FormQuestionEntity>)

    @Query("DELETE FROM form_questions")
    suspend fun clearAllQuestions()

    // Google Form Sources
    @Query("SELECT * FROM google_form_sources ORDER BY updatedAt DESC")
    fun getAllGoogleFormSources(): Flow<List<GoogleFormSourceEntity>>

    @Query("SELECT * FROM google_form_sources WHERE extractedFormId = :formId")
    suspend fun getGoogleFormSourceById(formId: String): GoogleFormSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoogleFormSource(source: GoogleFormSourceEntity)

    @Delete
    suspend fun deleteGoogleFormSource(source: GoogleFormSourceEntity)
}
