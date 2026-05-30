package com.civilagesurvey.app.data.repository

import com.civilagesurvey.app.data.api.*
import com.civilagesurvey.app.data.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class GoogleFormsRepository(
    private val surveyDao: SurveyDao,
    private val apiService: GoogleFormsApiService
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val optionsAdapter = moshi.adapter(List::class.java)

    suspend fun fetchAndMirrorForm(url: String, accessToken: String, surveyorId: String): MirrorResult = withContext(Dispatchers.IO) {
        val parseResult = GoogleFormsLinkParser.resolveAndParse(url)
        if (parseResult !is GoogleFormsLinkParser.ParseResult.Success) {
            return@withContext MirrorResult.Error("Invalid or unsupported Google Form link.")
        }

        try {
            val response = apiService.getForm(parseResult.formId, "Bearer $accessToken")
            
            val questions = mutableListOf<FormQuestionEntity>()
            var supportedCount = 0
            var unsupportedCount = 0
            var order = 0

            response.items?.forEach { item ->
                if (item.questionItem != null) {
                    val q = item.questionItem.question
                    val type = determineType(q)
                    val isUnsupported = type == "unsupported"
                    if (isUnsupported) unsupportedCount++ else supportedCount++

                    questions.add(FormQuestionEntity(
                        questionId = q.questionId,
                        formId = response.formId,
                        section = "Default",
                        questionText = item.title ?: "No Title",
                        type = type,
                        optionsJson = serializeOptions(q),
                        isRequired = q.required ?: false,
                        order = order++,
                        isUnsupported = isUnsupported,
                        unsupportedReason = if (isUnsupported) "Question type not natively supported." else null
                    ))
                }
            }

            val source = GoogleFormSourceEntity(
                extractedFormId = response.formId,
                originalUrl = url,
                normalizedUrl = parseResult.normalizedUrl,
                formTitle = response.info.title,
                formDescription = response.info.description,
                responderUri = response.responderUri,
                createdBy = surveyorId,
                questionCount = questions.size,
                supportedQuestionCount = supportedCount,
                unsupportedQuestionCount = unsupportedCount
            )

            // Save locally
            surveyDao.insertGoogleFormSource(source)
            surveyDao.insertQuestions(questions)

            // Save to Firestore
            try {
                firestore.collection("googleFormSources")
                    .document(source.extractedFormId)
                    .set(source)
                    .await()
            } catch (e: Exception) {
                // Local save is sufficient for immediate use
            }

            return@withContext MirrorResult.Success(source)
        } catch (e: Exception) {
            return@withContext MirrorResult.Error(e.message ?: "Failed to fetch form from Google API.")
        }
    }

    private fun determineType(q: Question): String {
        return when {
            q.choiceQuestion != null -> {
                when (q.choiceQuestion.type) {
                    "RADIO" -> "single_choice"
                    "CHECKBOX" -> "checkbox"
                    "DROP_DOWN" -> "dropdown"
                    else -> "unsupported"
                }
            }
            q.textQuestion != null -> {
                if (q.textQuestion.paragraph == true) "paragraph" else "text"
            }
            q.scaleQuestion != null -> "scale"
            q.dateQuestion != null -> "date"
            q.timeQuestion != null -> "time"
            else -> "unsupported"
        }
    }

    private fun serializeOptions(q: Question): String {
        val options = q.choiceQuestion?.options?.map { it.value } ?: emptyList()
        return optionsAdapter.toJson(options)
    }

    sealed class MirrorResult {
        data class Success(val source: GoogleFormSourceEntity) : MirrorResult()
        data class Error(val message: String) : MirrorResult()
    }
}
