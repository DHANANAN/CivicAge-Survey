package com.nlud.civilagesurvey.data.api

import com.example.config.Config
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Sends a transcription block to Gemini-3.5-flash.
     * Returns a JSON text string with keys "summary" and "themes".
     */
    suspend fun summarizeAndExtractThemes(transcript: String): String? {
        val apiKey = Config.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }

        val prompt = """
            You are an expert AI Smart City Researcher. Analyse this interview transcript from a senior citizen aged 75+ regarding city wellbeing, neighborhood quality, and public service difficulties.
            Perform two tasks:
            1. Summarize their feedback into 2-3 precise bullet points.
            2. Extract exactly which of these 10 predefined themes are present:
               - "mobility"
               - "healthcare"
               - "safety"
               - "digital access"
               - "public services"
               - "social isolation"
               - "accessibility"
               - "sanitation"
               - "transport"
               - "suggestions"
            
            Respond SOLELY in valid JSON format. Avoid wrappers, ticks or extra letters.
            {
              "summary": "• Point 1\n• Point 2",
              "themes": ["mobility", "healthcare"]
            }
            
            Transcript:
            "$transcript"
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.2f)
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
