# HOOPOE FEATURE ROADMAP & MODULE SPECIFICATIONS

**Target:** Complete Feature Inventory & Milestone Plan for Standalone Automotive Companion  
**Version:** 1.0.0  

---

## Module Inventory & Implementation Matrix

### MODULE 1 – Automotive Dashboard
- **Digital Clock & Date:** Minute-boundary precision, 12h/24h formats, date and day readout.
- **GPS Speedometer:** Filtered speed with KM/H and MPH toggle, peak speed log, neon waveform graph, P-R-N-D gear selector, simulated RPM gauge.
- **Dynamic Weather:** Open-Meteo REST service integration, temperature, weather conditions, wind speed, offline state handling.
- **Prayer Timing Widget:** Real-time countdown to next prayer (Fajr, Dhuhr, Asr, Maghrib, Isha), current prayer indicator.
- **Media Player Card:** Track metadata, album artwork, play/pause, next/previous controls, audio spectrum visualizer, FM radio toggle.
- **Fuel & KM Log Widget:** Refuel logging modal, tank capacity tracker, live trip fuel consumption, cost calculator.
- **Interactive Assistant Ring:** Notification-aware animated arc-reactor pulsing with alerts (overspeed, prayer reminders, weather warnings).
- **Branding Dock:** Bottom-center logo emblem with tech-styled "MDUS OS" inscription.
- **3 Configurable Layout Presets:**
  1. *3-Zone Cyber HUD* (Speedometer, Navigation Map, Media/Fuel Hub).
  2. *Minimal Dashboard* (Clean large speed, clock, prayer, media).
  3. *Cockpit Quad Grid* (Speedometer, Map, Media, Diagnostics).

### MODULE 2 – Navigation Centre
- **Live Interactive Map:** Embedded Osmdroid offline-capable OpenStreetMap with vehicle rotation.
- **One-Touch Navigation Routing:** Intent-based routing to Google Maps, OsmAnd, Waze, and Sygic.
- **Category Shortcuts:** One-touch search for:
  - Nearby Petrol Stations (`geo:0,0?q=petrol+station`)
  - Nearby Mosques (`geo:0,0?q=mosque`)
  - Nearby Hospitals (`geo:0,0?q=hospital`)
  - Nearby Restaurants (`geo:0,0?q=restaurant`)
- **Saved Favorites:** Home and Work destination shortcuts with configurable addresses.
- **Split-Screen Support:** Intent flags (`FLAG_ACTIVITY_LAUNCH_ADJACENT`) for supported Android multi-window environments.

### MODULE 3 – Media Centre
- **Multi-Player Support:** Integrates with Spotify, YouTube Music, VLC, local USB audio, and Bluetooth audio via `MediaSessionManager`.
- **Playback Controls:** Play, pause, skip forward, skip backward, scrub slider.
- **Album Art:** High-fidelity artwork with automatic contrast background glow.
- **Source Selection Drawer:** Switch between streaming apps, local storage player, and FM/AM radio.
- **Hardware Radio Deck:** Dual backend (szchoiceway MCU broadcast commands and universal MediaSession controller).

### MODULE 4 – Vehicle Information & Diagnostics
- **Live ECU Diagnostics (OBD-II):**
  - Bluetooth SPP connection to ELM327 adapters (`00001101-0000-1000-8000-00805F9B34FB`).
  - Real-time PIDs: Engine RPM, Vehicle Speed, Coolant Temp, Throttle Position, Fuel Level, Battery Voltage.
  - Diagnostic Trouble Codes (DTCs): Read codes, code descriptions, and clear codes request.
  - Connection Monitor: Automatic reconnect on Bluetooth drop; clean offline fallback banner.
- **TPMS Monitor:**
  - 4-wheel visual car diagram (FL, FR, RL, RR).
  - Real-time pressure in PSI/BAR and temperature in °C.
  - Visual warnings for under-inflation (< 28 PSI) or over-inflation (> 40 PSI).
- **Trip Telemetry Computer:**
  - Rolling trip distance, elapsed trip time, idle time, estimated average speed, 0–100 km/h acceleration timer.
  - Strict distinction: GPS-derived telemetry vs. OBD-II ECU telemetry.

### MODULE 5 – Islamic Travel Companion
- **Astronomical Prayer Times Calculation:**
  - 5 Daily Prayers: Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha.
  - Calculation Methods: Muslim World League, ISNA, Egypt General Authority, Umm Al-Qura (Makkah), Karachi.
  - Asr Juristic Methods: Shafi'i/Standard vs. Hanafi.
  - Next prayer countdown clock with audio Adhan/notification reminder option.
- **Qibla Direction & Compass:**
  - Great-circle navigation bearing from GPS coordinates to the Kaaba.
  - Real-time compass dial using low-pass filtered magnetometer data.
  - Calibration guidance for vehicle sensor interference.
- **Nearby Mosques:** One-tap GPS search for mosques along the route.
- **Travel Dua Modal & Audio Player:**
  - Arabic script, transliteration, English and Tamil translations.
  - Audio recitation playback with play/pause and repeat.
  - Option to show upon application startup with "Fi Aman Allah" journey completion banner.

### MODULE 6 – Smart Automation
- **Boot Startup Automation:** Optional `RECEIVE_BOOT_COMPLETED` receiver to launch HOOPOE on device power-on.
- **Bluetooth Trigger:** Automatically resume media or open dashboard when head unit connects to vehicle Bluetooth.
- **Speed Warning Automation:** Visual and audio alert via Assistant Ring when speed exceeds user threshold.
- **Automated Day/Night Switching:** Time-based, GPS solar-elevation based, or system-synchronized dark mode.

### MODULE 7 – Voice Assistant
- **Modular Speech Recognition:** Integrates with Android `SpeechRecognizer` and voice intents.
- **Automotive Commands:**
  - "Open navigation" / "Navigate home"
  - "Play music" / "Pause" / "Next track"
  - "What's the weather?"
  - "Next prayer time"
  - "Open diagnostics"
- **Text-to-Speech (TTS):** Spoken feedback for weather, prayer countdown, and vehicle diagnostics.
- **Privacy:** Push-to-talk by default via Assistant Ring; no unauthorized background microphone listening.

### MODULE 8 – Quick Controls Panel
- **Slide-out / Overlay Control Bar:**
  - Screen brightness slider (via Android window brightness attributes).
  - Quick Wi-Fi toggle / settings shortcut.
  - Quick Bluetooth toggle / settings shortcut.
  - Volume control slider.
  - Audio output / Mute toggle.
  - Exit to Android System Launcher button.
  - Open Android System Settings button.

### MODULE 9 – Customization & Theme Engine
- **4 Master Themes:**
  1. *Dark Automotive* (Obsidian / Neon Cyan)
  2. *Black & Champagne Gold* (Piano Black / Champagne Gold Accent)
  3. *Graphite Tech* (Carbon Gray / Titanium Silver)
  4. *Minimal Dashboard* (Zero-Light Black / Minimal White & Amber)
- **Widget Customizer:** Add, remove, rearrange, and resize widgets across the 3×2 grid.
- **Shortcut Editor:** 5 assignable quick shortcuts with custom vector icons.
- **Vehicle Profile:** Custom vehicle name (e.g. `DZIRE`), custom car icon, metric/imperial unit switch.

### MODULE 10 – Reverse Camera & OEM Coexistence
- **Passive Coexistence Principle:** Zero interference with OEM 360° camera hardware and video overlays.
- **Lifecycle Cleanliness:** HOOPOE yields the screen instantaneously when the reverse camera is triggered by the gear switch.
- **Camera Shortcut:** Provides intent-based launch to the factory camera / AVIN application when manually requested.

---

## 2. Implementation Milestones

| Milestone | Deliverables | Verification Criteria |
| :--- | :--- | :--- |
| **M1: Architecture & Manifest** | Manifest refactored to standalone app (`category.LAUNCHER`); 13 navigation destinations declared; Core Clean Architecture layout. | App installs and runs alongside default launcher without hijacking HOME. |
| **M2: UI Overhaul & Navigation Shell** | Persistent automotive navigation bar; 13 screens created; MDUS OS dock with logo; Assistant Ring alert bus. | All 13 screens navigable with fluid transitions. |
| **M3: Dashboard & Widgets** | 3 Dashboard layout presets; Prayer widget; Refuel & KM log widget; Speedometer with peak speed; Weather widget. | Layouts switch instantly; widgets reflect live sensor and time data. |
| **M4: Islamic Travel Companion** | Astronomical prayer calculation engine; Qibla compass; Mosque navigation; Travel Dua audio playback. | Accurate prayer times calculated offline for current GPS coords. |
| **M5: Navigation & Media Centres** | Fullscreen Navigation Centre with POI shortcuts; Fullscreen Media Centre with player switching. | Intent routing opens Google Maps/Waze; media controls drive active player. |
| **M6: Vehicle Diagnostics & OBD** | Bluetooth SPP ELM327 manager; DTC reader; TPMS dashboard; graceful offline status. | Graceful fallback when OBD/TPMS hardware is not connected. |
| **M7: Automation, Voice & Themes** | Boot receiver; Speech command engine; Quick Controls panel; 4-theme engine. | Voice commands recognize queries; themes switch accents and surfaces. |
| **M8: Polish, Packaging & Release** | Memory leak checks; release documentation; Gradle build validation. | Stable execution on 2 GB RAM head unit. |
