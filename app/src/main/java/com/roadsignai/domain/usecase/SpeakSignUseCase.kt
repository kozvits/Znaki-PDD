package com.roadsignai.domain.usecase

import android.speech.tts.TextToSpeech
import com.roadsignai.domain.model.RoadSign
import com.roadsignai.domain.model.SignCategory
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case for Text-To-Speech announcements of detected signs.
 *
 * Priority levels:
 * 1. Critical (Stop, No Entry, No Stopping) — QUEUE_FLUSH
 * 2. Warning (Speed Limit, Pedestrian Crossing) — QUEUE_FLUSH for critical, QUEUE_ADD for warning
 * 3. Informational (Main Road, Yield) — QUEUE_ADD
 */
class SpeakSignUseCase @Inject constructor(
    private val tts: TextToSpeech
) {
    private var lastSpeakTimestamp = 0L
    private var minSpeakInterval = 2000L // 2 seconds default

    /**
     * Speak the sign announcement.
     */
    suspend operator fun invoke(
        sign: RoadSign,
        minIntervalMs: Long = 2000L
    ) {
        val now = System.currentTimeMillis()
        if (now - lastSpeakTimestamp < minIntervalMs) return

        withContext(Dispatchers.Main) {
            val text = SignCategory.getTtsText(sign.category, sign.speedLimitValue)

            val queueMode = when {
                sign.category.priority == 1 -> TextToSpeech.QUEUE_FLUSH
                sign.category.priority == 2 -> TextToSpeech.QUEUE_ADD
                else -> TextToSpeech.QUEUE_ADD
            }

            tts.speak(text, queueMode, null, "sign_${sign.id}")
            lastSpeakTimestamp = System.currentTimeMillis()
        }
    }

    /**
     * Speak a custom warning message (for stop-in-zone alerts).
     */
    suspend fun speakWarning(message: String, flush: Boolean = true) {
        withContext(Dispatchers.Main) {
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(message, queueMode, null, "warning_${System.nanoTime()}")
        }
    }

    /**
     * Speak that a zone has ended.
     */
    suspend fun speakZoneEnded() {
        speakWarning("Предположительно зона действия знака закончена", flush = false)
    }

    /**
     * Stop any current speech.
     */
    fun stop() {
        tts.stop()
    }

    /**
     * Shutdown TTS engine.
     */
    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    /**
     * Update min interval between announcements.
     */
    fun setMinInterval(intervalMs: Long) {
        minSpeakInterval = intervalMs
    }
}
