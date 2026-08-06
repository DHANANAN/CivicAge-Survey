package com.civilagesurvey.app.telemetry

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

object TelemetryManager {
    private const val TAG = "TelemetryManager"

    fun logGeoEvent(context: Context, status: String, accuracy: Float, tier: String) {
        val event = mapOf(
            "eventType" to "GEO_LOCATION_CAPTURE",
            "status" to status, // "ok", "denied", "unavailable"
            "accuracy" to accuracy,
            "tier" to tier,
            "timestamp" to Date().time
        )
        Log.i(TAG, "Geo Event: $event")
        recordToFirestore(event)
    }

    fun logQaAction(instrumentId: String, action: String, performedBy: String) {
        val event = mapOf(
            "eventType" to "QA_ACTION",
            "instrumentId" to instrumentId,
            "action" to action, // "approve", "reject", "request_qa"
            "performedBy" to performedBy,
            "timestamp" to Date().time
        )
        Log.i(TAG, "QA Action: $event")
        recordToFirestore(event)
    }

    fun logDrivePushResult(instrumentId: String, status: String, drivePath: String?, error: String?) {
        val event = mapOf(
            "eventType" to "DRIVE_PUSH_RESULT",
            "instrumentId" to instrumentId,
            "status" to status, // "success", "failed"
            "drivePath" to drivePath,
            "error" to error,
            "timestamp" to Date().time
        )
        Log.i(TAG, "Drive Push Result: $event")
        recordToFirestore(event)
    }

    private fun recordToFirestore(data: Map<String, Any?>) {
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("telemetryLogs")
                .add(data)
        }
    }
}
