package com.dhananan.civicage.data.api

import com.example.config.Config
import com.example.data.database.SurveyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FormsEndpointResult(
    val success: Boolean,
    val responseId: String = "",
    val message: String
)

class FormsEndpointClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun postSurveyResponse(
        survey: SurveyEntity,
        mcqResponses: Map<String, String>,
        themes: List<String>
    ): FormsEndpointResult = withContext(Dispatchers.IO) {
        if (Config.FORMS_WEB_APP_URL.isBlank()) {
            return@withContext FormsEndpointResult(
                success = false,
                message = "Google Forms/Sheets direct API is backend-required. Add FORMS_WEB_APP_URL in local.properties for the Apps Script endpoint."
            )
        }

        val payload = JSONObject()
            .put("action", "submitSurvey")
            .put("secret", Config.FORMS_SHARED_SECRET)
            .put("surveyUid", survey.surveyUid)
            .put("surveyorName", survey.surveyorName)
            .put("surveyorEmail", survey.surveyorEmail)
            .put("surveyorId", survey.surveyorId.ifBlank { survey.createdBy })
            .put("respondentName", survey.participantName)
            .put("respondentAge", survey.participantAge)
            .put("respondentLocality", survey.manualLocality)
            .put("consentStatus", if (survey.consentGiven) "granted" else "not_granted")
            .put("latitude", survey.latitude)
            .put("longitude", survey.longitude)
            .put("mcqResponses", JSONObject(mcqResponses))
            .put("interviewTranscript", survey.reviewedTranscriptText.ifBlank { survey.transcriptText })
            .put("aiSummary", survey.summaryText)
            .put("aiThemes", JSONArray(themes))
            .put("urgency", survey.aiUrgencyTag)
            .put("createdAt", survey.createdAt)

        return@withContext execute(payload)
    }

    suspend fun sendFormAction(action: String, formId: String): FormsEndpointResult = withContext(Dispatchers.IO) {
        if (Config.FORMS_WEB_APP_URL.isBlank()) {
            return@withContext FormsEndpointResult(
                success = false,
                message = "Set FORMS_WEB_APP_URL to use Apps Script for $action. Direct Google Forms API calls stay on the backend."
            )
        }

        val payload = JSONObject()
            .put("action", action)
            .put("secret", Config.FORMS_SHARED_SECRET)
            .put("formId", formId)

        return@withContext execute(payload)
    }

    private fun execute(payload: JSONObject): FormsEndpointResult {
        val request = Request.Builder()
            .url(Config.FORMS_WEB_APP_URL)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val responseId = runCatching { JSONObject(body).optString("responseId") }.getOrDefault("")
                    FormsEndpointResult(true, responseId, body.ifBlank { "Endpoint accepted payload." })
                } else {
                    FormsEndpointResult(false, message = "Endpoint returned HTTP ${response.code}: $body")
                }
            }
        } catch (e: Exception) {
            FormsEndpointResult(false, message = e.message ?: "Forms endpoint request failed.")
        }
    }
}
