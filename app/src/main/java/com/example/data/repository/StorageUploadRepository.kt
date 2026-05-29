package com.nlud.civilagesurvey.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.database.SurveyEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

data class StorageUploadResult(
    val success: Boolean,
    val fields: Map<String, String>,
    val message: String
)

class StorageUploadRepository(
    private val context: Context
) {
    fun isConfigured(): Boolean = FirebaseRuntime.isConfigured(context)

    suspend fun uploadSurveyFiles(survey: SurveyEntity): StorageUploadResult {
        if (!isConfigured()) {
            return StorageUploadResult(false, emptyMap(), FirebaseRuntime.statusMessage(context))
        }
        val storage = storageOrNull()
            ?: return StorageUploadResult(false, emptyMap(), "Firebase Storage is not initialized.")

        val uploadedFields = linkedMapOf<String, String>()
        val failures = mutableListOf<String>()
        val timestamp = System.currentTimeMillis()

        uploadIfPresent(
            storage = storage,
            localPath = survey.participantPhotoLocalPath,
            storagePath = "surveys/${survey.surveyUid}/photo/respondent_photo.jpg",
            pathField = "photoStoragePath",
            urlField = "photoDownloadUrl",
            fields = uploadedFields,
            failures = failures
        )

        uploadIfPresent(
            storage = storage,
            localPath = survey.participantVoiceSampleLocalPath,
            storagePath = "surveys/${survey.surveyUid}/audio/consent_sample.m4a",
            pathField = "consentAudioStoragePath",
            urlField = "consentAudioDownloadUrl",
            fields = uploadedFields,
            failures = failures
        )

        uploadIfPresent(
            storage = storage,
            localPath = survey.interviewAudioLocalPath,
            storagePath = "surveys/${survey.surveyUid}/audio/interview_main_$timestamp.m4a",
            pathField = "interviewAudioStoragePath",
            urlField = "interviewAudioDownloadUrl",
            fields = uploadedFields,
            failures = failures
        )

        uploadIfPresent(
            storage = storage,
            localPath = survey.jsonExportPath,
            storagePath = "surveys/${survey.surveyUid}/json/survey_export.json",
            pathField = "jsonStoragePath",
            urlField = "jsonDownloadUrl",
            fields = uploadedFields,
            failures = failures
        )

        return if (failures.isEmpty()) {
            StorageUploadResult(true, uploadedFields, "Storage upload completed.")
        } else {
            StorageUploadResult(false, uploadedFields, failures.joinToString("; "))
        }
    }

    private suspend fun uploadIfPresent(
        storage: FirebaseStorage,
        localPath: String?,
        storagePath: String,
        pathField: String,
        urlField: String,
        fields: MutableMap<String, String>,
        failures: MutableList<String>
    ) {
        if (localPath.isNullOrBlank()) return
        val file = File(localPath)
        if (!file.exists()) {
            failures.add("Local file missing: $localPath")
            return
        }

        try {
            val ref = storage.reference.child(storagePath)
            ref.putFile(Uri.fromFile(file)).await()
            val url = ref.downloadUrl.await().toString()
            fields[pathField] = storagePath
            fields[urlField] = url
        } catch (e: Exception) {
            failures.add("${file.name}: ${e.message ?: "upload failed"}")
        }
    }

    private fun storageOrNull(): FirebaseStorage? {
        return if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseStorage.getInstance()
    }
}
