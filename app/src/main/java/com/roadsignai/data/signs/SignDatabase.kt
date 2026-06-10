package com.roadsignai.data.signs

import android.content.Context
import com.roadsignai.domain.model.SignCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a single sign entry from the Belarusian ПДД РБ database.
 */
data class BelarusSign(
    val code: String,
    val name: String,
    val description: String,
    val section: String,
    val sectionLabel: String,
    val shape: String,
    val colorScheme: String,
    val visualGroup: String,
    val category: SignCategory
)

/**
 * Loads and queries the Belarusian road sign database (by_signs.json).
 * Matches ML Kit detections to specific signs.
 */
@Singleton
class SignDatabase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var signs: List<BelarusSign> = emptyList()
    private var loaded = false

    /** Map of visual_group -> list of signs in that group. */
    private val groupIndex: MutableMap<String, List<BelarusSign>> = mutableMapOf()
    /** Map of sign code -> sign. */
    private val codeIndex: MutableMap<String, BelarusSign> = mutableMapOf()
    /** Map of section -> list of signs. */
    private val sectionIndex: MutableMap<String, List<BelarusSign>> = mutableMapOf()

    /**
     * Ensure the database is loaded from assets.
     */
    suspend fun ensureLoaded() {
        if (loaded) return
        loadFromAssets()
    }

    /**
     * Load signs from the JSON asset file.
     */
    private fun loadFromAssets() {
        try {
            val inputStream = context.assets.open("by_signs.json")
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val text = reader.readText()
            reader.close()
            inputStream.close()

            val jsonArray = JSONArray(text)
            val parsedSigns = mutableListOf<BelarusSign>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val sign = parseSign(obj)
                parsedSigns.add(sign)
            }

            signs = parsedSigns
            buildIndexes()
            loaded = true
        } catch (e: Exception) {
            // If assets not available (e.g., in tests), load empty
            signs = emptyList()
            loaded = true
        }
    }

    private fun parseSign(obj: JSONObject): BelarusSign {
        val code = obj.getString("code")
        val name = obj.getString("name")
        val description = obj.optString("description", "")
        val section = obj.getString("section")
        val sectionLabel = obj.getString("section_label")
        val shape = obj.getString("shape")
        val colorScheme = obj.getString("color_scheme")
        val visualGroup = obj.getString("visual_group")
        val category = mapVisualGroupToCategory(visualGroup, section)
        return BelarusSign(
            code = code,
            name = name,
            description = description,
            section = section,
            sectionLabel = sectionLabel,
            shape = shape,
            colorScheme = colorScheme,
            visualGroup = visualGroup,
            category = category
        )
    }

    private fun buildIndexes() {
        groupIndex.clear()
        codeIndex.clear()
        sectionIndex.clear()

        for (sign in signs) {
            // Group index
            val existing = groupIndex.getOrDefault(sign.visualGroup, emptyList())
            groupIndex[sign.visualGroup] = existing + sign

            // Code index
            codeIndex[sign.code] = sign

            // Section index
            val sectionExisting = sectionIndex.getOrDefault(sign.section, emptyList())
            sectionIndex[sign.section] = sectionExisting + sign
        }
    }

    /**
     * Find signs by visual group (ML Kit label match).
     */
    fun findByVisualGroup(visualGroup: String): List<BelarusSign> {
        if (!loaded) return emptyList()
        return groupIndex[visualGroup] ?: emptyList()
    }

    /**
     * Find a sign by its ПДД РБ code (e.g. "3.24.1", "2.5").
     */
    fun findByCode(code: String): BelarusSign? {
        if (!loaded) return null
        return codeIndex[code]
    }

    /**
     * Find signs by section.
     */
    fun findBySection(section: String): List<BelarusSign> {
        if (!loaded) return emptyList()
        return sectionIndex[section] ?: emptyList()
    }

    /**
     * Search signs by name (partial match, case-insensitive).
     */
    fun searchByName(query: String): List<BelarusSign> {
        if (!loaded || query.isBlank()) return emptyList()
        val lower = query.lowercase()
        return signs.filter { it.name.lowercase().contains(lower) }
    }

    /**
     * Search signs by description (partial match, case-insensitive).
     */
    fun searchByDescription(query: String): List<BelarusSign> {
        if (!loaded || query.isBlank()) return emptyList()
        val lower = query.lowercase()
        return signs.filter { it.description.lowercase().contains(lower) }
    }

    /**
     * Get signs by shape.
     */
    fun findByShape(shape: String): List<BelarusSign> {
        if (!loaded) return emptyList()
        return signs.filter { it.shape == shape }
    }

    /**
     * Get signs by color scheme.
     */
    fun findByColorScheme(colorScheme: String): List<BelarusSign> {
        if (!loaded) return emptyList()
        return signs.filter { it.colorScheme == colorScheme }
    }

    /**
     * Get all signs.
     */
    fun getAllSigns(): List<BelarusSign> = signs

    /**
     * Get total count.
     */
    fun getCount(): Int = signs.size

    /**
     * Find best match for a recognized text on a sign (e.g., speed number, destination).
     * Used with TextRecognizer results.
     */
    fun findBestMatchForText(
        recognizedText: String,
        candidateGroup: String? = null
    ): Pair<BelarusSign?, Float> {
        if (!loaded || recognizedText.isBlank()) return null to 0f

        val candidates = if (candidateGroup != null) {
            findByVisualGroup(candidateGroup)
        } else {
            signs
        }
        if (candidates.isEmpty()) return null to 0f

        val cleanText = recognizedText.trim().replace("\\s+".toRegex(), " ")

        // Try exact match on code
        val codeMatch = candidates.find { it.code == cleanText }
        if (codeMatch != null) return codeMatch to 1.0f

        // Try number match (for speed limits)
        val numberMatch = Regex("""\d{2,3}""").find(cleanText)
        if (numberMatch != null && candidateGroup != null) {
            // Speed limit signs with this number
            return candidates.firstOrNull() to 0.9f
        }

        return null to 0f
    }

    companion object {
        /**
         * Map a visual group string to the appropriate SignCategory enum.
         */
        fun mapVisualGroupToCategory(visualGroup: String, section: String = ""): SignCategory {
            return when (visualGroup) {
                // Priority
                "stop" -> SignCategory.STOP
                "yield" -> SignCategory.YIELD
                "main_road" -> SignCategory.MAIN_ROAD
                "intersection_warning" -> SignCategory.INTERSECTION_WARNING
                "oncoming_traffic_priority" -> SignCategory.ONCOMING_PRIORITY
                "priority_over_oncoming" -> SignCategory.PRIORITY_OVER_ONCOMING

                // Prohibition
                "no_entry" -> SignCategory.NO_ENTRY
                "no_vehicles" -> SignCategory.NO_VEHICLES
                "no_bicycles" -> SignCategory.NO_BICYCLES
                "no_pedestrians" -> SignCategory.NO_PEDESTRIANS
                "no_right_turn" -> SignCategory.NO_RIGHT_TURN
                "no_left_turn" -> SignCategory.NO_LEFT_TURN
                "no_u_turn" -> SignCategory.NO_U_TURN
                "no_overtaking" -> SignCategory.NO_OVERTAKING
                "speed_limit" -> SignCategory.SPEED_LIMIT
                "end_speed_limit" -> SignCategory.END_SPEED_LIMIT
                "no_stopping" -> SignCategory.NO_STOPPING
                "no_parking" -> SignCategory.NO_PARKING
                "customs" -> SignCategory.CUSTOMS
                "danger" -> SignCategory.DANGER
                "vehicle_restriction" -> SignCategory.VEHICLE_RESTRICTION
                "end_all_restrictions" -> SignCategory.END_ALL_RESTRICTIONS
                "no_dangerous_goods" -> SignCategory.NO_DANGEROUS_GOODS
                "speed_limit_zone" -> SignCategory.SPEED_LIMIT_ZONE
                "no_personal_mobility" -> SignCategory.NO_VEHICLES
                "minimum_distance" -> SignCategory.VEHICLE_RESTRICTION
                "prohibition_circle" -> SignCategory.NO_ENTRY

                // Warning
                "warning_triangle" -> SignCategory.GENERAL_WARNING
                "railroad_crossing" -> SignCategory.RAILROAD_CROSSING
                "pedestrian_warning" -> SignCategory.PEDESTRIAN_WARNING
                "children_warning" -> SignCategory.CHILDREN_WARNING
                "traffic_light_warning" -> SignCategory.TRAFFIC_LIGHT_WARNING
                "roundabout_warning" -> SignCategory.ROUNDABOUT_WARNING
                "two_way_warning" -> SignCategory.TWO_WAY_WARNING
                "dangerous_curve" -> SignCategory.DANGEROUS_CURVE
                "slippery_warning" -> SignCategory.SLIPPERY_ROAD
                "road_narrows" -> SignCategory.ROAD_NARROWS
                "drawbridge" -> SignCategory.DRAWBRIDGE
                "quay_warning" -> SignCategory.GENERAL_WARNING
                "steep_hill" -> SignCategory.STEEP_HILL
                "road_works" -> SignCategory.ROAD_WORKS
                "animals_warning" -> SignCategory.ANIMALS_WARNING
                "soft_shoulder" -> SignCategory.SOFT_SHOULDER
                "crosswind" -> SignCategory.CROSSWIND
                "bumpy_road" -> SignCategory.BUMPY_ROAD
                "loose_gravel" -> SignCategory.LOOSE_GRAVEL
                "congestion" -> SignCategory.CONGESTION
                "low_flying_aircraft" -> SignCategory.LOW_FLYING_AIRCRAFT
                "falling_rocks" -> SignCategory.FALLING_ROCKS
                "ice_warning" -> SignCategory.ICE_WARNING
                "dangerous_area" -> SignCategory.DANGEROUS_AREA
                "speed_control" -> SignCategory.SPEED_CONTROL
                "amphibian_migration" -> SignCategory.GENERAL_WARNING
                "turn_direction" -> SignCategory.DANGEROUS_CURVE

                // Mandatory
                "direction_mandatory" -> SignCategory.DIRECTION_MANDATORY
                "roundabout" -> SignCategory.ROUNDABOUT
                "cars_only" -> SignCategory.CARS_ONLY
                "minimum_speed" -> SignCategory.MINIMUM_SPEED
                "end_minimum_speed" -> SignCategory.MINIMUM_SPEED
                "pedestrian_cycle_path" -> SignCategory.PEDESTRIAN_CYCLE_PATH
                "horse_riding" -> SignCategory.HORSE_RIDING
                "pedestrian_cycle_shared" -> SignCategory.PEDESTRIAN_CYCLE_PATH
                "mandatory_circle" -> SignCategory.DIRECTION_MANDATORY
                "obstacle_avoidance" -> SignCategory.DIRECTION_MANDATORY

                // Information
                "motorway" -> SignCategory.MOTORWAY
                "expressway" -> SignCategory.EXPRESSWAY
                "one_way" -> SignCategory.ONE_WAY
                "one_way_entry" -> SignCategory.ONE_WAY
                "lane_direction" -> SignCategory.DIRECTION_MANDATORY
                "bus_lane" -> SignCategory.DIRECTION_MANDATORY
                "turnaround" -> SignCategory.DIRECTION_MANDATORY
                "pedestrian_crossing" -> SignCategory.PEDESTRIAN_CROSSING
                "cycle_crossing" -> SignCategory.CYCLE_CROSSING
                "pedestrian_underpass" -> SignCategory.PEDESTRIAN_UNDERPASS
                "pedestrian_overpass" -> SignCategory.PEDESTRIAN_UNDERPASS
                "recommended_speed" -> SignCategory.RECOMMENDED_SPEED
                "dead_end" -> SignCategory.DEAD_END
                "settlement_sign" -> SignCategory.SETTLEMENT_SIGN
                "kilometer_marker" -> SignCategory.KILOMETER_MARKER
                "road_number" -> SignCategory.ROAD_NUMBER
                "residential_zone" -> SignCategory.RESIDENTIAL_ZONE
                "pedestrian_zone" -> SignCategory.PEDESTRIAN_ZONE
                "parking" -> SignCategory.PARKING
                "toll_road" -> SignCategory.TOLL_ROAD
                "information_rectangle" -> SignCategory.SETTLEMENT_SIGN

                // Service
                "first_aid" -> SignCategory.FIRST_AID
                "hospital" -> SignCategory.HOSPITAL
                "gas_station" -> SignCategory.GAS_STATION
                "charging_station" -> SignCategory.CHARGING_STATION
                "car_service" -> SignCategory.CAR_SERVICE
                "car_wash" -> SignCategory.CAR_WASH
                "telephone" -> SignCategory.TELEPHONE
                "restaurant" -> SignCategory.RESTAURANT
                "drinking_water" -> SignCategory.DRINKING_WATER
                "hotel" -> SignCategory.HOTEL
                "camping" -> SignCategory.CAMPING
                "rest_area" -> SignCategory.REST_AREA
                "police" -> SignCategory.POLICE
                "toilet" -> SignCategory.TOILET
                "landmark" -> SignCategory.LANDMARK
                "service_rectangle" -> SignCategory.FIRST_AID

                // Additional info
                "additional_plate" -> SignCategory.ADDITIONAL_PLATE
                "distance_plate" -> SignCategory.ADDITIONAL_PLATE
                "zone_plate" -> SignCategory.ADDITIONAL_PLATE
                "direction_plate" -> SignCategory.ADDITIONAL_PLATE
                "vehicle_type_plate" -> SignCategory.ADDITIONAL_PLATE
                "time_plate" -> SignCategory.ADDITIONAL_PLATE
                "parking_method_plate" -> SignCategory.ADDITIONAL_PLATE

                else -> SignCategory.UNKNOWN
            }
        }

        /**
         * Map ML Kit label text to a visual group.
         */
        fun mapMlKitLabelToVisualGroup(label: String): String? {
            val normalized = label.lowercase().trim()
            return when {
                normalized.contains("stop") -> "stop"
                normalized.contains("speed limit") || normalized.contains("speed") -> "speed_limit"
                normalized.contains("yield") -> "yield"
                normalized.contains("pedestrian crossing") || normalized.contains("crosswalk") -> "pedestrian_crossing"
                normalized.contains("no entry") || normalized.contains("do not enter") -> "no_entry"
                normalized.contains("no parking") -> "no_parking"
                normalized.contains("no stopping") || normalized.contains("no stop") -> "no_stopping"
                normalized.contains("no overtaking") || normalized.contains("no passing") -> "no_overtaking"
                normalized.contains("no traffic") || normalized.contains("no vehicles") || normalized.contains("do not pass") -> "no_vehicles"
                normalized.contains("main road") -> "main_road"
                normalized.contains("end of") || normalized.contains("end all") -> "end_all_restrictions"
                normalized.contains("one way") -> "one_way"
                normalized.contains("roundabout") || normalized.contains("circular") -> "roundabout"
                normalized.contains("children") || normalized.contains("child") || normalized.contains("school") -> "children_warning"
                normalized.contains("pedestrian") -> "pedestrian_warning"
                normalized.contains("road work") || normalized.contains("construction") -> "road_works"
                normalized.contains("bicycle") || normalized.contains("bike") -> "no_bicycles"
                normalized.contains("motorway") || normalized.contains("highway") || normalized.contains("freeway") -> "motorway"
                normalized.contains("parking") -> "parking"
                normalized.contains("customs") || normalized.contains("border") -> "customs"
                normalized.contains("danger") -> "danger"
                normalized.contains("hospital") || normalized.contains("medical") -> "hospital"
                normalized.contains("gas station") || normalized.contains("fuel") || normalized.contains("petrol") -> "gas_station"
                normalized.contains("restaurant") || normalized.contains("food") || normalized.contains("eating") -> "restaurant"
                normalized.contains("hotel") || normalized.contains("motel") || normalized.contains("lodging") -> "hotel"
                normalized.contains("camping") || normalized.contains("camp") -> "camping"
                normalized.contains("phone") || normalized.contains("telephone") -> "telephone"
                normalized.contains("police") || normalized.contains("dps") -> "police"
                normalized.contains("toilet") || normalized.contains("wc") || normalized.contains("restroom") -> "toilet"
                normalized.contains("first aid") || normalized.contains("aid") -> "first_aid"
                normalized.contains("handicap") || normalized.contains("disabled") -> "parking"
                normalized.contains("no left turn") || normalized.contains("left turn") -> "no_left_turn"
                normalized.contains("no right turn") || normalized.contains("right turn") -> "no_right_turn"
                normalized.contains("no u turn") || normalized.contains("no uturn") -> "no_u_turn"
                normalized.contains("crosswind") || normalized.contains("wind") -> "crosswind"
                normalized.contains("slippery") || normalized.contains("slide") -> "slippery_warning"
                normalized.contains("hill") || normalized.contains("steep") || normalized.contains("grade") -> "steep_hill"
                normalized.contains("bumpy") || normalized.contains("rough") || normalized.contains("uneven") -> "bumpy_road"
                normalized.contains("soft shoulder") || normalized.contains("shoulder") -> "soft_shoulder"
                normalized.contains("falling rocks") || normalized.contains("rocks") || normalized.contains("rock") -> "falling_rocks"
                normalized.contains("low flying") || normalized.contains("aircraft") || normalized.contains("plane") -> "low_flying_aircraft"
                normalized.contains("animals") || normalized.contains("deer") || normalized.contains("moose") || normalized.contains("wild") -> "animals_warning"
                normalized.contains("dip") || normalized.contains("bump") -> "bumpy_road"
                normalized.contains("traffic light") || normalized.contains("signal") -> "traffic_light_warning"
                normalized.contains("truck") || normalized.contains("lorry") || normalized.contains("heavy") -> "no_trucks"
                normalized.contains("no pedestrian") -> "no_pedestrians"
                normalized.contains("detour") || normalized.contains("diversion") -> "dead_end"
                normalized.contains("toll") || normalized.contains("pay") -> "toll_road"
                normalized.contains("dead end") || normalized.contains("no outlet") -> "dead_end"
                normalized.contains("car wash") || normalized.contains("wash") -> "car_wash"
                normalized.contains("car service") || normalized.contains("repair") || normalized.contains("mechanical") -> "car_service"
                normalized.contains("water") || normalized.contains("drink") -> "drinking_water"
                normalized.contains("rest area") || normalized.contains("rest stop") || normalized.contains("lay-by") -> "rest_area"
                normalized.contains("cattle") || normalized.contains("farm") || normalized.contains("livestock") -> "animals_warning"
                normalized.contains("two way") || normalized.contains("2 way") -> "two_way_warning"
                normalized.contains("narrow") || normalized.contains("narrows") -> "road_narrows"
                normalized.contains("gravel") || normalized.contains("chip") || normalized.contains("loose") -> "loose_gravel"
                normalized.contains("ice") || normalized.contains("snow") || normalized.contains("frost") -> "ice_warning"
                else -> null
            }
        }
    }
}
