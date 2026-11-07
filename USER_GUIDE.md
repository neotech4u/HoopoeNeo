# HOOPOE — Automotive Companion: Comprehensive User & Field Guide

## 1. Overview & Architecture

**HOOPOE** is a premium, high-performance standalone automotive companion application engineered specifically for aftermarket Android head units (e.g., JGL, Allwinner, Rockchip, MediaTek, UIS7862) running Android 10 through Android 15+. 

Unlike traditional custom car launchers that aggressively replace the Android home screen (`android.intent.category.HOME`), HOOPOE operates as a **non-invasive, standalone companion application**. This architectural decision provides distinct advantages:

1. **Zero Launcher Hijacking:** Your factory system launcher, factory MCU settings, and OEM interfaces remain completely intact.
2. **Factory 360° Reverse Camera Protection:** The vehicle's hardware-triggered 360° surround camera and reverse-gear interruption operate with 100% reliability, completely bypassing any risk of launcher crash or black-screen lockout.
3. **Firmware Stability:** No root access, no ADB system-privilege tampering, and no system partition modifications are ever required.
4. **Instant Exit & Switch:** Drivers can toggle between HOOPOE's futuristic cockpit HUD and the factory head-unit home screen at any time with a single touch.

---

## 2. Installation on Aftermarket Head Units

### Method A: USB Flash Drive Sideload (Recommended for JGL Head Units)
1. Copy the compiled `app-debug.apk` (or `app-release.apk`) to a USB flash drive (FAT32 or exFAT formatted).
2. Insert the USB drive into the vehicle's USB media/charging port connected to the head unit.
3. Open the factory **File Manager** or **ApkInstaller** app on your Android head unit.
4. Locate `app-debug.apk` on the USB storage drive and tap to install.
5. If prompted, toggle **Allow installation from unknown sources** for the file manager.
6. Once installation completes, tap **Open** or find **HOOPOE** in your factory app drawer.

### Method B: Wireless or Wired ADB
If your head unit is connected to local Wi-Fi or developer USB:
```bash
# Connect to head unit IP
adb connect <head-unit-ip>:5555

# Install APK directly
adb install -r app-debug.apk
```

---

## 3. First-Run Setup & Permissions Walkthrough

When launching HOOPOE for the first time, a streamlined **Cockpit Setup Guide** guides you through required permissions. Every permission is strictly automotive-standard:

| Permission | Purpose in HOOPOE | Hardware Fallback if Denied |
| :--- | :--- | :--- |
| **Location (Fine / GPS)** | Drives digital speedometer, altimeter, offline solar prayer time calculation, and Qibla azimuth calculation. | Speedometer displays `0 KM/H (GPS Required)`; prayer times fallback to stored coordinates. |
| **Bluetooth (Connect & Scan)** | Connects to ELM327 Bluetooth OBD-II scanners and external TPMS receivers. | OBD/TPMS panels switch to simulated/offline status cards. |
| **Notification Listener** | Captures live media playback metadata (Spotify, VLC, YouTube Music, Radio) via standard Android `MediaSession`. | Shows manual playback controls without dynamic song title/artist metadata. |
| **Audio (Microphone)** | Push-to-talk speech recognition for hands-free voice commands. | Voice Assistant button can trigger Google Assistant or installed speech engines. |
| **Boot Completed** | Allows optional automatic startup when the car head unit boots up. | Manual launch from app drawer remains available. |

---

## 4. Coexistence with Factory Head Unit & 360° Reverse Camera

### Reverse Gear Engagement
On aftermarket head units (such as JGL on the Maruti Suzuki Dzire 2020), reverse gear triggers an immediate CAN-bus / hardware interrupt that launches the dedicated reverse camera application or MCU overlay:
- **HOOPOE's Behavior:** The Android OS immediately invokes HOOPOE's `onPause()` / `onStop()` lifecycle callback. HOOPOE halts background animations, releases GL/video decoder surfaces, and passively waits in memory.
- **Reverse Gear Disengagement:** When shifting back to Drive (D) or Neutral (N), the camera application yields and Android immediately brings HOOPOE back to the foreground via `onResume()`, instantly restoring gauges and widgets without any latency.

### Exiting to Factory Launcher
To return to your car's factory Android home screen:
1. Tap the **Quick Controls** icon in the sidebar or navigate to **Quick Controls**.
2. Tap the large button labeled **Exit to Car System Launcher**.
3. HOOPOE will dispatch an intent to the system launcher and minimize itself. Alternatively, pressing the hardware **Home** button on your steering wheel or head unit fascia returns to the factory launcher immediately.

---

## 5. Modules & Cockpit Controls

### Module 1: Automotive Dashboard (HUD)
The primary driving interface features:
- **Digital Speedometer:** Low-latency GPS speed in KM/H or MPH with smooth needle animation and overspeed alert styling.
- **Digital Clock & Date:** Large touch-friendly clock with minute-boundary update intervals to protect 2 GB RAM.
- **Holographic Assistant Ring (Jarvis Orb):** Interactive HUD orb that changes color dynamically based on driving state:
  - *Cyan / Blue:* Normal cruise & standby.
  - *Emerald Green:* Active prayer time or Azan alert.
  - *Crimson Red:* Vehicle speed exceeded configured speed limit.
  - *Amber / Orange:* Severe weather alert or high memory pressure.
- **MDUS OS Dock:** Positioned at the bottom center of the HUD, displaying the official Hoopoe emblem and styled technology identity.
- **Layout Presets:**
  - *Layout 1: Standard Cockpit (RHD)* — Speedometer on right, Navigation in center, Media & Widgets on left.
  - *Layout 2: Minimal Focus HUD* — Ultra-clean digital speedometer, clock, and primary navigation card.
  - *Layout 3: Tri-Pane Telemetry* — Comprehensive driver metrics with live trip computers and fuel log.

### Module 2: Navigation Centre
- **One-Touch POI Routing:** Dedicated shortcuts for instant routing to nearby **Petrol Stations**, **Mosques**, **Hospitals**, and **Restaurants**.
- **Home & Work Shortcuts:** Configurable in Settings for 1-tap commuting.
- **Embedded Map & External App Launch:** Features an offline-capable OpenStreetMap display while providing one-touch launching to Google Maps, OsmAnd, or Organic Maps.

### Module 3: Media Centre
- **Universal Player Controls:** Large, tactile 48dp+ buttons for Play/Pause, Next Track, Previous Track, and Progress Scrubbing.
- **Source Switcher:** Seamless toggle between streaming/local media (Spotify, VLC, YouTube Music, USB files) and the vehicle's **FM/AM Hardware Radio**.
- **Album Art & Track Info:** Low-overhead metadata rendering clamped to 512×512 resolution to prevent Out-Of-Memory events on 2 GB head units.

### Module 4: Vehicle Information & Diagnostics
- **GPS vs. ECU Data:** Strictly distinguishes satellite-calculated metrics (GPS Speed, Heading, Altitude) from authentic vehicle ECU metrics.
- **Bluetooth ELM327 OBD-II Reader:** Connects to standard ELM327 adapters over Bluetooth SPP. Displays live RPM, Coolant Temperature, Throttle Position, Battery Voltage, and scans for Diagnostic Trouble Codes (DTC).
- **TPMS 4-Wheel Monitor:** Visualizes real-time tyre pressure (PSI/Bar) and temperature (°C) with color-coded warnings for low pressure or overheating.

### Module 5: Islamic Travel Companion
- **High-Accuracy Offline Prayer Timings:** Computes Fajr, Dhuhr, Asr, Maghrib, and Isha using pure astronomical Meeus equations without requiring any internet connection.
- **5 Calculation Methods:**
  - Muslim World League (MWL)
  - Islamic Society of North America (ISNA)
  - Egyptian General Authority of Survey
  - Umm Al-Qura University (Makkah)
  - University of Islamic Sciences, Karachi
- **Asr Juristic Methods:** Standard (Shafi'i, Maliki, Hanbali) and Hanafi calculation.
- **Qibla Direction:** Built-in compass showing live heading and bearing directly to the Kaaba in Makkah, complete with figure-8 sensor calibration instructions.
- **Travel Dua (Safar Ki Dua):** Authentic Arabic text, English transliteration, English translation, and offline Text-to-Speech audio recitation.

### Module 6: Smart Automation
- **Auto-Launch on Boot:** When enabled, automatically brings HOOPOE to the screen as soon as the head unit boots up.
- **Logon Video Animation:** Plays the custom `hoopoe_logon.mp4` boot animation upon startup with instant tap-to-skip.
- **Overspeed Warning System:** Configurable speed threshold (e.g., 80 km/h, 100 km/h, 120 km/h). Triggers visual pulse and Assistant Ring alert when breached.
- **Adhan / Prayer Reminders:** Gentle audio/visual notification at prayer arrival times.

### Module 7: Voice Assistant
- **Push-to-Talk Activation:** Tap the microphone icon or the central Assistant Ring to initiate speech recognition.
- **Automotive Commands:** Hands-free control for navigation ("Navigate to petrol pump", "Find nearest mosque"), media ("Play music", "Pause", "Next track"), and diagnostics ("Check tire pressure", "Vehicle status").

### Module 8: Quick Controls
- Fast-access panel for screen brightness adjustment, Wi-Fi settings shortcut, Bluetooth pairing, and 1-tap exit to the factory car launcher.

### Module 9: Theme Engine
- **Dark Automotive (Default):** Deep obsidian background with electric blue and crisp white typography.
- **Black & Champagne Gold:** Ultra-premium luxury aesthetic with warm champagne gold accents.
- **Graphite Tech:** Industrial dark graphite surfaces with neon cyan accents.
- **Minimal Dashboard:** Ultra-clean monochrome theme optimized for zero driver distraction.

---

## 6. Driving Ergonomics & Safety
- **Right-Hand Drive (RHD) Optimized:** Designed natively for the Indian automotive market (Maruti Suzuki Dzire 2020), placing primary touch targets closest to the driver's reach.
- **Night-Driving Safety:** High-contrast, dark-mode surfaces prevent windscreen glare and night blindness.
- **High-Touch Surface Guidelines:** All interactive buttons exceed Android's 48×48dp minimum touch target specifications for effortless use on bumpy roads.
