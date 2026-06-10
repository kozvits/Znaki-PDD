package com.roadsignai.domain.repository

import com.roadsignai.domain.model.RoadSign
import com.roadsignai.domain.model.SignZone
import com.roadsignai.domain.model.VehicleState
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for sign detection data.
 */
interface SignRepository {

    /** Observe saved road signs. */
    fun observeSigns(): Flow<List<RoadSign>>

    /** Save detected sign to local storage. */
    suspend fun saveSign(sign: RoadSign)

    /** Get recent unique signs (no consecutive duplicates). */
    suspend fun getRecentSigns(limit: Int = 20): List<RoadSign>

    /** Save zone to local storage. */
    suspend fun saveZone(zone: SignZone)

    /** Observe active zones. */
    fun observeActiveZones(): Flow<List<SignZone>>

    /** Get active zones. */
    suspend fun getActiveZones(): List<SignZone>

    /** Deactivate a zone when its end condition is met. */
    suspend fun deactivateZone(zoneId: String)

    /** Save current vehicle state. */
    suspend fun saveVehicleState(state: VehicleState)

    /** Observe vehicle state. */
    fun observeVehicleState(): Flow<VehicleState>

    /** Clear all data. */
    suspend fun clearAll()
}
