package com.roadsignai.di

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale

@Module
@InstallIn(ViewModelComponent::class)
object TTSModule {

    @Provides
    fun provideTextToSpeech(@ApplicationContext context: Context): TextToSpeech {
        val tts = TextToSpeech(context, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale("ru", "RU"))
                tts.setSpeechRate(0.9f)
            }
        })
        return tts
    }
}
