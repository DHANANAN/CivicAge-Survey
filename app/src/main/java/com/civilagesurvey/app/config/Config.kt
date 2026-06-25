package com.civilagesurvey.app.config

import com.civilagesurvey.app.BuildConfig

object Config {
    val ADMIN_EMAIL: String = BuildConfig.ADMIN_EMAIL.ifBlank { "mdc18042007@gmail.com" }.trim().lowercase()
    val DEFAULT_SURVEYOR_ID: String = BuildConfig.DEFAULT_SURVEYOR_ID.ifBlank { "CAS-ADMIN-001" }
    val DEFAULT_FORM_ID: String = BuildConfig.DEFAULT_FORM_ID
    val FORMS_WEB_APP_URL: String = BuildConfig.FORMS_WEB_APP_URL
    val FORMS_SHARED_SECRET: String = BuildConfig.FORMS_SHARED_SECRET
    const val GOOGLE_WEB_CLIENT_ID = "777856548247-vrksj3qs9vj8fmm7up1lf22ln2dkjl5i.apps.googleusercontent.com"

    const val ADMIN_ROLE = "Admin/Test Admin"
    const val FIELD_SURVEYOR_ROLE = "Field Surveyor"
    const val FIREBASE_PROJECT_ID_HINT = "civilage-survey"
    const val FIREBASE_PROJECT_NUMBER_HINT = "777856548247"

    fun roleForEmail(email: String): String {
        val cleanEmail = email.trim().lowercase()
        return if (cleanEmail == ADMIN_EMAIL || cleanEmail == "scpdata01@gmail.com") ADMIN_ROLE else FIELD_SURVEYOR_ROLE
    }
}
