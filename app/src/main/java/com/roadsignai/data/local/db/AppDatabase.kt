package com.roadsignai.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SignEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun signDao(): SignDao
}
