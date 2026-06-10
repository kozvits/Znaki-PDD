package com.roadsignai.domain.model

import android.location.Location

/**
 * Represents the zone of action for a prohibitory road sign per ПДД РФ.
 */
data class SignZone(
    val id: String,
    val signId: String,
    val signCategory: SignCategory,
    val startLocation: Location,
    val endLocation: Location? = null,
    val zoneType: ZoneType,
    val defaultRadiusMeters: Float = 500f,
    val isActive: Boolean = true
)

enum class ZoneType {
    // 3.24 - Speed limit zone (until next intersection or end sign)
    SPEED_LIMIT,
    // 3.27 - No stopping zone
    NO_STOPPING,
    // 3.28-3.30 - No parking zone (by days)
    NO_PARKING,
    // 3.20 - No overtaking zone
    NO_OVERTAKING,
    // 3.2 - No traffic zone
    NO_TRAFFIC,
    // Generic prohibitory zone
    PROHIBITORY;

    /**
     * Default zone radius in meters as per ПДД РФ defaults.
     * Actual zone ends at the next intersection, end-of-zone sign,
     * or this distance as a fallback.
     */
    fun defaultRadiusMeters(): Float = when (this) {
        SPEED_LIMIT -> 500f
        NO_STOPPING -> 50f
        NO_PARKING -> 50f
        NO_OVERTAKING -> 500f
        NO_TRAFFIC -> 500f
        PROHIBITORY -> 100f
    }

    /**
     * Zone approaches warning distance.
     */
    fun approachWarningDistance(): Float = when (this) {
        SPEED_LIMIT -> 100f
        NO_STOPPING -> 30f
        NO_PARKING -> 30f
        NO_OVERTAKING -> 100f
        NO_TRAFFIC -> 100f
        PROHIBITORY -> 50f
    }
}
