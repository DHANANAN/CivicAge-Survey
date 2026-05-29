package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surveys")
data class SurveyEntity(
    @PrimaryKey
    val surveyUid: String,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String,
    val status: String, // "DRAFT" or "COMPLETED"

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
    val participantPhotoLocalPath: String? = null,
    val participantVoiceSampleLocalPath: String? = null,

    // Geo-Tagging
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val gpsAccuracy: Float = 0f,
    val manualLocality: String = "",

    // Answers (Stored as JSON string mapping questionId to answerValue)
    val answersJson: String = "{}",

    // Interview Audio Recording
    val interviewAudioLocalPath: String? = null,
    val interviewAudioDurationSeconds: Int = 0,
    val transcriptionStatus: String = "not_started", // "not_started", "recording_saved", "uploading", "uploaded", "processing", "completed", "failed", "reviewed"
    val transcriptText: String = "",
    val reviewedTranscriptText: String = "",
    val summaryText: String = "",
    val themesExtractionJson: String = "[]", // list of themes as JSON array

    // Upload / Sync Metadata
    val isSynced: Boolean = false,
    val lastSyncedAt: Long = 0L,

    // Surveyor and Export details
    val surveyorName: String = "",
    val surveyorEmail: String = "",
    val surveyorPhone: String = "",
    val surveyorRole: String = "Field Surveyor",
    val emailReportStatus: String = "not_sent",
    val emailReportSentAt: Long = 0L,
    val jsonExportPath: String? = null,
    val transcriptExportPath: String? = null
)

@Entity(tableName = "form_questions")
data class FormQuestionEntity(
    @PrimaryKey
    val questionId: String,
    val formId: String,
    val section: String,
    val questionText: String,
    val questionAudioPath: String? = null,
    val type: String, // "single_choice", "text" etc.
    val optionsJson: String, // JSON Array of options: e.g. ["Very easy", "Somewhat easy", "Difficult"]
    val isRequired: Boolean = true
)
