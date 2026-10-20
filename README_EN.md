# NutriTracker

[简体中文](README.md) | English

A native Android nutrition and calorie tracking app powered by AI food photo recognition and quick text logging, helping users manage daily meals, physical activities, and health goals.

## Screenshots

<div align="center">
<img src="images/1%20(1).jpg" alt="Home" width="240">&nbsp;&nbsp;
<img src="images/1%20(2).jpg" alt="Diary" width="240">&nbsp;&nbsp;
<img src="images/1%20(3).jpg" alt="Analytics" width="240">
<br><br>
<img src="images/1%20(4).jpg" alt="Profile" width="240">&nbsp;&nbsp;
<img src="images/1%20(5).jpg" alt="Settings" width="240">
</div>

## Features

**AI Recognition & Food Logging**
- Camera capture and gallery selection with free-form image cropping and multi-photo concurrent analysis
- Quick AI text logging ("one-sentence meal log") plus an interactive result review dialog for portion and sub-item editing
- Food library with favorites/archiving, multi-ingredient custom recipes, whole-portion entry, and quick calorie logging

**Nutrition Tracking & Analytics**
- Personalized daily calorie target via IOM 2005 TDEE equations with target-weight tapering
- Four meal sections (Breakfast, Lunch, Dinner, Snacks) and cross-day food diary management
- Macronutrient tracking plus micronutrient monitoring (fiber, sugars, saturated fat, sodium, cholesterol) with IOM DRI panels
- 7-day / 30-day calorie intake charts, average macro ratio donut chart, and streak counter

**Health Data, Widget & Personalization**
- 50+ MET-based physical activities, customizable water quick-add shortcuts, and hydration timeline details
- Weight history line chart with retroactive logging and dual BMI standards (WHO & China WGOC)
- Jetpack Glance home screen widget (one-tap `+250ml` water log and camera shortcut) & WorkManager meal/water reminders
- System / Light / Dark themes, Material You dynamic color, Imperial (`lbs`/`in`) & Energy (`kcal`/`kJ`) unit switching
- Encrypted API key storage via Android Keystore and atomic ZIP full backup/restore

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM (ViewModel + StateFlow + Flow) |
| Dependency Injection | Hilt 2.53.1 |
| Database | Room 2.6.1 |
| Navigation | Navigation Compose 2.8.5 |
| Camera | CameraX 1.4.1 |
| Networking & JSON | OkHttp 4.12.0 + Gson 2.11.0 |
| Image Loading | Coil 2.7.0 |
| Storage & Background | DataStore 1.1.1 · WorkManager 2.10.0 · Glance 1.1.1 |

## Requirements

- Latest stable Android Studio
- JDK 17+
- Android SDK 35 (`compileSdk`)
- Minimum Android version: Android 8.0 (API 26)

## Build

```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest

# Build Release APK (signing config required)
./gradlew assembleRelease
```

APK output format: `NutriTracker_v{versionName}_c{versionCode}_{buildType}_{date}.apk`

App versioning is centrally managed in [`version.properties`](version.properties).

## AI Configuration

The app does not bundle any built-in AI service. Configure your own provider in **Settings**:

- **API Key** - Required (encrypted locally via Android Keystore)
- **Base URL** - Any OpenAI-compatible `/chat/completions` endpoint
- **Model Name** - Multimodal vision model recommended for food photo analysis

Built-in connection testing in Settings verifies API reachability and multimodal capability.

## Project Structure

```text
app/src/main/java/com/example/nutritracker/
├── MainActivity.kt & RootViewModel.kt   # Entry point, global theme state & deep linking
├── data/
│   ├── AppDatabase.kt                   # Room database (9 tables, schema export & migrations)
│   ├── DataExportManager.kt             # Transactional ZIP full backup/restore
│   ├── entity/                          # Entities & MET physical activity catalog
│   ├── dao/                             # Reactive Flow DAO interfaces
│   └── repository/                      # Repositories & encrypted settings storage
├── di/                                  # Hilt dependency injection modules
├── feature/                             # Feature screens (home/diary/analytics/meal/camera/activity/profile/settings/onboarding/sources)
├── navigation/                          # Routes & 5-tab bottom navigation
├── ui/
│   ├── components/                      # Charts, cards, AI queue banner & shared dialogs
│   └── theme/                           # MD3 theming, dynamic colors & animations
├── util/                                # BMI/TDEE/DRI calculations, image utils & crypto
├── widget/                              # Jetpack Glance home screen widget
└── worker/                              # WorkManager meal & hydration reminders
```

## Database

| Table | Purpose |
|-------|---------|
| `user` | User profile & nutrition goals (single-user mode) |
| `meals` | Food templates, favorite/archived flags & AI sub-items |
| `intakes` | Logged meal intake entries |
| `recipes` | Custom multi-ingredient recipe definitions |
| `recipe_ingredients` | Recipe-to-ingredient gram mappings |
| `tracked_days` | Daily goal snapshots |
| `activities` | Physical activity burn records |
| `weight_logs` | Historical weight entries |
| `water_intakes` | Hydration intake logs |

## Permissions

- `INTERNET` - AI API network requests
- `CAMERA` - Camera capture for food recognition
- `POST_NOTIFICATIONS` - Scheduled meal & water reminder notifications (Android 13+)
- `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` (API ≤ 32) - Selecting food photos from gallery

## Acknowledgements

Inspired by and partially modeled after [OpenNutriTracker](https://github.com/simonoppowa/OpenNutriTracker), refactored and expanded as a lightweight native Android application.

## License

Open-source under the [GNU General Public License v3.0](LICENSE).
