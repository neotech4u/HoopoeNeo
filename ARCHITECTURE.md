# HOOPOE SYSTEM ARCHITECTURE & TECHNICAL SPECIFICATION

**System:** HOOPOE Automotive Companion  
**App Type:** Standalone Android Automotive Infotainment Environment  
**Package:** `com.hoopoe.automotive` (migrating from `com.hoopoe.launcher`)  
**Target Platform:** Android 15 (API 36) / Compatible back to Android 10 (API 29)  
**Hardware Profile:** Quad-Core ARM SoC, 2 GB RAM, 128 GB Flash, Landscape Automotive Touchscreen  
**Vehicle Integration Target:** Maruti Suzuki Dzire 2020 (RHD) with OEM 360° Camera & Steering Controls

---

## 1. Architectural Philosophy: The Standalone Infotainment Companion

HOOPOE provides an immersive, end-to-end digital cockpit experience without replacing the Android system launcher or modifying system partitions.

```
┌────────────────────────────────────────────────────────────────────────┐
│                      Android System Framework                          │
│   (Default OEM Launcher, System UI, Reverse Camera Service, Bluetooth)  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Non-invasive coexistence
┌───────────────────────────────────▼────────────────────────────────────┐
│                  HOOPOE STANDALONE COMPANION (App)                     │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                     Jetpack Compose UI Layer                   │   │
│   │  - 3-Zone Automotive HUD      - Navigation Centre              │   │
│   │  - Media Centre Control Deck   - Islamic Travel Companion       │   │
│   │  - Vehicle Diagnostics & TPMS  - Quick Controls & Voice Hub     │   │
│   │  - MDUS OS Branding Dock       - Assistant Ring (Alert Bus)     │   │
│   └────────────────────────────────┬───────────────────────────────┘   │
│                                    │ UI Events & State Observation     │
│   ┌────────────────────────────────▼───────────────────────────────┐   │
│   │               Feature ViewModels & Domain Use Cases            │   │
│   │  (DashboardViewModel, NavViewModel, MediaViewModel,            │   │
│   │   IslamicViewModel, ObdViewModel, AutomationViewModel)         │   │
│   └────────────────────────────────┬───────────────────────────────┘   │
│                                    │ Repository Contracts              │
│   ┌────────────────────────────────▼───────────────────────────────┐   │
│   │              Data Repositories & Hardware Adapters             │   │
│   │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │   │
│   │  │ Bluetooth OBD-II │  │ Prayer Times     │  │ MediaSession │  │   │
│   │  │ ELM327 Adapter   │  │ Calc Engine      │  │ Controller   │  │   │
│   │  └──────────────────┘  └──────────────────┘  └──────────────┘  │   │
│   │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │   │
│   │  │ Osmdroid GPS Map │  │ Fuel & Trip      │  │ Settings &   │  │   │
│   │  │ Offline Cache    │  │ Local Store      │  │ Theme Engine │  │   │
│   │  └──────────────────┘  └──────────────────┘  └──────────────┘  │   │
│   └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

### Standalone vs Launcher Operation Principles
1. **Manifest Registration:** Declares `<category android:name="android.intent.category.LAUNCHER" />` and omits `<category android:name="android.intent.category.HOME" />`.
2. **Coexistence with 360° Camera:** In reverse gear, the MCU switches video input or opens the factory camera application over the window. HOOPOE's Activity lifecycle enters `onPause()` or remains in the background cleanly, resuming without flicker when the camera closes.
3. **Graceful Exit Mechanism:** A dedicated "Exit to Car System" button launches the system home intent (`Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)`), instantly returning the driver to the OEM head-unit interface.
4. **Boot Option:** A `BootReceiver` listens for `ACTION_BOOT_COMPLETED` (if enabled in settings) to start HOOPOE automatically upon vehicle startup.

---

## 2. Modular Package Structure

The codebase is organized into clean, maintainable feature packages adhering to Clean Architecture principles:

```
com.hoopoe.automotive
│
├── core
│   ├── common               // Result wrappers, extensions, dispatchers
│   ├── theme                // Automotive color palettes, typography, theme engine
│   ├── location             // LocationCompassManager, GPS filter, NMEA provider
│   ├── network              // Weather API, OkHttp client, offline fallback
│   ├── storage              // DataStore preferences, Trip/Fuel store
│   └── audio                // Dua audio player, tactile SFX sound generator
│
├── domain
│   ├── model                // VehicleData, WeatherState, PrayerTimes, TripRecord
│   └── usecase              // CalculatePrayerTimesUseCase, ParseObdTelemetryUseCase
│
├── feature
│   ├── dashboard            // Main HUD screen, 3 layout presets, widget grid
│   ├── navigation           // Dedicated Navigation Centre screen, POI shortcuts
│   ├── media                // Fullscreen Media Centre, source switcher
│   ├── vehicle              // Vehicle telemetry, trip computer, fuel manager
│   ├── obd                  // OBD-II live diagnostic scanner, DTC trouble codes
│   ├── tpms                 // Tyre pressure and temperature monitor
│   ├── islamic              // Prayer times, Qibla compass, Mosques, Travel Dua
│   ├── voice                // Voice assistant parser, speech recognition, TTS
│   ├── quickcontrols        // Quick access control panel (brightness, Wi-Fi, etc.)
│   ├── automation           // Boot launch, Bluetooth auto-trigger settings
│   ├── theme                // Theme & styling customization screen
│   └── settings             // General preferences, vehicle name, compatibility audit
│
└── integration
    ├── obd                  // Bluetooth SPP ELM327 protocol driver
    ├── tpms                 // External TPMS sensor interface & mock provider
    ├── media                // MediaListenerService & NotificationListener
    ├── radio                // Hardware MCU broadcast bridge & session mirror
    └── headunit             // Vendor intent router (Navi, Dial, Radio, Camera, Settings)
```

---

## 3. UI/UX Automotive Design System

### 3.1 Color Themes & Surface Language
Automotive interfaces require deep black backgrounds to eliminate glare during night driving and maximize OLED/IPS contrast. HOOPOE provides 4 distinct automotive themes:

1. **Dark Automotive (Cyber Cyan):**
   - Background: Deep Obsidian `#07090E`
   - Surface / Card: `#0F141F` with `#243048` border
   - Primary Accent: Neon Electric Cyan `#00E5FF`
   - Text: High-contrast `#F8FAFC`
2. **Black & Champagne Gold (Luxury OEM):**
   - Background: Piano Black `#050505`
   - Surface / Card: Rich Graphite `#141416` with `#332B1E` border
   - Primary Accent: Champagne Gold `#D4AF37` / Bronze `#E5C158`
   - Text: Pearl White `#F5F5F0`
3. **Graphite Tech (Stealth Gray):**
   - Background: Dark Anthracite `#121417`
   - Surface / Card: Carbon Gray `#1E2229` with `#3A424E` border
   - Primary Accent: Titanium Silver `#E0E6ED` & Ice Blue `#64B5F6`
   - Text: Crisp White `#FFFFFF`
4. **Minimal Dashboard (Night Cruise):**
   - Background: Pure Zero-Light OLED Black `#000000`
   - Surface / Card: Translucent Outline `#0A0A0A`
   - Primary Accent: Minimal Warm White `#E2E8F0` / Muted Orange `#FF9100`
   - Text: `#CBD5E1`

### 3.2 Key UI Components Required by Specifications

1. **The 3 Dashboard Layout Options:**
   - **Preset 1 (3-Zone Cyber HUD):** Zone 1: Speedometer & Telemetry (Left); Zone 2: Embedded Navigation Road Canvas (Center); Zone 3: Media & Fuel Telemetry Cards (Right).
   - **Preset 2 (Minimal Drive HUD):** Ultra-clean large digital speed, minimalist clock, weather, next prayer time, and primary media card.
   - **Preset 3 (Cockpit Quad Grid):** 4 balanced widgets: Speedometer, Osmdroid Map, Media Player, and Live OBD/Trip Telemetry.
2. **Bottom Center MDUS OS Dock:**
   - Positioned cleanly at the lower-middle of the HUD.
   - Houses the Hoopoe vehicle logo emblem.
   - Features stylized high-tech text: **`MDUS OS • AUTOMOTIVE COMPANION`**.
   - Serves as an interactive touch point to reveal the Quick Controls panel.
3. **Interactive Assistant Ring (Alert Bus):**
   - Upgraded from `JarvisOrb.kt` into an intelligent automotive notification ring.
   - Continuously monitors:
     - Overspeed threshold alerts (e.g., speed > 80 km/h or user preset)
     - Prayer time alerts (within 15 minutes of Adhan)
     - Weather hazard / storm warnings
     - Low fuel & OBD warning indicators
   - Pulses with context-sensitive alert colors (Green = Normal, Amber = Upcoming Prayer/Weather, Red = Overspeed Warning).
   - Tap opens voice interaction or plays alert summary.

---

## 4. Hardware Abstraction Layer (HAL) & Graceful Fallbacks

```
                       ┌─────────────────────────┐
                       │  HOOPOE Hardware Layer  │
                       └────────────┬────────────┘
                                    │
           ┌────────────────────────┼────────────────────────┐
           │                        │                        │
┌──────────▼──────────┐  ┌──────────▼──────────┐  ┌──────────▼──────────┐
│  Bluetooth OBD-II   │  │   TPMS Hardware     │  │   Hardware Radio    │
├─────────────────────┤  ├─────────────────────┤  ├─────────────────────┤
│ 1. Check BT paired  │  │ 1. Check USB/BT Rx  │  │ 1. Check szchoiceway│
│    devices for ELM  │  │ 2. Read RF tyre IDs │  │    broadcast bridge │
│ 2. Open SPP socket  │  │ 3. If missing: show │  │ 2. Fallback: Mirror │
│ 3. If unavailable:  │  │    diagnostic gauge │  │    vendor app via   │
│    use GPS speed &  │  │    with clear status│  │    MediaSession     │
│    estimated fuel   │  │    badge            │  │ 3. 1-tap open radio │
└─────────────────────┘  └─────────────────────┘  └─────────────────────┘
```

1. **OBD-II Integration Layer:**
   - Standard ELM327 AT commands (`ATZ`, `ATE0`, `ATH0`, `ATSP0`).
   - Mode 01 PIDs: `010C` (RPM), `010D` (Vehicle Speed), `0105` (Coolant Temp), `012F` (Fuel Level Input), `0104` (Engine Load).
   - Mode 03: Request Diagnostic Trouble Codes (DTCs).
   - Mode 04: Clear Trouble Codes.
   - **Integrity Rule:** Zero fabricated data. If disconnected, status displays "OBD DISCONNECTED" and trip calculations fall back cleanly to GPS displacement.
2. **TPMS Integration Layer:**
   - 4-tyre display (Front Left, Front Right, Rear Left, Rear Right).
   - Pressure in PSI/BAR, Temperature in °C.
   - Unconnected state shows "Sensor Offline / Requires Receiver".
3. **Audio & Radio Integration Layer:**
   - NotificationListenerService tracks all system media sessions.
   - Hardware FM radio controls via MCU broadcasts or MediaSession skip commands.

---

## 5. Offline-First Islamic Travel Companion Engine

1. **Prayer Calculations:** Pure astronomical algorithm (Jean Meeus solar position formulae). Zero internet required.
   - Supports calculation conventions: Muslim World League (MWL), ISNA, Egyptian General Authority, Umm al-Qura (Makkah), University of Islamic Sciences (Karachi).
   - Asr juristic methods: Shafi'i/Standard (Shadow factor 1) and Hanafi (Shadow factor 2).
2. **Qibla Direction:** Great-circle azimuth formula calculated from current GPS latitude/longitude towards the Kaaba (21.4225° N, 39.8262° E).
3. **Travel Dua Audio:** Embedded audio player reading Travel Dua with Arabic voiceover / recitation, alongside English and Tamil transliterations.
4. **Mosque Discovery:** Offline search intent dispatching `"geo:0,0?q=mosque"` to installed navigation apps (Google Maps, OsmAnd, Petal Maps).

---

## 6. Resource & 2 GB RAM Optimization Strategy

1. **Recomposition Throttling:** Speedometer and clock updates are gated through `LinearOutSlowInEasing` and minute-boundary tickers to prevent Compose thread saturation.
2. **Bitmap Memory Clamping:** All remote/local artwork loaded through Coil is downsampled to `512×512` with hardware bitmap decoding enabled.
3. **MapView Tile Cache:** Osmdroid tile cache is restricted to 12 tiles in RAM with disk-backed caching on internal storage.
4. **Lifecycle Resource Disposal:** Sensor listeners (accelerometer, magnetometer) and GPS listeners are paused in `onStop()` when the Activity is covered by another app.
