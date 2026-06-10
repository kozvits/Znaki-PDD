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
        return TextToSpeech(context, TextToSpeech.OnInitListener { _ -> }).apply {
            setLanguage(Locale("ru", "RU"))
            setSpeechRate(0.9f)
        }
    }
}
