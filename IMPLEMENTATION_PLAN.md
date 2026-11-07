# HOOPOE STEP-BY-STEP IMPLEMENTATION PLAN

**Target:** Execution blueprint for converting HOOPOE into a standalone automotive companion application.  
**Version:** 1.0.0  

---

## Phase 3: Core Navigation Shell & Screen Architecture

### Objective
Migrate the single-activity architecture to support all 13 required automotive screens, convert manifest from launcher to standalone application, establish persistent automotive navigation rail, and add the bottom MDUS OS branding dock and assistant ring.

### Tasks
1. **Manifest Refactor:**
   - Remove `<category android:name="android.intent.category.HOME" />`.
   - Retain `<category android:name="android.intent.category.LAUNCHER" />` and `<action android:name="android.intent.action.MAIN" />`.
   - Add permissions: `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `RECEIVE_BOOT_COMPLETED`, `RECORD_AUDIO`, `POST_NOTIFICATIONS`.
2. **NavDestination Expansion:**
   - Expand `NavDestination` enum from 3 destinations to all 13 functional destinations:
     1. `DASHBOARD` (Main Cockpit)
     2. `NAVIGATION_CENTRE` (Maps, search, POI shortcuts)
     3. `MEDIA_CENTRE` (Spotify, Radio, USB, Bluetooth)
     4. `VEHICLE_INFO` (Trip computer, battery, vitals)
     5. `OBD_DIAGNOSTICS` (Live ECU parameters, DTC scanner)
     6. `TPMS_DASHBOARD` (Tyre pressure & temperature)
     7. `ISLAMIC_COMPANION` (Prayer times, Qibla compass, Mosques, Travel Dua)
     8. `VOICE_ASSISTANT` (Automotive voice commands)
     9. `QUICK_CONTROLS` (Brightness, volume, Wi-Fi, BT, exit to OEM launcher)
     10. `AUTOMATION_SETTINGS` (Boot startup, Bluetooth auto-trigger)
     11. `THEME_CUSTOMIZATION` (4 Master themes, accent picker)
     12. `GENERAL_SETTINGS` (Vehicle name, units, fonts, layouts)
     13. `PERMISSIONS_COMPATIBILITY` (Hardware and permissions status)
     *(Plus `APP_LIBRARY` retained for app shortcuts and app launching)*
3. **Persistent Automotive Sidebar & MDUS OS Dock:**
   - Upgrade `Sidebar.kt` to accommodate automotive navigation icons with tooltips.
   - Embed the Bottom Dock in the layout: Hoopoe logo emblem + stylized tech text: **`MDUS OS • AUTOMOTIVE COMPANION`**.
   - Embed the upgraded Assistant Ring (`JarvisOrb.kt` with alert indicators for overspeed, prayer reminders, and weather warnings).

---

## Phase 4: Core Functionality & Dashboard Overhaul

### Objective
Implement the 3 dashboard layout options, prayer timing widget, fuel/refuel logging widget, and core HUD widgets.

### Tasks
1. **3 Dashboard Layout Presets:**
   - **Preset 1 (3-Zone Cyber HUD):** Speedometer/RPM cluster (Left), Vector Navigation Map (Center), Media Player & Fuel Telemetry Cards (Right).
   - **Preset 2 (Minimal Dashboard):** High-readability large digital speedometer, clock, weather, next prayer countdown, and compact media card.
   - **Preset 3 (Cockpit Quad Grid):** 4 balanced instrument panes (Speedometer, Osmdroid Map, Media Player, Diagnostics).
2. **Dashboard Widgets:**
   - Digital Clock (minute-boundary efficiency).
   - Speedometer Widget with KM/H and MPH toggle, peak speed, neon wave graph.
   - Prayer Timing Widget with countdown to next prayer.
   - Fuel Hub & Refuel Modal for tracking petrol logs and odometer KM.
   - Weather Widget with offline state resilience.
3. **Theme Engine:**
   - 4 master automotive palettes: Dark Automotive (Cyan), Black & Champagne Gold, Graphite Tech, Minimal Dashboard.

---

## Phase 5: Hardware Integrations & Graceful Fallbacks

### Objective
Implement real Bluetooth ELM327 OBD-II communication layer, TPMS dashboard, and hardware radio bridge.

### Tasks
1. **Bluetooth OBD-II Integration (`core/obd`):**
   - RFCOMM SPP socket manager for ELM327 adapters.
   - Query PIDs: `010C` (RPM), `010D` (Speed), `0105` (Coolant Temp), `012F` (Fuel Level), `0104` (Engine Load).
   - Mode 03 DTC Trouble Code reader and Mode 04 clear codes request.
   - Offline detection: When no adapter is paired or connected, show clean "OBD-II Hardware Disconnected" status card without crashing, falling back to GPS telemetry.
2. **TPMS Integration (`core/tpms`):**
   - 4-wheel visual car diagram with pressure (PSI/BAR) and temperature (°C).
   - Warning thresholds and sensor pairing diagnostic instructions.
3. **Hardware Radio Integration (`core/radio`):**
   - Support szchoiceway MCU broadcast commands (`com.szchoiceway.eventcenter`).
   - Mirror vendor radio app via `MediaSessionManager` for non-MCU head units.

---

## Phase 6: Dedicated Modules & Advanced Features

### Objective
Implement the Islamic Travel Companion, Navigation Centre, Media Centre, Voice Assistant, and Smart Automation engine.

### Tasks
1. **Islamic Travel Companion:**
   - Astronomical offline prayer calculation algorithm (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha).
   - 5 recognized calculation methods (MWL, ISNA, Egypt, Umm al-Qura, Karachi) and Shafi'i/Hanafi Asr.
   - Qibla Direction Great-Circle calculation and animated compass.
   - Nearby Mosque discovery shortcut (`geo:0,0?q=mosque`).
   - Travel Dua modal with Arabic, transliteration, English and Tamil translations, plus audio playback.
2. **Dedicated Navigation Centre:**
   - Embedded interactive Osmdroid map canvas.
   - One-touch routing shortcuts for Petrol Stations, Mosques, Hospitals, Restaurants, Home, and Work.
   - Navigation app selector (Google Maps, OsmAnd, Waze).
3. **Dedicated Media Centre:**
   - Fullscreen player deck with source switcher (Spotify, Radio, USB, Bluetooth).
4. **Voice Assistant:**
   - Push-to-talk speech recognition via Assistant Ring.
   - Automotive command processor: navigation, media, weather, and prayer times.
   - Text-to-Speech (TTS) spoken responses.
5. **Smart Automation Engine:**
   - Boot completed receiver (`BootReceiver`) to optionally launch HOOPOE on device power-on.
   - Bluetooth connection receiver to trigger media resumption.
   - Overspeed warning alerts via the Assistant Ring.
6. **Quick Controls Panel:**
   - Window brightness slider, volume controls, quick settings shortcuts, and "Exit to Android System Launcher" button.

---

## Phase 7: Testing, Optimization & Verification

### Tasks
1. **2 GB RAM Footprint Verification:** Ensure bitmap clamping, minute-boundary clock updates, and map tile cache restrictions prevent OOM errors.
2. **Lifecycle & Reverse Camera Verification:** Verify Activity pauses and releases hardware decoder/sensor handles when covered by camera or third-party apps.
3. **Permission Handling:** Verify the app remains completely operational with graceful fallbacks when optional permissions (Bluetooth, Microphone, Notifications) are denied.

---

## Phase 8: Release Documentation & User Guide

### Tasks
1. Complete `README.md` update.
2. Create `USER_GUIDE.md` and `KNOWN_LIMITATIONS.md`.
3. Provide step-by-step installation instructions for the JGL Android 15 head unit via USB drive or ADB.
