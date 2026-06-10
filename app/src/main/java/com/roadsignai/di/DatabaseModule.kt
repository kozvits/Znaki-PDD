package com.roadsignai.di

import android.content.Context
import androidx.room.Room
import com.roadsignai.data.local.db.AppDatabase
import com.roadsignai.data.local.db.SignDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "roadsignai_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSignDao(database: AppDatabase): SignDao {
        return database.signDao()
    }
}
