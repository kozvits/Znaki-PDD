package com.roadsignai.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadsignai.domain.model.RoadSign
import com.roadsignai.domain.model.SignCategory
import com.roadsignai.presentation.theme.*

/**
 * Card displaying a single detected road sign with icon, name, and distance.
 */
@Composable
fun SignCard(
    sign: RoadSign,
    distance: String? = null,
    modifier: Modifier = Modifier
) {
    val categoryColor = when {
        sign.category.priority == 1 -> SignProhibitory
        sign.category.priority == 2 -> SignWarning
        else -> SignInformational
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(CardBorder.copy(alpha = 0.5f))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sign icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(categoryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getSignIcon(sign.category),
                    contentDescription = sign.label,
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Sign name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sign.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnDarkSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (sign.speedLimitValue != null) {
                    Text(
                        text = "${sign.speedLimitValue} км/ч",
                        style = MaterialTheme.typography.labelMedium,
                        color = categoryColor
                    )
                }
            }

            // Distance badge
            if (distance != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DarkSurfaceVariant
                ) {
                    Text(
                        text = distance,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnDarkSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Confidence badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = categoryColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${(sign.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = categoryColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Returns an icon for each sign category.
 */
fun getSignIcon(category: SignCategory): ImageVector {
    return when (category) {
        SignCategory.STOP -> Icons.Default.Dangerous
        SignCategory.SPEED_LIMIT -> Icons.Default.Circle
        SignCategory.YIELD -> Icons.Default.Circle
        SignCategory.MAIN_ROAD -> Icons.Default.Circle
        SignCategory.PEDESTRIAN_CROSSING -> Icons.Default.Circle
        SignCategory.NO_ENTRY -> Icons.Default.Dangerous
        SignCategory.NO_TRAFFIC -> Icons.Default.Circle
        SignCategory.NO_OVERTAKING -> Icons.Default.Circle
        SignCategory.NO_PARKING -> Icons.Default.Circle
        SignCategory.NO_STOPPING -> Icons.Default.Circle
        SignCategory.END_OF_RESTRICTIONS -> Icons.Default.Circle
        SignCategory.CROSSWALK -> Icons.Default.Circle
        SignCategory.UNKNOWN -> Icons.Default.Circle
    }
}
