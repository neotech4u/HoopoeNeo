# PROJECT AUDIT: HOOPOE AUTOMOTIVE COMPANION

**Document Version:** 1.0.0  
**Target Device:** JGL Aftermarket Android Head Unit (Reported OS: Android 15, 2 GB RAM, 128 GB Storage, Landscape Touchscreen)  
**Target Vehicle:** Maruti Suzuki Dzire 2020 (RHD) with factory 360° camera, Bluetooth, GPS  
**Base Repository Reference:** `https://github.com/neotech4u/hoopoe` (v0.0.5)

---

## 1. Executive Summary & Directional Pivot

The original repository was conceived and partially implemented as an Android Home/Launcher replacement (`com.hoopoe.launcher` declaring `android.intent.category.HOME`). On aftermarket Android head units, replacing the OEM home launcher presents severe risks:
1. **System Instability:** OEM custom services, MCU event bridges, CANbus handlers, and factory settings hooks are often tied to the factory launcher.
2. **Camera Collision:** OEM 360° camera displays and reverse gear interrupts rely on factory launcher overlay permissions and proprietary display pipelines.
3. **Loss of OEM Functions:** Proprietary Bluetooth phone dialers, vehicle settings, and audio DSP controllers become inaccessible or crash.

### Core Strategic Shift
**HOOPOE is transitioning from a replacement launcher into a high-performance standalone automotive companion application (`com.hoopoe.automotive`).**
- **Non-destructive:** It does not declare `category.HOME`. The OEM launcher remains the default home screen.
- **Dedicated Automotive Cockpit:** Operates as a rich standalone immersive application launched manually or via optional boot completion.
- **Safe Exit:** At any moment, driver can tap "Exit to Car System" or the physical Home button to return to the factory launcher.
- **Preserved Value:** Reuses the high-value Jetpack Compose widgets, tactile UI controls, audio decoders, and Islamic companion assets from the previous codebase.

---

## 2. Technical Stack & Build Configuration Audit

| Dimension | Existing Value | Audit & Assessment |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.2.10 | Modern, idiomatic Kotlin coroutines and Flows. |
| **UI Framework** | Jetpack Compose (BOM `2024.09.00`) | Hardware-accelerated declarative UI; efficient when recompositions are controlled. |
| **Gradle Plugins** | Android Application `9.1.1`, Compose Plugin `2.2.10` | Uses modern AGP and Compose compiler plugin. |
| **SDK Levels** | `minSdk: 21`, `targetSdk: 36`, `compileSdk: 36` (Android 15+) | Must account for Android 15 foreground service policies, media session restrictions, and background launch gates. |
| **Data Storage** | Jetpack DataStore Preferences `1.1.7` & SharedPreferences | Fast, asynchronous, resilient against corrupted preferences. |
| **Networking** | Retrofit 2.12.0 + Gson 2.13.1 | Clean Open-Meteo REST client. Needs offline timeout guards. |
| **Image Loading** | Coil Compose 2.7.0 | Used for album art and custom wallpapers with bitmap clamping. |
| **Map Engine** | Osmdroid Android 6.1.20 | Offline-capable OpenStreetMap engine with vehicle tracking and dark mode inversion matrix. |
| **Architecture** | Single-Activity MVVM (`LauncherViewModel`) | Monolithic ViewModel (`LauncherViewModel` has 811 lines). Needs refactoring into modular Clean Architecture / feature ViewModels. |

---

## 3. Inventory of Existing Components & Screens

### 3.1 Screens & Dialogs
1. **`MainActivity.kt`**: Single Activity managing window insets (immersive fullscreen), settings state, theme providers, screen switches, and lifecycle events.
2. **`HomeScreen.kt`**: Multi-zone interactive HUD. Contains Top Status Rail (drive profiles ECO/SPORT/PILOT, vehicle badge, OBD link indicator, turn blinkers, Jarvis orb, weather pill) and the drag-and-drop 3×2 widget grid.
3. **`LogonScreen.kt`**: Cold-boot video player displaying `hoopoe_logon.mp4` with tap-to-skip and safe decoder cleanup.
4. **`TravelDuaScreen.kt`**: Muslim travel prayer modal with Arabic, transliteration, English and Tamil translations, plus "Fi Aman Allah / Happy Journey" card.
5. **`OnboardingScreen.kt`**: Initial setup wizard for vehicle name, measurement units, and permissions.
6. **`SettingsScreen.kt`**: Extensive configuration list for colors, wallpaper, layout grids, font, and system permissions.
7. **`AppLibraryScreen.kt`**: Full application drawer showing installed OEM and user apps, with search and shortcut assignment modes.
8. **`RefuelDialog.kt`**: Fuel log input dialog (liters, cost/L, odometer, full-tank switch).
9. **`ColorPickerDialog.kt` & `ConfirmDialog.kt`**: Reusable automotive dialog modals.
10. **`JarvisOrb.kt`**: Animated multi-ring arc reactor canvas with voice search trigger.

### 3.2 Dashboard Widgets
- **`SpeedometerWidget.kt`**: Dual-ring GPS speedometer with KM/H and MPH toggle, live neon speed wave graph, P-R-N-D gear selector, and RPM estimator.
- **`NavigationMapWidget.kt`**: Embedded Osmdroid live map tile canvas, GPS vehicle heading pointer, zoom controls, and 1-tap launcher for Google Maps / OsmAnd.
- **`NowPlayingWidget.kt`**: MediaSession reader with play/pause/skip, clamped album art, frequency spectrum visualizer, and hardware radio tuner mirror.
- **`FuelHubWidget.kt`**: Dual-mode widget showing Live Trip telemetry (distance, economy, cost) and fuel tank level.
- **`TripTrackerWidget.kt`**: Odometer, elapsed time, average speed, and 0–100 km/h acceleration timer.
- **`ClockWidget.kt`**: Digital and analog time displays with minute-boundary update intervals.
- **`WeatherWidget.kt`**: Temperature, wind speed, weather condition icons via Open-Meteo API.
- **`TelemetryWidget.kt` & `AltimeterWidget.kt`**: GPS-derived compass bearing, altitude, pitch and roll.
- **`VitalsWidget.kt`**: System CPU, RAM, and thermal diagnostic gauges.
- **`SoundboardWidget.kt`**: 6 assignable tactile audio pads (SFX Mario jump, coin, boom, etc.).

---

## 4. Gap Analysis: Implemented vs. Missing vs. Proposed

| Requirement Module | Existing Repo State | Gap / Missing Elements | Proposed Action |
| :--- | :--- | :--- | :--- |
| **MOD 1: Automotive Dashboard** | Partial (3×2 grid + 3-Zone HUD) | Missing 3 distinct layout presets (3-Zone, Minimal, Cockpit Quad). Missing prayer timing widget on dashboard. Missing bottom-center logo space with "MDUS OS" text. | Implement layout switcher with 3 presets; add prayer widget to grid; add logo + "MDUS OS" branding dock. |
| **MOD 2: Navigation Centre** | Partial (`NavigationMapWidget` only) | No dedicated Navigation Centre screen. Missing destination shortcuts (Home, Work, Petrol, Hospital, Mosque, Food), one-touch routing, and app selector. | Build dedicated `NavigationScreen` with POI search intent shortcuts, recent routes, and split-screen support. |
| **MOD 3: Media Centre** | Partial (`NowPlayingWidget` + `MediaListenerService`) | No dedicated full Media Centre screen. Lacks direct player switching (Spotify, VLC, Radio, Bluetooth Audio, Local USB). | Build dedicated `MediaScreen` with audio source tabs, playlist/track browsing intent shortcuts, and media session control. |
| **MOD 4: Vehicle & Diagnostics** | Partial (`FuelHubWidget` + simulated OBD state) | No real Bluetooth OBD-II ELM327 protocol engine. No DTC scanner. No TPMS dashboard. Fabricated OBD status toggle in existing repository. | Build real OBD-II communication layer (Bluetooth SPP ELM327 parser) with graceful offline fallback; build TPMS dashboard; strictly distinguish GPS vs ECU data. |
| **MOD 5: Islamic Companion** | Minimal (`TravelDuaScreen` only) | No prayer calculation engine (Fajr, Dhuhr, Asr, Maghrib, Isha). No Qibla compass. No nearby mosque finder. No audio playback for Dua. | Implement offline astronomical prayer times engine (MWL, ISNA, Egypt, Makkah, Karachi methods); build Qibla compass screen; add Mosque navigation shortcut; add Dua audio player. |
| **MOD 6: Smart Automation** | None | No startup boot receiver. No Bluetooth connect/disconnect automation trigger. No speed alert triggers. | Implement `BootReceiver`, `BluetoothReceiver`, notification alert engine for overspeed, weather warning, and prayer calls. |
| **MOD 7: Voice Assistant** | Partial (Orb triggers Google Assistant) | No in-app voice command processor. No spoken responses for weather or prayer times. | Implement modular Voice Command Parser with Android `SpeechRecognizer` and Text-To-Speech (TTS) engine. |
| **MOD 8: Quick Controls** | None | No quick settings drawer or overlay panel. | Build automotive Quick Controls panel (Display brightness, Wi-Fi, BT, Volume, System Settings shortcut, Day/Night toggle). |
| **MOD 9: Theme Engine** | Basic (Dark / Day / Custom Color) | Missing predefined premium themes: Dark Automotive, Black & Champagne Gold, Graphite, Minimal. | Implement Theme Engine with 4 distinct automotive palettes and typography presets. |
| **MOD 10: Reverse Camera & 360°** | Basic (Launches camera app via intent) | No safeguard against interfering with 360° camera system. | Document zero-interference policy; ensure standalone app yields display instantly on reverse gear trigger; verify camera launch intents. |

---

## 5. Hardware-Dependent Features & Android 15 Restrictions

### 5.1 Hardware Dependencies
1. **Bluetooth OBD-II (ELM327):**
   - Requires Bluetooth Classic SPP (`00001101-0000-1000-8000-00805F9B34FB`).
   - Many cheap head units disable Bluetooth SPP in Android userspace and route it exclusively to an MCU phone dialer.
   - **Fallback Architecture:** When SPP is not exposed or no adapter is paired, HOOPOE shows an informative "OBD-II Hardware Disconnected" status card and seamlessly uses GPS telemetry for speed, trip distance, and acceleration metrics without crashing.
2. **TPMS (Tyre Pressure Monitoring System):**
   - External USB/Bluetooth TPMS modules require vendor protocols.
   - **Fallback:** TPMS screen displays clear hardware status with calibration guides and simulated preview mode for diagnostics.
3. **FM/AM Hardware Radio:**
   - Standard Android has no public FM tuner API. The previous repo cleverly supported szchoiceway MCU broadcasts and generic MediaSession mirroring. We preserve this dual-backend approach.
4. **Ignition & Reverse Camera:**
   - Handled directly by MCU/CANbus hardware lines. HOOPOE must remain completely passive during reverse gear activation.

### 5.2 Android 15 / API 36 Platform Constraints
1. **Background Activity Starts:** Android 10+ and especially Android 15 strictly restrict launching activities from background services. The boot completion receiver must notify or launch via standard permitted intents.
2. **Notification Listener Service:** Requires explicit user grant in Android Settings. A fallback must exist for media display if permission is not granted.
3. **Location Updates:** On Android 14/15, background location requires specific foreground service types (`foregroundServiceType="location"`). For HOOPOE, running location in the foreground Activity while active is optimal and prevents memory exhaustion.
4. **Package Visibility:** Android 11+ requires `<queries>` in `AndroidManifest.xml` to discover installed navigation and media applications.

---

## 6. Detailed Reusability Matrix

| Source File | Status | Action & Justification |
| :--- | :--- | :--- |
| `res/raw/hoopoe_logon.mp4` | Reusable (100%) | Essential branding video for the cold boot sequence. |
| `ui/screen/LogonScreen.kt` | Reusable (95%) | Solid hardware-accelerated video playback with error guards. |
| `ui/screen/TravelDuaScreen.kt` | Reusable (90%) | High quality Arabic/Tamil/English dua text. Needs audio playback integration. |
| `ui/components/JarvisOrb.kt` | Reusable (85%) | Beautiful holographic ring canvas. Needs notification badge and alert state indicators. |
| `ui/widget/SpeedometerWidget.kt` | Reusable (90%) | Excellent neon telemetry wave and dual metric display. |
| `ui/widget/NavigationMapWidget.kt`| Reusable (90%) | Osmdroid map with low-RAM optimizations. |
| `ui/widget/NowPlayingWidget.kt` | Reusable (85%) | MediaSession controls and MCU radio bridge. |
| `ui/widget/FuelHubWidget.kt` | Reusable (90%) | Live trip cost and fuel logging. |
| `ui/components/RefuelDialog.kt` | Reusable (100%)| Complete petrol and kilometer log input. |
| `data/TripFuelStore.kt` | Reusable (80%) | Working refuel record repository. Needs Room database migration for long-term trips. |
| `util/LocationCompassManager.kt`| Reusable (90%) | Low-pass filter compass and GPS speed calculations. |
| `AndroidManifest.xml` | Modify | Remove `category.HOME`; add standalone app categories, boot receiver, and Bluetooth permissions. |
| `MainActivity.kt` & `LauncherViewModel.kt` | Refactor | Modularize into feature modules; add screen router for the 13 required screens. |

---

## 7. Migration Sequence

1. **Phase 1 (Completed):** Comprehensive audit report (`PROJECT_AUDIT.md`).
2. **Phase 2:** Technical Architecture specification (`ARCHITECTURE.md`), Feature Roadmap (`FEATURE_ROADMAP.md`), Hardware Compatibility Matrix (`DEVICE_COMPATIBILITY.md`), and Implementation Plan (`IMPLEMENTATION_PLAN.md`).
3. **Phase 3:** Core Shell & Navigation Re-architecture (Standalone App conversion, 13 screen destinations, persistent automotive navigation rail, bottom dock with logo and "MDUS OS" branding).
4. **Phase 4:** Dashboard Overhaul & Widgets (3 Layout presets, Prayer widget, Refuel/Trip widget, Notification-integrated Jarvis Assistant Ring).
5. **Phase 5:** Dedicated Automotive Modules (Navigation Centre, Media Centre, Islamic Travel Companion with offline calculations, Qibla compass & Dua audio).
6. **Phase 6:** Vehicle Diagnostics & Hardware Integration (Real Bluetooth ELM327 OBD-II engine, TPMS dashboard, graceful offline fallbacks).
7. **Phase 7:** Automation, Voice & Settings (Boot receiver, Bluetooth automations, Voice Command processor, Quick Controls panel, 4-theme engine).
8. **Phase 8:** Testing, Verification, Optimization (2 GB RAM optimization, leak checks, build scripts, user documentation).
