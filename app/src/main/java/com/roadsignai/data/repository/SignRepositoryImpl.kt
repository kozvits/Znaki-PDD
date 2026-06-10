package com.roadsignai.data.repository

import android.graphics.Rect
import com.roadsignai.data.local.db.SignDao
import com.roadsignai.data.local.db.SignEntity
import com.roadsignai.data.local.preferences.SettingsDataStore
import com.roadsignai.domain.model.RoadSign
import com.roadsignai.domain.model.SignCategory
import com.roadsignai.domain.model.SignZone
import com.roadsignai.domain.model.VehicleState
import com.roadsignai.domain.repository.SignRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignRepositoryImpl @Inject constructor(
    private val signDao: SignDao,
    private val settingsDataStore: SettingsDataStore
) : SignRepository {

    private val zonesFlow = MutableStateFlow<List<SignZone>>(emptyList())
    private val vehicleStateFlow = MutableStateFlow(VehicleState())

    override fun observeSigns(): Flow<List<RoadSign>> {
        return signDao.observeRecentSigns(50).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveSign(sign: RoadSign) {
        if (!settingsDataStore.settingsFlow.first().saveLogs) return
        signDao.insertSign(sign.toEntity())
    }

    override suspend fun getRecentSigns(limit: Int): List<RoadSign> {
        return signDao.getRecentSigns(limit).map { it.toDomain() }
    }

    override suspend fun saveZone(zone: SignZone) {
        val current = zonesFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == zone.id }
        if (existingIndex >= 0) {
            current[existingIndex] = zone
        } else {
            current.add(zone)
        }
        zonesFlow.value = current
    }

    override fun observeActiveZones(): Flow<List<SignZone>> {
        return zonesFlow.map { zones -> zones.filter { it.isActive } }
    }

    override suspend fun getActiveZones(): List<SignZone> {
        return zonesFlow.value.filter { it.isActive }
    }

    override suspend fun deactivateZone(zoneId: String) {
        val current = zonesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == zoneId }
        if (index >= 0) {
            current[index] = current[index].copy(isActive = false)
            zonesFlow.value = current
        }
    }

    override suspend fun saveVehicleState(state: VehicleState) {
        vehicleStateFlow.value = state
    }

    override fun observeVehicleState(): Flow<VehicleState> {
        return vehicleStateFlow
    }

    override suspend fun clearAll() {
        signDao.clearAll()
        zonesFlow.value = emptyList()
        vehicleStateFlow.value = VehicleState()
    }

    private fun RoadSign.toEntity() = SignEntity(
        id = id,
        category = category.name,
        label = label,
        confidence = confidence,
        speedLimitValue = speedLimitValue,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        boundingBoxLeft = boundingBox.left,
        boundingBoxTop = boundingBox.top,
        boundingBoxRight = boundingBox.right,
        boundingBoxBottom = boundingBox.bottom,
        signCode = signCode,
        signName = signName,
        signDescription = signDescription,
        visualGroup = visualGroup
    )

    private fun SignEntity.toDomain() = RoadSign(
        id = id,
        category = try { SignCategory.valueOf(category) } catch (e: Exception) { SignCategory.UNKNOWN },
        label = label,
        confidence = confidence,
        boundingBox = Rect(boundingBoxLeft, boundingBoxTop, boundingBoxRight, boundingBoxBottom),
        speedLimitValue = speedLimitValue,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        signCode = signCode,
        signName = signName,
        signDescription = signDescription,
        visualGroup = visualGroup
    )
}
