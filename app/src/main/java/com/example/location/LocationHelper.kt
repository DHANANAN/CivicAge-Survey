package com.dhananan.civicage.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onLocationReceived: (LocationData?) -> Unit) {
        val cancellationTokenSource = CancellationTokenSource()
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    onLocationReceived(
                        LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = location.time
                        )
                    )
                } else {
                    // Fallback to last known location
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            onLocationReceived(
                                LocationData(
                                    latitude = lastLoc.latitude,
                                    longitude = lastLoc.longitude,
                                    accuracy = lastLoc.accuracy,
                                    timestamp = lastLoc.time
                                )
                            )
                        } else {
                            onLocationReceived(null)
                        }
                    }.addOnFailureListener {
                        onLocationReceived(null)
                    }
                }
            }.addOnFailureListener {
                onLocationReceived(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onLocationReceived(null)
        }
    }
}
