# Znaki-PDD (RoadSignAI)

**Детектор дорожных знаков** — Android-приложение на Jetpack Compose для обнаружения и распознавания дорожных знаков через камеру смартфона в реальном времени, с озвучиванием и расчётом зон действия по ПДД РФ.

## Возможности

- 📷 **CameraX** — захват видео с задней камеры, Preview + ImageAnalysis
- 🧠 **ML Kit Object Detection** — детекция дорожных знаков
- 👁️ **ML Kit Text Recognition (русский)** — распознавание текста на знаках
- 🗣️ **TTS озвучивание** — голосовые подсказки на русском языке с трёхуровневым приоритетом
- 📍 **GPS-трекинг** — привязка знаков к местоположению
- 🚧 **Зоны действия** — расчёт зон согласно ПДД РФ (3.24, 3.27, 3.28-3.30, 3.20)
- ⛔ **Детекция остановки в зоне запрета** — визуальное + звуковое + вибро-предупреждение
- 🗺️ **Карта зон** — отображение активных зон (OSMDroid)
- 🌙 **Тёмная тема** — оптимизирована для использования ночью
- ⚙️ **Гибкие настройки** — пороги уверенности, зоны, TTS, логирование

## Стек технологий

| Компонент | Технология |
|-----------|-----------|
| Язык | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Архитектура | MVVM + Clean Architecture (data/domain/presentation) |
| DI | Hilt (KSP) |
| Камера | CameraX 1.3 |
| ML | ML Kit Object Detection + Text Recognition |
| TTS | Android TextToSpeech (ru_RU) |
| GPS | Fused Location Provider |
| Карты | OSMDroid |
| БД | Room |
| Настройки | DataStore Preferences |
| Навигация | Jetpack Navigation Compose |
| Асинхронность | Kotlin Coroutines + Flow |

## Структура проекта

```
app/src/main/java/com/roadsignai/
├── RoadSignAIApp.kt              # Application + Hilt
├── MainActivity.kt                # Single Activity + NavHost
├── di/                            # Hilt модули
│   ├── CameraModule.kt
│   ├── MLModule.kt
│   ├── LocationModule.kt
│   ├── DatabaseModule.kt
│   └── TTSModule.kt
├── data/
│   ├── local/
│   │   ├── db/                    # Room: SignEntity, SignDao, AppDatabase
│   │   ├── preferences/           # DataStore: настройки
│   │   └── TaskUtils.kt           # Google Tasks → coroutines
│   ├── location/                  # LocationTrackingService
│   └── repository/                # SignRepositoryImpl
├── domain/
│   ├── model/                     # RoadSign, SignZone, VehicleState
│   ├── usecase/                   # DetectSigns, CalculateZone, CheckStop, SpeakSign
│   └── repository/                # SignRepository interface
└── presentation/
    ├── camera/                    # CameraScreen + CameraViewModel
    ├── components/                # SignOverlay, SignCard, ZoneMap, WarningBanner
    ├── settings/                  # SettingsScreen + SettingsViewModel
    └── theme/                     # Color, Type, Theme (Material 3 dark scheme)
```

## Сборка

### Требования

- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17
- Android SDK 34

### Инструкция

```bash
# 1. Установите Gradle Wrapper (если отсутствует)
cd Znaki-PDD
gradle wrapper --gradle-version 8.7

# 2. Сборка debug APK
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew assembleDebug

# 3. APK будет в app/build/outputs/apk/debug/
```

### Примечание по ML Kit

Приложение использует ML Kit Object Detection со встроенной базовой моделью. Для детекции конкретных категорий дорожных знаков (ограничение скорости, стоп, и т.д.) ML Kit использует встроенную классификацию объектов. Для production-сценария рекомендуется обучить кастомную TFLite-модель на датасете GTSRB и разместить её в `app/src/main/assets/model/sign_detection.tflite`.

## Разрешения

Приложение запрашивает:
- `CAMERA` — для детекции знаков
- `ACCESS_FINE_LOCATION` — для GPS-трекинга зон
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` — для работы в фоне (Android 10+)
- `POST_NOTIFICATIONS` — для уведомлений (Android 13+)
- `VIBRATE` — для виброотклика

## Тестирование

```bash
# Unit тесты
./gradlew test

# Инструментальные тесты
./gradlew connectedAndroidTest
```

## Лицензия

MIT
