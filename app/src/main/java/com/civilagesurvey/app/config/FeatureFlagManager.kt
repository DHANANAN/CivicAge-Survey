package com.civilagesurvey.app.config

import android.content.Context
import android.content.SharedPreferences

class FeatureFlagManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cas_feature_flags", Context.MODE_PRIVATE)

    var isQaDashboardEnabled: Boolean
        get() = prefs.getBoolean("flag_qa_dashboard", true)
        set(value) = prefs.edit().putBoolean("flag_qa_dashboard", value).apply()

    var isAutoDrivePushEnabled: Boolean
        get() = prefs.getBoolean("flag_auto_drive_push", true)
        set(value) = prefs.edit().putBoolean("flag_auto_drive_push", value).apply()

    var isStrictGeoEnabled: Boolean
        get() = prefs.getBoolean("flag_strict_geo", true)
        set(value) = prefs.edit().putBoolean("flag_strict_geo", value).apply()

    var isSwipeGesturesEnabled: Boolean
        get() = prefs.getBoolean("flag_swipe_gestures", true)
        set(value) = prefs.edit().putBoolean("flag_swipe_gestures", value).apply()
}
