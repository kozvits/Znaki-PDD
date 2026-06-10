package com.roadsignai.domain.model

import android.graphics.Rect
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a detected road sign with its category, position, and confidence.
 * Now enriched with Belarusian sign database info.
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
    val longitude: Double? = null,
    /** Belarusian sign code from ПДД РБ (e.g. "3.24.1", "2.5") */
    val signCode: String? = null,
    /** Full Russian name from the sign database */
    val signName: String? = null,
    /** Description / пояснение from the sign database */
    val signDescription: String? = null,
    /** Visual group key for matching */
    val visualGroup: String? = null
) : Parcelable

/**
 * Categories of road signs according to Belarusian traffic rules (ПДД РБ).
 * The priority value determines TTS urgency (higher = more critical).
 */
enum class SignCategory(
    val displayName: String,
    val priority: Int,   // 1 = critical, 2 = warning, 3 = informational
    val isProhibitory: Boolean = false
) {
    // === Предупреждающие знаки (Warning) ===
    RAILROAD_CROSSING("Железнодорожный переезд", 1),
    PEDESTRIAN_WARNING("Пешеходный переход впереди", 2),
    CHILDREN_WARNING("Осторожно, дети", 2),
    TRAFFIC_LIGHT_WARNING("Светофорное регулирование", 3),
    ROUNDABOUT_WARNING("Круговое движение впереди", 3),
    TWO_WAY_WARNING("Двустороннее движение", 2),
    DANGEROUS_CURVE("Опасный поворот", 2),
    SLIPPERY_ROAD("Скользкая дорога", 2),
    ROAD_NARROWS("Сужение дороги", 2),
    DRAWBRIDGE("Разводной мост", 2),
    STEEP_HILL("Крутой спуск/подъём", 2),
    ROAD_WORKS("Дорожные работы", 2),
    ANIMALS_WARNING("Дикие животные", 2),
    SOFT_SHOULDER("Опасная обочина", 2),
    CROSSWIND("Боковой ветер", 2),
    BUMPY_ROAD("Неровная дорога", 2),
    LOOSE_GRAVEL("Выброс гравия", 2),
    CONGESTION("Затор на дороге", 3),
    LOW_FLYING_AIRCRAFT("Низколетящие самолёты", 3),
    FALLING_ROCKS("Падение камней", 2),
    ICE_WARNING("Гололедица", 1),
    DANGEROUS_AREA("Аварийно-опасный участок", 1),
    SPEED_CONTROL("Контроль скорости", 3),
    GENERAL_WARNING("Предупреждающий знак", 2),

    // === Знаки приоритета (Priority) ===
    STOP("Стоп", 1),
    YIELD("Уступи дорогу", 2),
    MAIN_ROAD("Главная дорога", 3),
    END_MAIN_ROAD("Конец главной дороги", 3),
    INTERSECTION_WARNING("Пересечение дорог", 2),
    ONCOMING_PRIORITY("Преимущество встречного движения", 2),
    PRIORITY_OVER_ONCOMING("Преимущество перед встречным движением", 2),

    // === Запрещающие знаки (Prohibition) ===
    NO_ENTRY("Въезд запрещён", 1, isProhibitory = true),
    NO_VEHICLES("Движение запрещено", 2, isProhibitory = true),
    NO_TRAFFIC("Движение запрещено", 2, isProhibitory = true),
    NO_MOTOR_VEHICLES("Механические ТС запрещены", 2, isProhibitory = true),
    NO_TRUCKS("Грузовым запрещено", 2, isProhibitory = true),
    NO_MOTORCYCLES("Мотоциклы запрещены", 2, isProhibitory = true),
    NO_BICYCLES("Велосипеды запрещены", 2, isProhibitory = true),
    NO_PEDESTRIANS("Пешеходам запрещено", 2, isProhibitory = true),
    SPEED_LIMIT("Ограничение скорости", 1),
    END_SPEED_LIMIT("Конец ограничения скорости", 3),
    NO_OVERTAKING("Обгон запрещён", 2, isProhibitory = true),
    END_NO_OVERTAKING("Конец зоны запрещения обгона", 3),
    NO_STOPPING("Остановка запрещена", 1, isProhibitory = true),
    NO_PARKING("Стоянка запрещена", 1, isProhibitory = true),
    NO_RIGHT_TURN("Поворот направо запрещён", 2, isProhibitory = true),
    NO_LEFT_TURN("Поворот налево запрещён", 2, isProhibitory = true),
    NO_U_TURN("Разворот запрещён", 2, isProhibitory = true),
    CUSTOMS("Таможня", 3),
    DANGER("Опасность", 1),
    VEHICLE_RESTRICTION("Ограничение габаритов", 2, isProhibitory = true),
    END_ALL_RESTRICTIONS("Конец всех ограничений", 3),
    NO_DANGEROUS_GOODS("Опасные грузы запрещены", 2, isProhibitory = true),
    SPEED_LIMIT_ZONE("Зона ограничения скорости", 1),

    // === Предписывающие знаки (Mandatory) ===
    DIRECTION_MANDATORY("Предписание направления", 3),
    ROUNDABOUT("Круговое движение", 2),
    CARS_ONLY("Легковые автомобили", 3),
    MINIMUM_SPEED("Минимальная скорость", 3),
    PEDESTRIAN_CYCLE_PATH("Пешеходная/велосипедная дорожка", 3),
    HORSE_RIDING("Дорожка для всадников", 3),

    // === Информационно-указательные знаки (Information) ===
    MOTORWAY("Автомагистраль", 3),
    EXPRESSWAY("Дорога для автомобилей", 3),
    ONE_WAY("Одностороннее движение", 3),
    PEDESTRIAN_CROSSING("Пешеходный переход", 2),
    CYCLE_CROSSING("Велосипедный переезд", 3),
    PEDESTRIAN_UNDERPASS("Подземный переход", 3),
    RECOMMENDED_SPEED("Рекомендуемая скорость", 3),
    PARKING("Парковка", 3),
    DEAD_END("Тупик", 3),
    RESIDENTIAL_ZONE("Жилая зона", 3),
    PEDESTRIAN_ZONE("Пешеходная зона", 3),
    SETTLEMENT_SIGN("Населённый пункт", 3),
    ROAD_NUMBER("Номер дороги", 3),
    KILOMETER_MARKER("Километровый знак", 3),
    TOLL_ROAD("Платная дорога", 3),

    // === Знаки сервиса (Service) ===
    FIRST_AID("Пункт медицинской помощи", 3),
    HOSPITAL("Больница", 3),
    GAS_STATION("Автозаправка", 3),
    CHARGING_STATION("Электрозарядка", 3),
    CAR_SERVICE("СТО", 3),
    CAR_WASH("Мойка", 3),
    TELEPHONE("Телефон", 3),
    RESTAURANT("Пункт питания", 3),
    DRINKING_WATER("Питьевая вода", 3),
    HOTEL("Гостиница", 3),
    CAMPING("Кемпинг", 3),
    REST_AREA("Место отдыха", 3),
    POLICE("Милиция/ГАИ", 3),
    TOILET("Туалет", 3),
    LANDMARK("Достопримечательность", 3),

    // === Прочие ===
    ADDITIONAL_PLATE("Табличка доп. информации", 3),
    UNKNOWN("Неизвестный знак", 3);

    companion object {
        /**
         * Returns the TTS text for a given sign category and optional parameters.
         */
        fun getTtsText(
            category: SignCategory,
            speedValue: Int? = null,
            signName: String? = null,
            signDescription: String? = null
        ): String {
            val name = signName ?: category.displayName
            return when (category) {
                SPEED_LIMIT -> {
                    if (speedValue != null) "Ограничение скорости $speedValue километров в час"
                    else "Знак ограничения скорости"
                }
                STOP -> "Знак стоп. Движение без остановки запрещено"
                YIELD -> "Уступите дорогу"
                MAIN_ROAD -> "Начало главной дороги"
                PEDESTRIAN_CROSSING -> "Внимание, пешеходный переход"
                PEDESTRIAN_WARNING -> "Впереди пешеходный переход"
                CHILDREN_WARNING -> "Осторожно, дети"
                RAILROAD_CROSSING -> "Внимание, железнодорожный переезд"
                ROAD_WORKS -> "Внимание, дорожные работы"
                ICE_WARNING -> "Внимание, гололедица"
                DANGEROUS_AREA -> "Аварийно-опасный участок"
                DANGEROUS_CURVE -> "Опасный поворот"
                SLIPPERY_ROAD -> "Скользкая дорога"
                CROSSWIND -> "Боковой ветер"
                ANIMALS_WARNING -> "Осторожно, дикие животные"
                NO_ENTRY -> "Въезд запрещён"
                NO_VEHICLES -> "Движение запрещено"
                NO_TRAFFIC -> "Движение запрещено"
                NO_STOPPING -> "Остановка запрещена. Зона действия: до следующего перекрёстка"
                NO_PARKING -> "Стоянка запрещена"
                NO_OVERTAKING -> "Обгон запрещён"
                END_ALL_RESTRICTIONS -> "Конец зоны всех ограничений"
                END_SPEED_LIMIT -> "Конец ограничения скорости"
                ROUNDABOUT -> "Круговое движение"
                MOTORWAY -> "Автомагистраль"
                PEDESTRIAN_UNDERPASS -> "Подземный пешеходный переход"
                GAS_STATION -> "Автозаправочная станция"
                POLICE -> "Пост милиции или ГАИ"
                HOSPITAL -> "Больница"
                FIRST_AID -> "Пункт первой медицинской помощи"
                RESTAURANT -> "Пункт питания"
                HOTEL -> "Гостиница"
                CAMPING -> "Кемпинг"
                TOILET -> "Туалет"
                PARKING -> "Место стоянки"
                RESIDENTIAL_ZONE -> "Жилая зона"
                PEDESTRIAN_ZONE -> "Пешеходная зона"
                SETTLEMENT_SIGN -> "Населённый пункт"
                CONGESTION -> "Впереди возможен затор"
                SPEED_CONTROL -> "Контроль скорости"
                NO_BICYCLES -> "Движение велосипедов запрещено"
                NO_RIGHT_TURN -> "Поворот направо запрещён"
                NO_LEFT_TURN -> "Поворот налево запрещён"
                NO_U_TURN -> "Разворот запрещён"
                DANGER -> "Опасность, проезд запрещён"
                CUSTOMS -> "Таможня, необходима остановка"
                ONE_WAY -> "Дорога с односторонним движением"
                DEAD_END -> "Тупик"
                RECOMMENDED_SPEED -> {
                    if (speedValue != null) "Рекомендуемая скорость $speedValue километров в час"
                    else "Рекомендуемая скорость"
                }
                SOFT_SHOULDER -> "Опасная обочина"
                BUMPY_ROAD -> "Неровная дорога"
                LOOSE_GRAVEL -> "Возможен выброс гравия"
                LOW_FLYING_AIRCRAFT -> "Низколетящие самолёты"
                FALLING_ROCKS -> "Возможен обвал камней"
                ROAD_NARROWS -> "Сужение дороги"
                DRAWBRIDGE -> "Разводной мост или паромная переправа"
                STEEP_HILL -> "Крутой спуск или подъём"
                TWO_WAY_WARNING -> "Начало двустороннего движения"
                TRAFFIC_LIGHT_WARNING -> "Впереди светофорное регулирование"
                ROUNDABOUT_WARNING -> "Впереди круговое движение"
                SPEED_LIMIT_ZONE -> "Зона с ограничением скорости"
                TOLL_ROAD -> "Платная автомобильная дорога"
                INTERSECTION_WARNING -> "Пересечение дорог"
                ONCOMING_PRIORITY -> "Преимущество встречного движения"
                PRIORITY_OVER_ONCOMING -> "Преимущество перед встречным движением"
                END_MAIN_ROAD -> "Конец главной дороги"
                NO_MOTOR_VEHICLES -> "Движение механических транспортных средств запрещено"
                NO_TRUCKS -> "Движение грузовых автомобилей запрещено"
                NO_MOTORCYCLES -> "Движение мотоциклов запрещено"
                NO_PEDESTRIANS -> "Движение пешеходов запрещено"
                VEHICLE_RESTRICTION -> "Ограничение габаритов транспортных средств"
                NO_DANGEROUS_GOODS -> "Движение с опасными грузами запрещено"
                END_NO_OVERTAKING -> "Конец зоны запрещения обгона"
                DIRECTION_MANDATORY -> "Направление движения"
                CARS_ONLY -> "Дорога только для легковых автомобилей"
                MINIMUM_SPEED -> "Минимальная скорость"
                PEDESTRIAN_CYCLE_PATH -> "Пешеходная или велосипедная дорожка"
                HORSE_RIDING -> "Дорожка для всадников"
                EXPRESSWAY -> "Дорога для автомобилей"
                CYCLE_CROSSING -> "Велосипедный переезд"
                ROAD_NUMBER -> "Номер дороги"
                KILOMETER_MARKER -> "Километровый знак"
                SETTLEMENT_SIGN -> "Населённый пункт"
                CAR_SERVICE -> "Техническое обслуживание автомобилей"
                CAR_WASH -> "Мойка автомобилей"
                TELEPHONE -> "Телефонная связь"
                DRINKING_WATER -> "Питьевая вода"
                REST_AREA -> "Место отдыха"
                LANDMARK -> "Достопримечательность"
                CHARGING_STATION -> "Электромобильная зарядная станция"
                ADDITIONAL_PLATE -> "Табличка дополнительной информации"
                UNKNOWN -> "Обнаружен дорожный знак"
                GENERAL_WARNING -> "Предупреждающий дорожный знак"
            }
        }
    }
}
