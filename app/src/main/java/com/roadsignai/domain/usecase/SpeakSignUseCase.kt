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
 *
 * Now uses the Belarusian sign database for richer announcements:
 * "Знак 3.24.1: Ограничение максимальной скорости, 60 километров в час"
 */
class SpeakSignUseCase @Inject constructor(
    private val tts: TextToSpeech
) {
    private var lastSpeakTimestamp = 0L
    private var minSpeakInterval = 2000L // 2 seconds default

    /**
     * Speak the sign announcement.
     * Uses signName from the database when available for richer announcements.
     */
    suspend operator fun invoke(
        sign: RoadSign,
        minIntervalMs: Long = 2000L
    ) {
        val now = System.currentTimeMillis()
        if (now - lastSpeakTimestamp < minIntervalMs) return

        withContext(Dispatchers.Main) {
            val text = if (sign.signName != null) {
                // Use the database name for a detailed announcement
                buildRichAnnouncement(sign)
            } else {
                SignCategory.getTtsText(sign.category, sign.speedLimitValue)
            }

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
     * Build a rich announcement using the sign database info.
     */
    private fun buildRichAnnouncement(sign: RoadSign): String {
        val signName = sign.signName ?: return SignCategory.getTtsText(sign.category, sign.speedLimitValue)
        val code = sign.signCode?.let { ". Код знака $it" } ?: ""

        return when (sign.category) {
            SignCategory.SPEED_LIMIT -> {
                if (sign.speedLimitValue != null)
                    "$signName$code: ${sign.speedLimitValue} километров в час"
                else
                    "$signName$code"
            }
            SignCategory.RECOMMENDED_SPEED -> {
                if (sign.speedLimitValue != null)
                    "$signName$code: ${sign.speedLimitValue} километров в час"
                else
                    "$signName$code"
            }
            SignCategory.STOP -> "Знак стоп. $signName$code"
            SignCategory.RAILROAD_CROSSING -> "Внимание! $signName$code"
            SignCategory.ROAD_WORKS -> "Внимание! $signName$code"
            SignCategory.ICE_WARNING -> "Внимание! $signName$code"
            SignCategory.DANGEROUS_AREA -> "Внимание! $signName$code"
            SignCategory.CHILDREN_WARNING -> "Осторожно! $signName$code"
            SignCategory.PEDESTRIAN_CROSSING -> "Внимание! $signName$code"
            SignCategory.NO_ENTRY -> "Запрещающий знак: $signName$code"
            SignCategory.NO_STOPPING -> "$signName$code. Зона действия до следующего перекрёстка"
            SignCategory.PEDESTRIAN_UNDERPASS -> "$signName$code"
            SignCategory.MOTORWAY -> "$signName$code"
            SignCategory.GAS_STATION -> "$signName$code"
            SignCategory.POLICE -> "$signName$code"
            SignCategory.HOSPITAL -> "$signName$code"
            SignCategory.FIRST_AID -> "$signName$code"
            SignCategory.RESTAURANT -> "$signName$code"
            else -> "$signName$code"
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
