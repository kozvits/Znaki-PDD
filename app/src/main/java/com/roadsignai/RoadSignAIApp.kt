package com.roadsignai

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RoadSignAIApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
