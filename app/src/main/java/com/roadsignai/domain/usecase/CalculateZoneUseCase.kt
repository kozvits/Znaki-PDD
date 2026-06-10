package com.roadsignai.domain.usecase

import android.location.Location
import com.roadsignai.domain.model.RoadSign
import com.roadsignai.domain.model.SignCategory
import com.roadsignai.domain.model.SignZone
import com.roadsignai.domain.model.ZoneType
import com.roadsignai.domain.repository.SignRepository
import javax.inject.Inject

/**
 * Use case for calculating zones of action for detected signs per ПДД РФ.
 *
 * Zone termination conditions per ПДД РФ:
 * - Next intersection (detected via GPS proximity to known intersections)
 * - End-of-zone sign (3.25, 3.21, 3.31)
 * - Another prohibitory sign replacing the current one
 * - Default max distance as fallback
 */
class CalculateZoneUseCase @Inject constructor(
    private val repository: SignRepository
) {
    private val activeZones = mutableMapOf<String, SignZone>()

    /**
     * Calculate or update zone of action for a detected prohibitory sign.
     */
    suspend operator fun invoke(
        sign: RoadSign,
        currentLocation: Location,
        defaultSpeedZoneDistance: Float = 500f,
        defaultParkingZoneDistance: Float = 50f
    ): ZoneCalculationResult {
        if (!sign.category.isProhibitory) {
            return ZoneCalculationResult(
                zone = null,
                isWithinZone = false,
                approachingWarning = null,
                zoneEnded = false
            )
        }

        val zoneType = sign.category.toZoneType()
        val defaultRadius = when (zoneType) {
            ZoneType.SPEED_LIMIT, ZoneType.NO_OVERTAKING, ZoneType.NO_TRAFFIC -> defaultSpeedZoneDistance
            ZoneType.NO_STOPPING, ZoneType.NO_PARKING -> defaultParkingZoneDistance
            ZoneType.PROHIBITORY -> 100f
        }

        // Check if we just entered or are already in a zone
        val existingZone = activeZones.values.find { zone ->
            zone.isActive && isWithinRadius(currentLocation, zone.startLocation, zone.defaultRadiusMeters)
        }

        val result = if (existingZone != null) {
            checkZoneConditions(sign, existingZone, currentLocation)
        } else {
            // Create new zone
            val zone = SignZone(
                id = "zone_${System.nanoTime()}",
                signId = sign.id,
                signCategory = sign.category,
                startLocation = sign.latitude?.let {
                    Location("").apply {
                        latitude = it
                        longitude = sign.longitude ?: 0.0
                    }
                } ?: currentLocation,
                zoneType = zoneType,
                defaultRadiusMeters = defaultRadius,
                isActive = true
            )
            activeZones[zone.id] = zone
            repository.saveZone(zone)

            ZoneCalculationResult(
                zone = zone,
                isWithinZone = true,
                approachingWarning = null,
                zoneEnded = false
            )
        }

        return result
    }

    /**
     * Get all currently active zones.
     */
    fun getActiveZones(): List<SignZone> = activeZones.values.filter { it.isActive }

    /**
     * Check if a location is within any active zone.
     */
    fun isInAnyZone(location: Location): SignZone? {
        return activeZones.values.firstOrNull { zone ->
            zone.isActive && isWithinRadius(location, zone.startLocation, zone.defaultRadiusMeters)
        }
    }

    /**
     * Check approaching distance to any zone.
     */
    fun getApproachingZone(location: Location): SignZone? {
        val zones = activeZones.values.filter { it.isActive }
        val approachingZones = zones.flatMap { zone ->
            val distance = calculateDistance(location, zone.startLocation)
            val approachDist = zone.zoneType.approachWarningDistance()
            if (distance in 0f..approachDist) listOf(zone to distance)
            else emptyList()
        }
        return approachingZones.minByOrNull { it.second }?.first
    }

    /**
     * Check if a location is within radius of a reference point.
     */
    private fun isWithinRadius(location: Location, reference: Location, radiusMeters: Float): Boolean {
        return calculateDistance(location, reference) <= radiusMeters
    }

    /**
     * Calculate distance between two locations.
     */
    private fun calculateDistance(loc1: Location, loc2: Location): Float {
        return loc1.distanceTo(loc2)
    }

    /**
     * Check zone termination conditions.
     */
    private suspend fun checkZoneConditions(
        newSign: RoadSign,
        existingZone: SignZone,
        currentLocation: Location
    ): ZoneCalculationResult {
        return when {
            // New prohibitory sign replaces old zone
            newSign.category.isProhibitory -> {
                activeZones[existingZone.id] = existingZone.copy(isActive = false)
                repository.deactivateZone(existingZone.id)

                // Create new zone
                val newZone = SignZone(
                    id = "zone_${System.nanoTime()}",
                    signId = newSign.id,
                    signCategory = newSign.category,
                    startLocation = currentLocation,
                    zoneType = newSign.category.toZoneType(),
                    defaultRadiusMeters = existingZone.defaultRadiusMeters,
                    isActive = true
                )
                activeZones[newZone.id] = newZone
                repository.saveZone(newZone)

                ZoneCalculationResult(
                    zone = newZone,
                    isWithinZone = true,
                    approachingWarning = null,
                    zoneEnded = false
                )
            }
            // End-of-restrictions sign
            newSign.category == SignCategory.END_OF_RESTRICTIONS -> {
                activeZones[existingZone.id] = existingZone.copy(isActive = false)
                repository.deactivateZone(existingZone.id)

                ZoneCalculationResult(
                    zone = null,
                    isWithinZone = false,
                    approachingWarning = null,
                    zoneEnded = true
                )
            }
            // Still within zone
            isWithinRadius(currentLocation, existingZone.startLocation, existingZone.defaultRadiusMeters) -> {
                ZoneCalculationResult(
                    zone = existingZone,
                    isWithinZone = true,
                    approachingWarning = null,
                    zoneEnded = false
                )
            }
            // Exceeded zone distance
            else -> {
                activeZones[existingZone.id] = existingZone.copy(isActive = false)
                repository.deactivateZone(existingZone.id)

                ZoneCalculationResult(
                    zone = null,
                    isWithinZone = false,
                    approachingWarning = "Предположительно зона действия знака закончена",
                    zoneEnded = true
                )
            }
        }
    }

    data class ZoneCalculationResult(
        val zone: SignZone?,
        val isWithinZone: Boolean,
        val approachingWarning: String?,
        val zoneEnded: Boolean
    )
}

/**
 * Map SignCategory to ZoneType.
 */
private fun SignCategory.toZoneType(): ZoneType = when (this) {
    SignCategory.SPEED_LIMIT -> ZoneType.SPEED_LIMIT
    SignCategory.NO_STOPPING -> ZoneType.NO_STOPPING
    SignCategory.NO_PARKING -> ZoneType.NO_PARKING
    SignCategory.NO_OVERTAKING -> ZoneType.NO_OVERTAKING
    SignCategory.NO_TRAFFIC -> ZoneType.NO_TRAFFIC
    SignCategory.NO_ENTRY -> ZoneType.PROHIBITORY
    else -> ZoneType.PROHIBITORY
}
