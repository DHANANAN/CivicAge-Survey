package com.nlud.civilagesurvey.data.repository

import android.content.Context
import com.example.data.database.SurveyEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class CloudSyncResult(
    val success: Boolean,
    val documentPath: String = "",
    val message: String
)

class CloudSyncRepository(
    private val context: Context
) {
    fun isConfigured(): Boolean = FirebaseRuntime.isConfigured(context)

    fun statusMessage(): String = FirebaseRuntime.statusMessage(context)

    suspend fun syncSurvey(
        survey: SurveyEntity,
        mcqResponses: Map<String, String>,
        aiThemes: List<String>,
        linkedFormId: String
    ): CloudSyncResult {
        if (!isConfigured()) {
            return CloudSyncResult(false, message = statusMessage())
        }

        val auth = authOrNull()
            ?: return CloudSyncResult(false, message = "Firebase Auth is not initialized.")
        val firestore = firestoreOrNull()
            ?: return CloudSyncResult(false, message = "Firestore is not initialized.")
        val currentUser = auth.currentUser
        val surveyorUid = currentUser?.uid ?: survey.surveyorUid
        if (surveyorUid.isBlank()) {
            return CloudSyncResult(false, message = "Firebase sign-in is required before cloud sync.")
        }

        return try {
            val document = firestore.collection("surveys").document(survey.surveyUid)
            val documentPath = "surveys/${survey.surveyUid}"
            val audioPaths = listOfNotNull(
                survey.consentAudioStoragePath.ifBlank { survey.participantVoiceSampleLocalPath ?: "" }.takeIf { it.isNotBlank() },
                survey.interviewAudioStoragePath.ifBlank { survey.interviewAudioLocalPath ?: "" }.takeIf { it.isNotBlank() }
            )

            val surveyDocument = mapOf(
                "surveyUid" to survey.surveyUid,
                "createdAt" to survey.createdAt,
                "updatedAt" to System.currentTimeMillis(),
                "status" to survey.status,
                "surveyorUid" to surveyorUid,
                "surveyorName" to survey.surveyorName,
                "surveyorEmail" to survey.surveyorEmail.ifBlank { currentUser?.email.orEmpty() },
                "surveyorId" to survey.surveyorId.ifBlank { survey.createdBy },
                "surveyorMobile" to survey.surveyorPhone,
                "respondentName" to survey.participantName,
                "respondentAge" to survey.participantAge,
                "respondentGender" to survey.participantGender,
                "respondentLocality" to survey.manualLocality,
                "consentStatus" to if (survey.consentGiven) "granted" else "not_granted",
                "consentTimestamp" to survey.consentTimestamp,
                "latitude" to survey.latitude,
                "longitude" to survey.longitude,
                "geotagAccuracy" to survey.gpsAccuracy,
                "photoPath" to survey.photoStoragePath.ifBlank { survey.participantPhotoLocalPath.orEmpty() },
                "photoDownloadUrl" to survey.photoDownloadUrl,
                "audioPaths" to audioPaths,
                "consentAudioDownloadUrl" to survey.consentAudioDownloadUrl,
                "interviewAudioDownloadUrl" to survey.interviewAudioDownloadUrl,
                "localRecordId" to survey.localRecordId,
                "firebaseSyncStatus" to "synced",
                "googleFormsSyncStatus" to survey.googleFormsSyncStatus,
                "linkedGoogleFormId" to linkedFormId.ifBlank { survey.linkedGoogleFormId },
                "linkedGoogleResponseId" to survey.linkedGoogleResponseId,
                "aiSummary" to survey.summaryText,
                "aiThemes" to aiThemes,
                "aiUrgencyTag" to survey.aiUrgencyTag,
                "jsonExportPath" to survey.jsonExportPath.orEmpty(),
                "jsonStoragePath" to survey.jsonStoragePath,
                "jsonDownloadUrl" to survey.jsonDownloadUrl
            )

            document.set(surveyDocument, SetOptions.merge()).await()

            for ((questionId, answer) in mcqResponses) {
                document.collection("mcqResponses")
                    .document(questionId)
                    .set(
                        mapOf(
                            "surveyUid" to survey.surveyUid,
                            "questionId" to questionId,
                            "answer" to answer,
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }

            val transcript = survey.reviewedTranscriptText.ifBlank { survey.transcriptText }
            if (transcript.isNotBlank()) {
                document.collection("interviewResponses")
                    .document("interview_full")
                    .set(
                        mapOf(
                            "surveyUid" to survey.surveyUid,
                            "questionId" to "interview_full",
                            "transcript" to transcript,
                            "audioPath" to survey.interviewAudioStoragePath.ifBlank { survey.interviewAudioLocalPath.orEmpty() },
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }

            val formId = linkedFormId.ifBlank { survey.linkedGoogleFormId }
            if (formId.isNotBlank()) {
                firestore.collection("forms")
                    .document(formId)
                    .set(
                        mapOf(
                            "formId" to formId,
                            "lastLinkedSurveyUid" to survey.surveyUid,
                            "updatedAt" to System.currentTimeMillis(),
                            "directGoogleFormsApi" to "backend-required"
                        ),
                        SetOptions.merge()
                    )
                    .await()

                firestore.collection("forms")
                    .document(formId)
                    .collection("responses")
                    .document(survey.surveyUid)
                    .set(
                        mapOf(
                            "surveyUid" to survey.surveyUid,
                            "surveyorEmail" to survey.surveyorEmail,
                            "respondentName" to survey.participantName,
                            "mcqResponses" to mcqResponses,
                            "interviewTranscript" to transcript,
                            "aiSummary" to survey.summaryText,
                            "aiThemes" to aiThemes,
                            "createdAt" to survey.createdAt,
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }

            firestore.collection("syncLogs")
                .document("${survey.surveyUid}_${System.currentTimeMillis()}")
                .set(
                    mapOf(
                        "surveyUid" to survey.surveyUid,
                        "target" to "firestore",
                        "status" to "synced",
                        "message" to "Survey synced to $documentPath",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            CloudSyncResult(true, documentPath, "Firestore sync completed.")
        } catch (e: Exception) {
            CloudSyncResult(false, message = e.message ?: "Firestore sync failed.")
        }
    }

    suspend fun countFirestoreSurveys(): Result<Long> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException(statusMessage()))
        }
        return try {
            val firestore = firestoreOrNull()
                ?: return Result.failure(IllegalStateException("Firestore is not initialized."))
            val result = firestore.collection("surveys").get().await()
            Result.success(result.size().toLong())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun authOrNull(): FirebaseAuth? {
        return if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance()
    }

    private fun firestoreOrNull(): FirebaseFirestore? {
        return if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()
    }
}
