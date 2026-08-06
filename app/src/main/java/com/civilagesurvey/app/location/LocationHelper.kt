package com.civilagesurvey.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.civilagesurvey.app.telemetry.TelemetryManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

data class LocationValidationResult(
    val location: Location? = null,
    val status: String = "unavailable", // "ok", "denied", "unavailable"
    val accuracy: Float = 0f,
    val timestamp: Long = 0L,
    val isValid: Boolean = false,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        tier: String = "strict",
        onResult: (LocationValidationResult) -> Unit
    ) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            TelemetryManager.logGeoEvent(context, "denied", 0f, tier)
            onResult(
                LocationValidationResult(
                    status = "denied",
                    errorCode = "ERR_GEO_REQUIRED",
                    errorMessage = "Location permission has been denied. High-accuracy GPS is required before starting."
                )
            )
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location == null) {
                    TelemetryManager.logGeoEvent(context, "unavailable", 0f, tier)
                    onResult(
                        LocationValidationResult(
                            status = "unavailable",
                            errorCode = "ERR_GEO_REQUIRED",
                            errorMessage = "Unable to obtain GPS fix. Please ensure location services are enabled."
                        )
                    )
                    return@addOnSuccessListener
                }

                val now = System.currentTimeMillis()
                val ageMs = Math.abs(now - location.time)
                val isFresh = ageMs <= 30000 // 30 seconds
                val isAccurate = location.accuracy <= 100.0f

                val isValid = if (tier == "strict") (isFresh && isAccurate) else true

                val errorCode = when {
                    !isAccurate -> "ERR_GEO_ACCURACY"
                    !isFresh -> "ERR_GEO_STALE"
                    else -> null
                }

                val errorMessage = when {
                    !isAccurate -> "Location accuracy (${location.accuracy.toInt()}m) exceeds maximum 100m threshold."
                    !isFresh -> "Location fix is stale (${ageMs / 1000}s old). Maximum allowed freshness is 30s."
                    else -> null
                }

                TelemetryManager.logGeoEvent(context, "ok", location.accuracy, tier)
                onResult(
                    LocationValidationResult(
                        location = location,
                        status = "ok",
                        accuracy = location.accuracy,
                        timestamp = location.time,
                        isValid = isValid,
                        errorCode = errorCode,
                        errorMessage = errorMessage
                    )
                )
            }
            .addOnFailureListener { e ->
                TelemetryManager.logGeoEvent(context, "unavailable", 0f, tier)
                onResult(
                    LocationValidationResult(
                        status = "unavailable",
                        errorCode = "ERR_GEO_REQUIRED",
                        errorMessage = "Location failure: ${e.localizedMessage}"
                    )
                )
            }
    }
}
