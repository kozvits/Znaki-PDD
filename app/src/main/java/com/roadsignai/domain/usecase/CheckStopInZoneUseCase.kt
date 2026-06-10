package com.roadsignai.domain.usecase

import android.location.Location
import com.roadsignai.domain.model.SignZone
import com.roadsignai.domain.model.VehicleState
import com.roadsignai.domain.repository.SignRepository
import javax.inject.Inject

/**
 * Use case for detecting when the vehicle stops within a prohibited zone.
 *
 * Logic per ПДД РФ:
 * - Speed < 3 km/h for > 5 seconds → stopped
 * - If stopped within zone 3.27 (Остановка запрещена) or 3.28–3.30 → violation
 * - If approaching zone (distance < approach threshold) → warning
 */
class CheckStopInZoneUseCase @Inject constructor(
    private val repository: SignRepository
) {
    private companion object {
        const val STOP_SPEED_THRESHOLD = 3f  // km/h
        const val STOP_DURATION_MS = 5000L    // 5 seconds
        const val APPROACH_DISTANCE_M = 30f   // 30 meters
    }

    /**
     * Check if the vehicle is stopped in a prohibited zone.
     */
    suspend operator fun invoke(
        vehicleState: VehicleState,
        activeZones: List<SignZone>
    ): StopCheckResult {
        if (vehicleState.location == null || activeZones.isEmpty()) {
            return StopCheckResult(
                isStoppedInZone = false,
                warningMessage = null,
                isApproachingZone = false
            )
        }

        // Check if within any zone
        val currentZone = activeZones.firstOrNull { zone ->
            zone.isActive && isWithinZone(vehicleState.location, zone)
        }

        if (currentZone == null) {
            // Check approaching zones
            val approachingZone = activeZones.firstOrNull { zone ->
                zone.isActive && isApproaching(vehicleState.location, zone)
            }
            return if (approachingZone != null) {
                StopCheckResult(
                    isStoppedInZone = false,
                    warningMessage = "Приближается зона действия знака. Остановка запрещена",
                    isApproachingZone = true,
                    approachingZone = approachingZone
                )
            } else {
                StopCheckResult(isStoppedInZone = false, warningMessage = null, isApproachingZone = false)
            }
        }

        // Within a zone — check if stopped
        val isStopped = VehicleState.isVehicleStopped(
            vehicleState.speedKmh,
            vehicleState.stoppedSinceTimestamp
        )

        return if (isStopped) {
            StopCheckResult(
                isStoppedInZone = true,
                warningMessage = "⚠️ ОСТАНОВКА В ЗОНЕ ЗАПРЕТА!\n" +
                    "Зона: ${currentZone.signCategory.displayName}\n" +
                    "Немедленно покиньте зону!",
                isApproachingZone = false,
                currentZone = currentZone,
                isCritical = true
            )
        } else {
            StopCheckResult(
                isStoppedInZone = false,
                warningMessage = null,
                isApproachingZone = false,
                currentZone = currentZone
            )
        }
    }

    private fun isWithinZone(location: Location, zone: SignZone): Boolean {
        val distance = location.distanceTo(zone.startLocation)
        return distance <= zone.defaultRadiusMeters
    }

    private fun isApproaching(location: Location, zone: SignZone): Boolean {
        val distance = location.distanceTo(zone.startLocation)
        return distance in 0f..APPROACH_DISTANCE_M
    }

    data class StopCheckResult(
        val isStoppedInZone: Boolean,
        val warningMessage: String?,
        val isApproachingZone: Boolean,
        val currentZone: SignZone? = null,
        val approachingZone: SignZone? = null,
        val isCritical: Boolean = false
    )
}
