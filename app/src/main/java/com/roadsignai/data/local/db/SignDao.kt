package com.roadsignai.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignDao {

    @Query("SELECT * FROM detected_signs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentSigns(limit: Int = 50): Flow<List<SignEntity>>

    @Query("SELECT * FROM detected_signs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSigns(limit: Int = 50): List<SignEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSign(sign: SignEntity)

    @Query("DELETE FROM detected_signs WHERE timestamp < :cutoffTime")
    suspend fun deleteOldSigns(cutoffTime: Long)

    @Query("DELETE FROM detected_signs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM detected_signs")
    fun observeSignCount(): Flow<Int>
}
