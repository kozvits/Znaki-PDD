package com.roadsignai.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
 * Card displaying a single detected road sign with icon, name, code, and confidence.
 * Shows both the sign name from the Belarusian ПДД РБ database and its code.
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

            // Sign name and code
            Column(modifier = Modifier.weight(1f)) {
                // Display sign name from database if available, else category name
                val displayName = sign.signName ?: sign.label
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnDarkSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                // Bottom row: sign code + speed value
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sign.signCode != null) {
                        Text(
                            text = "ПДД РБ ${sign.signCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnDarkSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    if (sign.speedLimitValue != null) {
                        Text(
                            text = "${sign.speedLimitValue} км/ч",
                            style = MaterialTheme.typography.labelMedium,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
 * Uses distinct icons for each visual group for quick recognition.
 * All icons are from Material Icons Extended (compose.material-icons-extended).
 */
fun getSignIcon(category: SignCategory): ImageVector {
    return when (category) {
        // Priority — critical
        SignCategory.STOP -> Icons.Default.Dangerous
        SignCategory.YIELD -> Icons.Default.ArrowBack
        SignCategory.MAIN_ROAD -> Icons.Default.ArrowForward
        SignCategory.END_MAIN_ROAD -> Icons.Default.ArrowForward
        SignCategory.INTERSECTION_WARNING -> Icons.Default.AltRoute
        SignCategory.ONCOMING_PRIORITY -> Icons.Default.SwapHoriz
        SignCategory.PRIORITY_OVER_ONCOMING -> Icons.Default.SwapHoriz

        // Warning
        SignCategory.RAILROAD_CROSSING -> Icons.Default.Train
        SignCategory.PEDESTRIAN_WARNING -> Icons.Default.DirectionsWalk
        SignCategory.CHILDREN_WARNING -> Icons.Default.Face
        SignCategory.TRAFFIC_LIGHT_WARNING -> Icons.Default.Traffic
        SignCategory.ROUNDABOUT_WARNING -> Icons.Default.Loop
        SignCategory.TWO_WAY_WARNING -> Icons.Default.SwapHoriz
        SignCategory.DANGEROUS_CURVE -> Icons.Default.TurnSlightRight
        SignCategory.SLIPPERY_ROAD -> Icons.Default.Warning
        SignCategory.ROAD_NARROWS -> Icons.Default.Compress
        SignCategory.DRAWBRIDGE -> Icons.Default.Construction
        SignCategory.STEEP_HILL -> Icons.Default.South
        SignCategory.ROAD_WORKS -> Icons.Default.Construction
        SignCategory.ANIMALS_WARNING -> Icons.Default.Pets
        SignCategory.SOFT_SHOULDER -> Icons.Default.Warning
        SignCategory.CROSSWIND -> Icons.Default.Air
        SignCategory.BUMPY_ROAD -> Icons.Default.Warning
        SignCategory.LOOSE_GRAVEL -> Icons.Default.Warning
        SignCategory.CONGESTION -> Icons.Default.Traffic
        SignCategory.LOW_FLYING_AIRCRAFT -> Icons.Default.Flight
        SignCategory.FALLING_ROCKS -> Icons.Default.Warning
        SignCategory.ICE_WARNING -> Icons.Default.AcUnit
        SignCategory.DANGEROUS_AREA -> Icons.Default.Warning
        SignCategory.SPEED_CONTROL -> Icons.Default.Speed
        SignCategory.GENERAL_WARNING -> Icons.Default.Warning

        // Prohibition
        SignCategory.NO_ENTRY -> Icons.Default.Dangerous
        SignCategory.NO_VEHICLES -> Icons.Default.DriveEta
        SignCategory.NO_TRAFFIC -> Icons.Default.DriveEta
        SignCategory.NO_MOTOR_VEHICLES -> Icons.Default.DriveEta
        SignCategory.NO_TRUCKS -> Icons.Default.LocalShipping
        SignCategory.NO_MOTORCYCLES -> Icons.Default.TwoWheeler
        SignCategory.NO_BICYCLES -> Icons.Default.PedalBike
        SignCategory.NO_PEDESTRIANS -> Icons.Default.DirectionsWalk
        SignCategory.SPEED_LIMIT -> Icons.Default.Speed
        SignCategory.END_SPEED_LIMIT -> Icons.Default.Speed
        SignCategory.NO_OVERTAKING -> Icons.Default.DriveEta
        SignCategory.END_NO_OVERTAKING -> Icons.Default.DriveEta
        SignCategory.NO_STOPPING -> Icons.Default.NotInterested
        SignCategory.NO_PARKING -> Icons.Default.NotInterested
        SignCategory.NO_RIGHT_TURN -> Icons.Default.TurnRight
        SignCategory.NO_LEFT_TURN -> Icons.Default.TurnLeft
        SignCategory.NO_U_TURN -> Icons.Default.Loop
        SignCategory.CUSTOMS -> Icons.Default.AccountBalance
        SignCategory.DANGER -> Icons.Default.Dangerous
        SignCategory.VEHICLE_RESTRICTION -> Icons.Default.Height
        SignCategory.END_ALL_RESTRICTIONS -> Icons.Default.CheckCircle
        SignCategory.NO_DANGEROUS_GOODS -> Icons.Default.Warning
        SignCategory.SPEED_LIMIT_ZONE -> Icons.Default.Speed

        // Mandatory
        SignCategory.DIRECTION_MANDATORY -> Icons.Default.ArrowUpward
        SignCategory.ROUNDABOUT -> Icons.Default.Loop
        SignCategory.CARS_ONLY -> Icons.Default.DirectionsCar
        SignCategory.MINIMUM_SPEED -> Icons.Default.Speed
        SignCategory.PEDESTRIAN_CYCLE_PATH -> Icons.Default.DirectionsBike
        SignCategory.HORSE_RIDING -> Icons.Default.Pets

        // Information
        SignCategory.MOTORWAY -> Icons.Default.Signpost
        SignCategory.EXPRESSWAY -> Icons.Default.Signpost
        SignCategory.ONE_WAY -> Icons.Default.ArrowForward
        SignCategory.PEDESTRIAN_CROSSING -> Icons.Default.DirectionsWalk
        SignCategory.CYCLE_CROSSING -> Icons.Default.PedalBike
        SignCategory.PEDESTRIAN_UNDERPASS -> Icons.Default.Stairs
        SignCategory.RECOMMENDED_SPEED -> Icons.Default.Speed
        SignCategory.PARKING -> Icons.Default.LocalParking
        SignCategory.DEAD_END -> Icons.Default.Block
        SignCategory.RESIDENTIAL_ZONE -> Icons.Default.Home
        SignCategory.PEDESTRIAN_ZONE -> Icons.Default.DirectionsWalk
        SignCategory.SETTLEMENT_SIGN -> Icons.Default.LocationCity
        SignCategory.ROAD_NUMBER -> Icons.Default.Signpost
        SignCategory.KILOMETER_MARKER -> Icons.Default.Signpost
        SignCategory.TOLL_ROAD -> Icons.Default.AttachMoney

        // Service
        SignCategory.FIRST_AID -> Icons.Default.MedicalServices
        SignCategory.HOSPITAL -> Icons.Default.LocalHospital
        SignCategory.GAS_STATION -> Icons.Default.LocalGasStation
        SignCategory.CHARGING_STATION -> Icons.Default.ElectricBolt
        SignCategory.CAR_SERVICE -> Icons.Default.Build
        SignCategory.CAR_WASH -> Icons.Default.CleaningServices
        SignCategory.TELEPHONE -> Icons.Default.Phone
        SignCategory.RESTAURANT -> Icons.Default.Restaurant
        SignCategory.DRINKING_WATER -> Icons.Default.WaterDrop
        SignCategory.HOTEL -> Icons.Default.Hotel
        SignCategory.CAMPING -> Icons.Default.Luggage
        SignCategory.REST_AREA -> Icons.Default.Chair
        SignCategory.POLICE -> Icons.Default.LocalPolice
        SignCategory.TOILET -> Icons.Default.Wc
        SignCategory.LANDMARK -> Icons.Default.PhotoCamera

        // Other
        SignCategory.ADDITIONAL_PLATE -> Icons.Default.Info
        SignCategory.UNKNOWN -> Icons.Default.HelpOutline
    }
}
