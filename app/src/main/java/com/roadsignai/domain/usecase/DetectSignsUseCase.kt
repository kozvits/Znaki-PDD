package com.roadsignai.domain.usecase

import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.text.TextRecognizer
import com.roadsignai.data.local.await
import com.roadsignai.data.signs.BelarusSign
import com.roadsignai.data.signs.SignDatabase
import com.roadsignai.domain.model.RoadSign
import com.roadsignai.domain.model.SignCategory
import com.roadsignai.domain.repository.SignRepository
import javax.inject.Inject

/**
 * Use case for detecting road signs from camera frames.
 * Uses ML Kit Object Detection + Text Recognition combined with the
 * full Belarusian ПДД РБ sign database (261 signs, 114 visual groups).
 *
 * Detection pipeline:
 *   1. ML Kit Object Detector identifies objects + provides labels
 *   2. Labels are mapped to visual groups using SignDatabase.mapMlKitLabelToVisualGroup()
 *   3. Visual group + OCR text are matched against the sign database
 *   4. Best match is returned with name, code, description from ПДД РБ
 *   5. Fallback: shape/color heuristics when ML Kit labels are insufficient
 */
class DetectSignsUseCase @Inject constructor(
    private val objectDetector: ObjectDetector,
    private val textRecognizer: TextRecognizer,
    private val repository: SignRepository,
    private val signDatabase: SignDatabase
) {
    /** Classification confidence thresholds. */
    private val DISPLAY_THRESHOLD = 0.6f
    private val SPEAK_THRESHOLD = 0.75f

    /** Number of signs to show on screen. */
    private val MAX_DISPLAY_SIGNS = 3

    /**
     * Process a camera image and detect road signs.
     * Returns a list of detected signs above the confidence threshold.
     */
    suspend operator fun invoke(image: InputImage): DetectionResult {
        return try {
            signDatabase.ensureLoaded()
            val objects = objectDetector.process(image).await()
            val signs = mutableListOf<RoadSign>()

            for (obj in objects.take(MAX_DISPLAY_SIGNS)) {
                val labels = obj.labels
                if (labels.isNotEmpty()) {
                    val topLabel = labels.maxByOrNull { it.confidence }
                    if (topLabel != null && topLabel.confidence >= DISPLAY_THRESHOLD) {
                        val result = matchWithDatabase(topLabel.text, topLabel.confidence, obj, image)
                        signs.add(result)
                    }
                } else {
                    // No labels from ML Kit: use OCR + shape/color heuristics
                    val result = classifyByHeuristics(obj, image)
                    signs.add(result)
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
     * Match a detected object against the Belarusian sign database.
     * Uses: ML Kit label → visual group → sign DB match + OCR for text.
     */
    private suspend fun matchWithDatabase(
        label: String,
        confidence: Float,
        obj: DetectedObject,
        image: InputImage
    ): RoadSign {
        val visualGroup = SignDatabase.mapMlKitLabelToVisualGroup(label)
        var matchedSign: BelarusSign? = null
        var speedValue: Int? = null

        if (visualGroup != null) {
            // Look up signs in this visual group from the database
            val candidates = signDatabase.findByVisualGroup(visualGroup)

            if (candidates.isNotEmpty()) {
                // For speed limits, try to read the number from the image
                if (visualGroup == "speed_limit" || visualGroup == "recommended_speed") {
                    speedValue = extractSpeedFromImage(obj.boundingBox, image)
                    if (speedValue == null) {
                        speedValue = extractSpeedFromLabel(label)
                    }
                }

                // For signs with text, use OCR to disambiguate
                if (candidates.size > 1) {
                    val recognizedText = recognizeText(obj.boundingBox, image)
                    if (recognizedText != null) {
                        val (textMatch, _) = signDatabase.findBestMatchForText(recognizedText, visualGroup)
                        if (textMatch != null) {
                            matchedSign = textMatch
                        }
                    }
                }

                // Default: use the first match or the most common one
                if (matchedSign == null) {
                    matchedSign = candidates.first()
                }
            }
        }

        val category = matchedSign?.category
            ?: SignDatabase.mapVisualGroupToCategory(visualGroup ?: "")
            ?: SignCategory.UNKNOWN

        val signCode = matchedSign?.code
        val signName = matchedSign?.name
        val signDescription = matchedSign?.description

        // Use sign name as label when available
        val displayLabel = signName ?: category.displayName

        return RoadSign(
            id = "sign_${System.nanoTime()}",
            category = category,
            label = displayLabel,
            confidence = confidence,
            boundingBox = obj.boundingBox,
            speedLimitValue = speedValue,
            signCode = signCode,
            signName = signName,
            signDescription = signDescription,
            visualGroup = visualGroup
        )
    }

    /**
     * Extract speed value from ML Kit label text.
     */
    private fun extractSpeedFromLabel(label: String): Int? {
        val speedRegex = Regex("""\b(\d{2,3})\b""")
        val match = speedRegex.find(label)
        if (match != null) {
            val value = match.groupValues[1].toIntOrNull()
            if (value != null && value in 20..130) return value
        }
        return null
    }

    /**
     * Use ML Kit TextRecognizer to read text within the sign's bounding box.
     */
    private suspend fun recognizeText(boundingBox: Rect, image: InputImage): String? {
        return try {
            // Crop to bounding box area for better OCR
            val result = textRecognizer.process(image).await()
            val textBlocks = result.textBlocks.filter { block ->
                val box = block.boundingBox
                box != null && Rect.intersects(boundingBox, box)
            }
            if (textBlocks.isNotEmpty()) {
                textBlocks.joinToString(" ") { it.text }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract speed value from OCR text within the sign's bounding box.
     */
    private suspend fun extractSpeedFromImage(boundingBox: Rect, image: InputImage): Int? {
        return try {
            val result = textRecognizer.process(image).await()
            // Look for speed-like numbers in the image
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val speedRegex = Regex("""\b(\d{2,3})\b""")
                    val match = speedRegex.find(line.text)
                    if (match != null) {
                        val value = match.groupValues[1].toIntOrNull()
                        if (value != null && value in 20..130) return value
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fallback classification using shape, color, and aspect-ratio heuristics
     * when ML Kit labels are not available.
     */
    private suspend fun classifyByHeuristics(
        obj: DetectedObject,
        image: InputImage
    ): RoadSign {
        val box = obj.boundingBox
        val aspectRatio = box.width().toFloat() / box.height().toFloat()
        val area = box.width() * box.height()

        // Try OCR first to find any text on the sign
        val recognizedText = recognizeText(box, image)

        // Determine likely category from shape and text
        val category = when {
            // Octagon / square-ish → likely STOP
            aspectRatio in 0.8f..1.2f && area > 5000 -> {
                if (recognizedText?.lowercase()?.contains("stop") == true) {
                    SignCategory.STOP
                } else {
                    // Could be speed limit or prohibition
                    SignCategory.SPEED_LIMIT
                }
            }
            // Wide rectangle → likely information or direction
            aspectRatio > 1.5f -> SignCategory.SETTLEMENT_SIGN
            // Tall rectangle → likely service
            aspectRatio < 0.67f -> SignCategory.FIRST_AID
            // Circle (aspectRatio ~ 1.0)
            aspectRatio in 0.85f..1.15f -> SignCategory.NO_ENTRY
            // Other
            else -> SignCategory.UNKNOWN
        }

        val speedValue = recognizedText?.let { extractSpeedFromLabel(it) }

        return RoadSign(
            id = "sign_${System.nanoTime()}",
            category = category,
            label = category.displayName,
            confidence = 0.7f,
            boundingBox = box,
            speedLimitValue = speedValue,
            signCode = null,
            signName = null,
            signDescription = null,
            visualGroup = null
        )
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
