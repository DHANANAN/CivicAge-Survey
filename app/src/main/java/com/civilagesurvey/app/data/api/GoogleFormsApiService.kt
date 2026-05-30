package com.civilagesurvey.app.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

interface GoogleFormsApiService {
    @GET("v1/forms/{formId}")
    suspend fun getForm(
        @Path("formId") formId: String,
        @Header("Authorization") authHeader: String
    ): GoogleFormResponse

    companion object {
        private const val BASE_URL = "https://forms.googleapis.com/"

        fun create(): GoogleFormsApiService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GoogleFormsApiService::class.java)
        }
    }
}

// Data models for Google Forms API
data class GoogleFormResponse(
    val formId: String,
    val info: FormInfo,
    val items: List<FormItem>?,
    val responderUri: String
)

data class FormInfo(
    val title: String,
    val description: String?
)

data class FormItem(
    val itemId: String,
    val title: String?,
    val description: String?,
    val questionItem: QuestionItem?,
    val questionGroupItem: QuestionGroupItem?,
    val pageBreakItem: Map<String, Any>?,
    val textItem: Map<String, Any>?
)

data class QuestionItem(
    val question: Question
)

data class QuestionGroupItem(
    val questions: List<Question>
)

data class Question(
    val questionId: String,
    val required: Boolean?,
    val choiceQuestion: ChoiceQuestion?,
    val textQuestion: TextQuestion?,
    val scaleQuestion: ScaleQuestion?,
    val dateQuestion: DateQuestion?,
    val timeQuestion: TimeQuestion?
)

data class ChoiceQuestion(
    val type: String,
    val options: List<Option>
)

data class Option(
    val value: String
)

data class TextQuestion(
    val paragraph: Boolean?
)

data class ScaleQuestion(
    val low: Int,
    val high: Int,
    val lowLabel: String?,
    val highLabel: String?
)

data class DateQuestion(
    val includeTime: Boolean?,
    val includeYear: Boolean?
)

data class TimeQuestion(
    val duration: Boolean?
)
