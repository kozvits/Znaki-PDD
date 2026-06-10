package com.roadsignai.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detected_signs")
data class SignEntity(
    @PrimaryKey
    val id: String,
    val category: String,
    val label: String,
    val confidence: Float,
    val speedLimitValue: Int? = null,
    val timestamp: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val boundingBoxLeft: Int = 0,
    val boundingBoxTop: Int = 0,
    val boundingBoxRight: Int = 0,
    val boundingBoxBottom: Int = 0
)
