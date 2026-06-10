package com.roadsignai.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.roadsignai.domain.model.RoadSign
import com.roadsignai.presentation.theme.SignProhibitory
import com.roadsignai.presentation.theme.SignWarning
import com.roadsignai.presentation.theme.SignInformational
import com.roadsignai.presentation.theme.SignMandatory

/**
 * Canvas overlay that draws bounding boxes around detected road signs
 * on top of the camera preview.
 *
 * @param signs detected road signs with bounding boxes
 * @param previewWidth width of the camera preview
 * @param previewHeight height of the camera preview
 */
@Composable
fun SignOverlay(
    signs: List<RoadSign>,
    previewWidth: Int,
    previewHeight: Int,
    modifier: Modifier = Modifier
) {
    if (signs.isEmpty()) return

    Canvas(modifier = modifier) {
        val scaleX = size.width / previewWidth.toFloat()
        val scaleY = size.height / previewHeight.toFloat()

        for (sign in signs) {
            val box = sign.boundingBox

            // Scale bounding box to canvas size
            val left = box.left * scaleX
            val top = box.top * scaleY
            val right = box.right * scaleX
            val bottom = box.bottom * scaleY

            val color = when {
                sign.category.priority == 1 -> SignProhibitory
                sign.category.priority == 2 -> SignWarning
                sign.category.isProhibitory -> SignProhibitory
                else -> SignMandatory
            }

            // Draw bounding box
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4f)
            )

            // Draw semi-transparent fill
            drawRect(
                color = color.copy(alpha = 0.15f),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top)
            )

            // Draw label background
            val labelText = sign.label
            val paint = android.graphics.Paint().apply {
                this.color = color.toArgb()
                textSize = 36f
                isAntiAlias = true
            }

            val textWidth = paint.measureText(labelText)
            val labelHeight = 48f

            // Label background
            drawRect(
                color = color.copy(alpha = 0.85f),
                topLeft = Offset(left, top - labelHeight),
                size = Size(textWidth + 16f, labelHeight)
            )

            // Label text
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                left + 8f,
                top - 12f,
                paint.apply { color = android.graphics.Color.WHITE }
            )

            // Confidence badge
            if (sign.confidence > 0) {
                val confText = "${(sign.confidence * 100).toInt()}%"
                val confWidth = paint.measureText(confText)

                drawRect(
                    color = SignInformational.copy(alpha = 0.85f),
                    topLeft = Offset(right - confWidth - 16f, top - labelHeight),
                    size = Size(confWidth + 16f, labelHeight)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    confText,
                    right - confWidth - 8f,
                    top - 12f,
                    paint.apply { color = android.graphics.Color.WHITE; textSize = 28f }
                )
            }
        }
    }
}
