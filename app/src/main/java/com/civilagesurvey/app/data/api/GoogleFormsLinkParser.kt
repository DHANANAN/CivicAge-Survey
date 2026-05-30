package com.civilagesurvey.app.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object GoogleFormsLinkParser {
    private const val TAG = "GoogleFormsLinkParser"

    sealed class ParseResult {
        data class Success(val formId: String, val normalizedUrl: String) : ParseResult()
        object InvalidLink : ParseResult()
        object UnsupportedShortLink : ParseResult()
        object NetworkError : ParseResult()
    }

    fun parse(url: String): ParseResult {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ParseResult.InvalidLink

        // Standard Form URL: https://docs.google.com/forms/d/{FORM_ID}/viewform
        // or https://docs.google.com/forms/d/e/{FORM_ID}/viewform
        // or https://docs.google.com/forms/d/{FORM_ID}/edit
        val docsRegex = Regex("docs\\.google\\.com/forms/d/(?:e/)?([^/\\s?]+)")
        val docsMatch = docsRegex.find(trimmed)
        if (docsMatch != null) {
            val formId = docsMatch.groupValues[1]
            return ParseResult.Success(formId, "https://docs.google.com/forms/d/e/$formId/viewform")
        }

        // Short Link: https://forms.gle/{SHORT_ID}
        if (trimmed.contains("forms.gle/")) {
            return ParseResult.UnsupportedShortLink // Need to resolve redirect
        }

        return ParseResult.InvalidLink
    }

    suspend fun resolveAndParse(url: String): ParseResult = withContext(Dispatchers.IO) {
        val initialResult = parse(url)
        if (initialResult is ParseResult.UnsupportedShortLink) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connect()
                val expandedUrl = connection.getHeaderField("Location")
                connection.disconnect()
                if (expandedUrl != null) {
                    return@withContext parse(expandedUrl)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving short link", e)
                return@withContext ParseResult.NetworkError
            }
        }
        return@withContext initialResult
    }
}
