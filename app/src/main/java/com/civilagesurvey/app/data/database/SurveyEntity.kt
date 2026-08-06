package com.civilagesurvey.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surveys")
data class SurveyEntity(
    @PrimaryKey
    val surveyUid: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val status: String = "draft", // "draft", "pending_qa", "approved", "rejected", "pending_geo"

    // Consent section
    val consentGiven: Boolean = false,
    val photoConsent: Boolean = false,
    val audioConsent: Boolean = false,
    val geoLocationConsent: Boolean = false,
    val dataUseConsent: Boolean = false,
    val participantCanWithdraw: Boolean = false,
    val consentTimestamp: Long = 0L,

    // Participant Profile
    val participantName: String = "",
    val participantAge: Int = 0,
    val participantAgeGroup: String = "", // e.g., "75-79", "80-84", "85+"
    val participantGender: String = "",
    val participantEmail: String = "",
    val participantPhone: String = "",
    val participantPhotoLocalPath: String? = null,
    val participantVoiceSampleLocalPath: String? = null,

    // Respondent / Caregiver Profile
    val respondentName: String = "",
    val respondentAge: Int = 0,
    val respondentEmail: String = "",
    val respondentPhone: String = "",
    val respondentRelation: String = "",

    // Geo-Tagging & Compliance
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val gpsAccuracy: Float = 0f,
    val geoTimestamp: Long = System.currentTimeMillis(),
    val geoStatus: String = "ok", // "ok", "denied", "unavailable"
    val geoTier: String = "strict", // "strict", "soft_warning"
    val manualLocality: String = "",
    val city: String = "ND",
    val instrumentType: String = "General_Survey",

    // QA & Approval Workflow
    val qaNotes: String = "",
    val approvedBy: String = "",
    val approvedAt: Long = 0L,
    val driveFileId: String = "",
    val drivePath: String = "",

    // Answers (Stored as JSON string mapping questionId to answerValue)
    val answersJson: String = "{}",

    // Interview Audio Recording
    val interviewAudioLocalPath: String? = null,
    val interviewAudioDurationSeconds: Int = 0,
    val transcriptionStatus: String = "not_started",
    val transcriptText: String = "",
    val reviewedTranscriptText: String = "",
    val summaryText: String = "",
    val themesExtractionJson: String = "[]", 
    val aiStatus: String = "not_started",
    val cleanedTranscriptText: String = "",
    val aiUrgencyTag: String = "Low", // Low, Medium, High
    val aiResearchJson: String = "",
    val aiFollowUpQuestion: String = "",
    
    // New Manual Fields
    val manualSummary: String = "",
    val issueCategory: String = "Other", // Health, Mobility, Safety, etc.
    val manualUrgency: String = "Low", // Low, Medium, High

    // Upload / Sync Metadata
    val isSynced: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val localRecordId: String = surveyUid,
    val firebaseSyncStatus: String = "pending",
    val firebaseError: String = "",
    val uploadStatus: String = "not_started",
    val uploadError: String = "",
    val googleFormsSyncStatus: String = "not_configured",
    val googleFormsError: String = "",
    val linkedGoogleFormId: String = "",
    val linkedGoogleResponseId: String = "",
    val photoStoragePath: String = "",
    val photoDownloadUrl: String = "",
    val consentAudioStoragePath: String = "",
    val consentAudioDownloadUrl: String = "",
    val interviewAudioStoragePath: String = "",
    val interviewAudioDownloadUrl: String = "",
    val jsonStoragePath: String = "",
    val jsonDownloadUrl: String = "",
    val firebaseDocumentPath: String = "",

    // Surveyor and Export details
    val surveyorUid: String = "",
    val surveyorId: String = "",
    val surveyorName: String = "",
    val surveyorEmail: String = "",
    val surveyorPhone: String = "",
    val surveyorRole: String = "normal",
    val emailReportStatus: String = "not_sent",
    val emailReportSentAt: Long = 0L,
    val jsonExportPath: String? = null,
    val transcriptExportPath: String? = null
)

@Entity(tableName = "push_requests")
data class PushRequestEntity(
    @PrimaryKey
    val requestId: String,
    val instrumentId: String,
    val requestedBy: String,
    val requestedAt: Long = System.currentTimeMillis(),
    val status: String = "open", // "open", "processed", "rejected"
    val processedBy: String = ""
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val uid: String,
    val fullName: String,
    val email: String,
    val surveyorId: String,
    val mobileNumber: String = "",
    val role: String = "normal",
    val allowedPushToDrive: Boolean = false,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val appearanceSettingsJson: String = "{}"
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey
    val logId: String,
    val surveyUid: String = "",
    val target: String,
    val status: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "form_questions")
data class FormQuestionEntity(
    @PrimaryKey
    val questionId: String,
    val formId: String,
    val section: String,
    val questionText: String,
    val questionAudioPath: String? = null,
    val type: String, // "single_choice", "text", "checkbox", "dropdown", "scale", "date", "time"
    val optionsJson: String, // JSON Array of options
    val isRequired: Boolean = true,
    val order: Int = 0,
    val isUnsupported: Boolean = false,
    val unsupportedReason: String? = null
)

@Entity(tableName = "google_form_sources")
data class GoogleFormSourceEntity(
    @PrimaryKey
    val extractedFormId: String,
    val originalUrl: String,
    val normalizedUrl: String,
    val formTitle: String,
    val formDescription: String?,
    val responderUri: String,
    val createdBy: String,
    val fetchedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceType: String = "GOOGLE_FORMS",
    val mirrorStatus: String = "FETCHED", // FETCHED, NEEDS_PERMISSION, UNSUPPORTED, ERROR
    val questionCount: Int = 0,
    val supportedQuestionCount: Int = 0,
    val unsupportedQuestionCount: Int = 0
)
