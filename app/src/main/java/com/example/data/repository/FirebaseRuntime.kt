package com.nlud.civilagesurvey.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp

object FirebaseRuntime {
    fun statusMessage(context: Context): String {
        val apps = FirebaseApp.getApps(context)
        if (apps.isEmpty()) {
            return "Firebase is not initialized. Add app/google-services.json from Firebase Console."
        }

        val options = apps.first().options
        return when {
            options.projectId.isNullOrBlank() -> "Firebase project id is missing in google-services.json."
            options.projectId == "dummy-project-id" -> "app/google-services.json is still a placeholder. Replace it with the civilage-survey Firebase config."
            options.apiKey.isNullOrBlank() -> "Firebase API key is missing in google-services.json."
            options.apiKey == "dummy_api_key_placeholder" -> "Firebase API key is a placeholder. Download the real google-services.json."
            options.applicationId.contains("0000000000000000000000") -> "Firebase app id is a placeholder. Download the real google-services.json."
            else -> "Firebase configured for project ${options.projectId}."
        }
    }

    fun isConfigured(context: Context): Boolean {
        return statusMessage(context).startsWith("Firebase configured")
    }
}
