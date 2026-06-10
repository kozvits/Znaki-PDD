package com.roadsignai.domain.model

import android.location.Location

/**
 * Represents the current vehicle state derived from GPS and sensors.
 */
data class VehicleState(
    val speedKmh: Float = 0f,
    val location: Location? = null,
    val isStopped: Boolean = false,
    val stoppedSinceTimestamp: Long = 0L,
    val heading: Float = 0f,
    val accuracy: Float = 0f
) {
    companion object {
        private const val STOP_SPEED_THRESHOLD = 3f // km/h
        private const val STOP_DURATION_THRESHOLD = 5000L // 5 seconds

        /**
         * Determine if vehicle is considered stopped based on speed and duration.
         */
        fun isVehicleStopped(speedKmh: Float, stoppedSinceMs: Long): Boolean {
            val currentTime = System.currentTimeMillis()
            return speedKmh < STOP_SPEED_THRESHOLD &&
                (currentTime - stoppedSinceMs) > STOP_DURATION_THRESHOLD
        }
    }
}
