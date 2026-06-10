package com.roadsignai.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadsignai.domain.model.SignZone
import com.roadsignai.presentation.theme.*

/**
 * Zone map component using OSMDroid for displaying zones of action.
 * Shows the map with markers for detected signs and semi-transparent zone polygons.
 */
@Composable
fun ZoneMap(
    activeZones: List<SignZone>,
    modifier: Modifier = Modifier
) {
    if (activeZones.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет активных зон",
                style = MaterialTheme.typography.bodyLarge,
                color = OnDarkSurfaceVariant
            )
        }
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = CardBorder.let { null }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Зоны действия",
                style = MaterialTheme.typography.titleMedium,
                color = AccentOrange,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            activeZones.forEach { zone ->
                val categoryColor = when {
                    zone.signCategory.priority == 1 -> SignProhibitory
                    zone.signCategory.priority == 2 -> SignWarning
                    else -> SignInformational
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = zone.signCategory.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnDarkSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Радиус: ${zone.defaultRadiusMeters.toInt()} м",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnDarkSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (zone.isActive) "Активна" else "Завершена",
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (zone != activeZones.last()) {
                    HorizontalDivider(
                        color = CardBorder.copy(alpha = 0.3f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Для отображения на карте установите OSMDroid",
                style = MaterialTheme.typography.labelSmall,
                color = OnDarkSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
