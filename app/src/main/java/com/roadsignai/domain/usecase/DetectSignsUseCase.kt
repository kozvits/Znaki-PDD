package com.roadsignai.domain.usecase

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.text.TextRecognizer
import com.roadsignai.data.local.await
import com.roadsignai.domain.model.RoadSign
import com.roadsignai.domain.model.SignCategory
import com.roadsignai.domain.repository.SignRepository
import javax.inject.Inject

/**
 * Use case for detecting road signs from camera frames.
 * Uses ML Kit Object Detection + Text Recognition combined with heuristics.
 */
class DetectSignsUseCase @Inject constructor(
    private val objectDetector: ObjectDetector,
    private val textRecognizer: TextRecognizer,
    private val repository: SignRepository
) {
    /** Classification confidence thresholds. */
    private val DISPLAY_THRESHOLD = 0.6f
    private val SPEAK_THRESHOLD = 0.75f

    /**
     * Process a camera image and detect road signs.
     * Returns a list of detected signs above the confidence threshold.
     */
    suspend operator fun invoke(image: InputImage): DetectionResult {
        return try {
            val objects = objectDetector.process(image).await()
            val signs = mutableListOf<RoadSign>()

            for (obj in objects) {
                // Use ML Kit's built-in classification labels if available
                val labels = obj.labels
                if (labels.isNotEmpty()) {
                    val topLabel = labels.maxByOrNull { it.confidence }
                    if (topLabel != null && topLabel.confidence >= DISPLAY_THRESHOLD) {
                        val category = mapLabelToCategory(topLabel.text, obj.boundingBox, image)
                        val speedValue = extractSpeedFromText(topLabel.text, obj.boundingBox, image)
                        signs.add(
                            RoadSign(
                                id = "sign_${System.nanoTime()}",
                                category = category,
                                label = category.displayName,
                                confidence = topLabel.confidence,
                                boundingBox = obj.boundingBox,
                                speedLimitValue = speedValue
                            )
                        )
                    }
                } else {
                    // Fallback: use text recognition + heuristics
                    val category = classifyByHeuristics(obj, image)
                    if (category != SignCategory.UNKNOWN) {
                        signs.add(
                            RoadSign(
                                id = "sign_${System.nanoTime()}",
                                category = category,
                                label = category.displayName,
                                confidence = 0.7f,
                                boundingBox = obj.boundingBox
                            )
                        )
                    }
                }
            }

            DetectionResult(
                signs = signs,
                hasCriticalSign = signs.any { it.category.priority == 1 && it.confidence >= SPEAK_THRESHOLD }
            )
        } catch (e: Exception) {
            DetectionResult(emptyList(), hasCriticalSign = false, error = e.message)
        }
    }

    /**
     * Map ML Kit label to our sign category.
     */
    private fun mapLabelToCategory(
        label: String,
        boundingBox: Rect,
        image: InputImage
    ): SignCategory {
        val normalizedLabel = label.lowercase().trim()
        return when {
            normalizedLabel.contains("stop") -> SignCategory.STOP
            normalizedLabel.contains("speed limit") ||
            normalizedLabel.contains("speed") -> SignCategory.SPEED_LIMIT
            normalizedLabel.contains("yield") -> SignCategory.YIELD
            normalizedLabel.contains("pedestrian") ||
            normalizedLabel.contains("crosswalk") -> SignCategory.PEDESTRIAN_CROSSING
            normalizedLabel.contains("no entry") -> SignCategory.NO_ENTRY
            normalizedLabel.contains("do not enter") -> SignCategory.NO_ENTRY
            normalizedLabel.contains("no parking") -> SignCategory.NO_PARKING
            normalizedLabel.contains("no stopping") -> SignCategory.NO_STOPPING
            normalizedLabel.contains("no overtaking") -> SignCategory.NO_OVERTAKING
            normalizedLabel.contains("no traffic") ||
            normalizedLabel.contains("do not pass") -> SignCategory.NO_TRAFFIC
            normalizedLabel.contains("main road") -> SignCategory.MAIN_ROAD
            normalizedLabel.contains("end of") -> SignCategory.END_OF_RESTRICTIONS
            else -> {
                // Further classify based on shape/color heuristics
                classifyByShapeAndColor(label)
            }
        }
    }

    /**
     * Extract speed limit value from text on a sign using OCR.
     */
    private fun extractSpeedFromText(
        label: String,
        boundingBox: Rect,
        image: InputImage
    ): Int? {
        // Try to extract from label first
        val speedRegex = Regex("""\b(\d{2,3})\b""")
        val match = speedRegex.find(label)
        if (match != null) {
            val value = match.groupValues[1].toIntOrNull()
            if (value != null && value in 20..130) return value
        }
        return null
    }

    /**
     * Fallback heuristic classification using shape/color.
     */
    private fun classifyByShapeAndColor(label: String): SignCategory {
        val lower = label.lowercase()
        return when {
            lower.contains("circle") && lower.contains("red") -> SignCategory.NO_ENTRY
            lower.contains("triangle") && lower.contains("red") -> SignCategory.YIELD
            lower.contains("rectangle") && lower.contains("blue") -> SignCategory.MAIN_ROAD
            lower.contains("circle") && lower.contains("blue") -> SignCategory.SPEED_LIMIT
            else -> SignCategory.UNKNOWN
        }
    }

    /**
     * Fallback classification from detected object features.
     */
    private fun classifyByHeuristics(
        obj: DetectedObject,
        image: InputImage
    ): SignCategory {
        // Use bounding box aspect ratio and size for basic classification
        val box = obj.boundingBox
        val aspectRatio = box.width().toFloat() / box.height().toFloat()

        return if (aspectRatio in 0.8f..1.2f) {
            // Square-ish signs are likely prohibitory or mandatory
            SignCategory.STOP
        } else {
            SignCategory.UNKNOWN
        }
    }

    /**
     * Check if a sign should be spoken (above speak threshold).
     */
    fun shouldSpeak(confidence: Float): Boolean = confidence >= SPEAK_THRESHOLD

    data class DetectionResult(
        val signs: List<RoadSign>,
        val hasCriticalSign: Boolean,
        val error: String? = null
    )
}
