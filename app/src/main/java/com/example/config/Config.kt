package com.nlud.civilagesurvey.config

import com.nlud.civilagesurvey.BuildConfig

object Config {
    val ADMIN_EMAIL: String = BuildConfig.ADMIN_EMAIL.ifBlank { "mdc18042007@gmail.com" }.trim().lowercase()
    val DEFAULT_SURVEYOR_ID: String = BuildConfig.DEFAULT_SURVEYOR_ID.ifBlank { "CAS-ADMIN-001" }
    val DEFAULT_FORM_ID: String = BuildConfig.DEFAULT_FORM_ID
    val FORMS_WEB_APP_URL: String = BuildConfig.FORMS_WEB_APP_URL
    val FORMS_SHARED_SECRET: String = BuildConfig.FORMS_SHARED_SECRET
    val GEMINI_API_KEY: String = BuildConfig.GEMINI_API_KEY

    const val ADMIN_ROLE = "Admin/Test Admin"
    const val FIELD_SURVEYOR_ROLE = "Field Surveyor"
    const val FIREBASE_PROJECT_ID_HINT = "civilage-survey"
    const val FIREBASE_PROJECT_NUMBER_HINT = "777856548247"

    fun roleForEmail(email: String): String {
        return if (email.trim().lowercase() == ADMIN_EMAIL) ADMIN_ROLE else FIELD_SURVEYOR_ROLE
    }
}

