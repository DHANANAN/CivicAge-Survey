package com.civilagesurvey.app.config

import com.civilagesurvey.app.BuildConfig

object Config {
    val ADMIN_EMAIL: String = BuildConfig.ADMIN_EMAIL.ifBlank { "mdc18042007@gmail.com" }.trim().lowercase()
    val DEFAULT_SURVEYOR_ID: String = BuildConfig.DEFAULT_SURVEYOR_ID.ifBlank { "CAS-ADMIN-001" }
    val DEFAULT_FORM_ID: String = BuildConfig.DEFAULT_FORM_ID
    val FORMS_WEB_APP_URL: String = BuildConfig.FORMS_WEB_APP_URL
    val FORMS_SHARED_SECRET: String = BuildConfig.FORMS_SHARED_SECRET
    const val GOOGLE_WEB_CLIENT_ID = "777856548247-vrksj3qs9vj8fmm7up1lf22ln2dkjl5i.apps.googleusercontent.com"

    // Role definitions
    const val ROLE_MASTER = "master"
    const val ROLE_QA = "qa"
    const val ROLE_NORMAL = "normal"

    // Deprecated alias compatibility
    const val ADMIN_ROLE = ROLE_MASTER
    const val FIELD_SURVEYOR_ROLE = ROLE_NORMAL

    // Master password — typing this in Settings switches any session to Master
    const val MASTER_PASSWORD = "SCSCD@2026"

    const val FIREBASE_PROJECT_ID_HINT = "scscdata-51bb8"
    const val FIREBASE_PROJECT_NUMBER_HINT = "127415575414"

    /**
     * 25 Normal user accounts: ID → password
     * IDs: SC0101–SC0125  |  Passwords: pass01–pass25
     */
    val NORMAL_USER_CREDENTIALS: Map<String, String> = mapOf(
        "SC0101" to "pass01",
        "SC0102" to "pass02",
        "SC0103" to "pass03",
        "SC0104" to "pass04",
        "SC0105" to "pass05",
        "SC0106" to "pass06",
        "SC0107" to "pass07",
        "SC0108" to "pass08",
        "SC0109" to "pass09",
        "SC0110" to "pass10",
        "SC0111" to "pass11",
        "SC0112" to "pass12",
        "SC0113" to "pass13",
        "SC0114" to "pass14",
        "SC0115" to "pass15",
        "SC0116" to "pass16",
        "SC0117" to "pass17",
        "SC0118" to "pass18",
        "SC0119" to "pass19",
        "SC0120" to "pass20",
        "SC0121" to "pass21",
        "SC0122" to "pass22",
        "SC0123" to "pass23",
        "SC0124" to "pass24",
        "SC0125" to "pass25"
    )

    /** Validate ID+password and return the role, or null if invalid credentials */
    fun loginWithIdPassword(id: String, password: String): String? {
        val cleanId = id.trim().uppercase()
        val cleanPass = password.trim()

        // Master login via any ID if password matches master password
        if (cleanPass == MASTER_PASSWORD) return ROLE_MASTER

        // Normal user: check the 25 accounts
        val expectedPass = NORMAL_USER_CREDENTIALS[cleanId]
        if (expectedPass != null && expectedPass == cleanPass) return ROLE_NORMAL

        return null // invalid credentials
    }

    // Legacy email-based role — kept for compatibility
    fun roleForEmail(email: String): String {
        val cleanEmail = email.trim().lowercase()
        return when {
            cleanEmail == ADMIN_EMAIL || cleanEmail == "scscdata@gmail.com" || cleanEmail == "scpdata01@gmail.com" || cleanEmail == "scscpdata@gmail.com" -> ROLE_MASTER
            cleanEmail.contains("qa") || cleanEmail == "scscqa@gmail.com" -> ROLE_QA
            else -> ROLE_NORMAL
        }
    }
}
