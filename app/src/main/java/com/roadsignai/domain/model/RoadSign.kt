package com.roadsignai.domain.model

import android.graphics.Rect
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a detected road sign with its category, position, and confidence.
 */
@Parcelize
data class RoadSign(
    val id: String,
    val category: SignCategory,
    val label: String,
    val confidence: Float,
    val boundingBox: Rect,
    val speedLimitValue: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null
) : Parcelable

/**
 * Categories of road signs according to Russian traffic rules (ПДД РФ).
 * The priority value determines TTS urgency (higher = more critical).
 */
enum class SignCategory(
    val displayName: String,
    val priority: Int,   // 1 = critical, 2 = warning, 3 = informational
    val isProhibitory: Boolean = false
) {
    SPEED_LIMIT("Ограничение скорости", 2),
    STOP("Стоп", 1),
    YIELD("Уступи дорогу", 3),
    MAIN_ROAD("Главная дорога", 3),
    PEDESTRIAN_CROSSING("Пешеходный переход", 2),
    NO_ENTRY("Въезд запрещён", 1, isProhibitory = true),
    NO_TRAFFIC("Движение запрещено", 2, isProhibitory = true),
    NO_OVERTAKING("Обгон запрещён", 2, isProhibitory = true),
    NO_PARKING("Стоянка запрещена", 1, isProhibitory = true),
    NO_STOPPING("Остановка запрещена", 1, isProhibitory = true),
    END_OF_RESTRICTIONS("Конец зоны всех ограничений", 3),
    CROSSWALK("Пешеходный переход", 2),
    UNKNOWN("Неизвестный знак", 3);

    companion object {
        /**
         * Returns the TTS text for a given sign category and optional speed value.
         */
        fun getTtsText(category: SignCategory, speedValue: Int? = null): String {
            return when (category) {
                SPEED_LIMIT -> "Знак ограничения скорости $speedValue километров в час"
                STOP -> "Знак стоп"
                YIELD -> "Знак уступи дорогу"
                MAIN_ROAD -> "Знак главная дорога"
                PEDESTRIAN_CROSSING -> "Внимание, пешеходный переход"
                NO_ENTRY -> "Знак въезд запрещён"
                NO_TRAFFIC -> "Знак движение запрещено"
                NO_OVERTAKING -> "Знак обгон запрещён"
                NO_PARKING -> "Знак стоянка запрещена"
                NO_STOPPING -> "Знак остановка запрещена. Зона действия: до следующего перекрестка"
                END_OF_RESTRICTIONS -> "Конец зоны всех ограничений"
                CROSSWALK -> "Внимание, пешеходный переход"
                UNKNOWN -> "Обнаружен неизвестный дорожный знак"
            }
        }
    }
}
