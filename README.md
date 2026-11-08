<div align="center">
  <img width="200" height="200" alt="Hoopoe Automotive Companion Logo" src="./hoopoe_logo.png" />
  <h1>HOOPOE — Smart Automotive Companion</h1>
  <p><strong>A standalone automotive cockpit prototype for aftermarket Android head units. Hardware integrations vary by device and adapter.</strong></p>

  [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
  [![Platform: Android](https://img.shields.io/badge/Platform-Android%2010%2B%20%28API%2029%2B%29-green.svg)](https://developer.android.com)
  [![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-purple.svg)](https://developer.android.com/jetpack/compose)
  [![Architecture: Standalone App](https://img.shields.io/badge/Architecture-Standalone%20Companion-gold.svg)](#-standalone-companion-architecture)
  [![Target profile: 2GB RAM](https://img.shields.io/badge/Target%20profile-2GB%20RAM-orange.svg)](#-hardware--vehicle-compatibility)
</div>

---

## 📖 Table of Contents
- [Overview & Evolution](#-overview--evolution)
- [Standalone Companion Architecture](#-standalone-companion-architecture)
- [Complete 10-Module Inventory](#-complete-10-module-inventory)
- [Hardware & Vehicle Compatibility](#-hardware--vehicle-compatibility)
- [Optimizations for 2 GB RAM & Automotive SoCs](#-optimizations-for-2-gb-ram--automotive-socs)
- [Installation Guide](#-installation-guide)
- [Building from Source](#-building-from-source)
- [Project Documentation](#-project-documentation)
- [License & Attribution](#-license--attribution)

---

## 🧠 Overview & Evolution

HOOPOE was originally conceived as a custom Android car launcher. However, replacing the system default launcher (`category.HOME`) on aftermarket head units introduces critical issues:
- Interference with factory-integrated 360-degree reversing cameras.
- Black-screen lockouts during MCU firmware sleep/wake cycles.
- Loss of proprietary OEM hardware settings (radio, steering wheel controls, amplifier DSP).

**Development Direction:**
HOOPOE now operates as a **Standalone Automotive Companion Application**. It delivers a full cockpit infotainment environment—digital speedometer, navigation centre, media control, OBD-II telemetry, TPMS, and an Islamic travel companion—without modifying system partitions, requiring root, or replacing the factory launcher.

---

## 🛡️ Standalone Companion Architecture

1. **Non-Invasive Execution:** Runs like any standard Android application. Drivers can launch it manually, configure optional automatic activation on device boot, or exit back to the factory system launcher at any time.
2. **Reverse Camera Coexistence:** Passive Android lifecycle yielding ensures that when reverse gear is engaged, the factory 360-degree camera takes over the display without interference. HOOPOE seamlessly resumes when reverse is disengaged.
3. **No Root or Firmware Tampering:** Does not modify system partitions, SELinux policies, or proprietary MCU daemons.
4. **Hardware Failure Isolation:** Graceful fallbacks for missing sensors—satellite GPS metrics are strictly segregated from ECU OBD-II parameters, and offline fallbacks prevent crashes when hardware is unavailable.

---

## 🚀 Complete 10-Module Inventory

### 🏎️ Module 1: Automotive Dashboard (HUD)
- **Digital Speedometer:** Low-latency GPS speed in KM/H or MPH with needle animation and overspeed alert styling.
- **Digital Clock & Date:** High-contrast touch-friendly display with minute-boundary update intervals to prevent CPU churn.
- **Holographic Assistant Ring (Jarvis Orb):** Interactive HUD orb indicating cruise standby (Cyan), Adhan/prayer time (Emerald), overspeed breach (Crimson), or weather alert (Amber).
- **MDUS OS Bottom Dock:** Branded Hoopoe emblem and styled technology identity at bottom-center.
- **Fuel & KM Log Widget:** Quick refuel tracking and mileage odometer.
- **3 Layout Presets:** Standard Cockpit (RHD), Minimal Focus HUD, and Tri-Pane Telemetry.

### 🗺️ Module 2: Navigation Centre
- **1-Touch Destination Routing:** Instant search and navigation intents to Google Maps, OsmAnd, or Organic Maps.
- **Quick POIs:** One-tap routes to nearby Petrol Stations, Mosques, Hospitals, and Restaurants.
- **Home & Work Shortcuts:** Direct commuting routes configured in preferences.
- **Embedded Map View:** OpenStreetMap canvas for visual context. The in-app connector line is illustrative; road-following routes and turn guidance are not implemented.

### 🎵 Module 3: Media Centre
- **Universal Playback Controls:** Tactile 48dp+ buttons for Play/Pause, Next Track, Previous Track, and Progress Scrubbing.
- **Dual Source Switching:** Instant toggle between streaming/local media (Spotify, VLC, USB audio) and the vehicle's **FM/AM Hardware Radio**.
- **MediaSession Integration:** Low-overhead metadata parsing with album art clamped to 512×512 to prevent OOM errors on 2 GB RAM.

### 🔧 Module 4: Vehicle Information & Diagnostics
- **GPS vs. ECU Data:** Strictly isolates GPS-derived telemetry (speed, heading, altitude) from authentic vehicle ECU data.
- **Bluetooth OBD-II (ELM327):** SPP RFCOMM integration for RPM, speed, coolant temperature, fuel level, battery voltage, and DTC reads; supported values depend on the adapter and vehicle.
- **TPMS screen:** Provides a four-wheel layout. Live sensor readings and calibration are not implemented; values remain unavailable until a compatible receiver integration is added.

### 🕌 Module 5: Islamic Travel Companion
- **Offline Astronomical Prayer Timings:** Computes Fajr, Dhuhr, Asr, Maghrib, and Isha using Meeus solar algorithms without internet connectivity.
- **5 Calculation Methods:** Muslim World League (MWL), ISNA, Egyptian General Authority, Umm Al-Qura (Makkah), and Karachi.
- **Asr Juristic Methods:** Standard (Shafi'i, Maliki, Hanbali) and Hanafi calculation.
- **Qibla Direction:** Real-time compass heading and Kaaba bearing with figure-8 sensor calibration guidance.
- **Travel Dua (Safar Ki Dua):** Authentic Arabic text, transliteration, English translation, and offline Text-to-Speech audio recitation.

### ⚡ Module 6: Smart Automation
- **Boot Auto-Launch:** Optional `BootReceiver` triggers HOOPOE upon head unit cold boot.
- **Logon Video Animation:** Hardware-accelerated `hoopoe_logon.mp4` boot animation with instant tap-to-skip.
- **Overspeed Warning System:** Configurable threshold triggering visual alerts on the speedometer and Assistant Ring.
- **Adhan Reminders:** Audio and visual notifications at prayer time arrivals.

### 🎙️ Module 7: Voice Assistant
- **Push-to-Talk Activation:** Single tap on the central Assistant Ring or microphone icon triggers speech recognition.
- **Automotive Voice Intents:** Hands-free commands for navigation, music control, prayer times, and diagnostics.
- **TTS Feedback:** Natural voice feedback for driving status and queries.

### 🎛️ Module 8: Quick Controls
- One-touch panel for screen brightness adjustment, Wi-Fi and Bluetooth settings shortcuts, and an **Exit to Car System Launcher** button.

### 🎨 Module 9: Theme & Customization Engine
- **4 Master Themes:**
  1. *Dark Automotive (Default)* — Obsidian background with electric blue accents.
  2. *Black & Champagne Gold* — Premium luxury cockpit aesthetic.
  3. *Graphite Tech* — Industrial dark graphite surfaces with neon cyan accents.
  4. *Minimal Dashboard* — Clean monochrome HUD for zero distraction.
- Full DataStore persistence across reboots.

### 📷 Module 10: Reverse Camera & Head-Unit Coexistence
- Non-invasive lifecycle handling yielding to the factory 360-degree reverse camera.
- Manual camera shortcut to launch the factory reverse camera application.

---

## 📟 Hardware & Vehicle Compatibility

| Parameter | Specifications / Target Profile |
| :--- | :--- |
| **Primary Target Vehicle** | Maruti Suzuki Dzire 2020 (RHD, India) |
| **Primary Target Head Unit** | JGL Aftermarket Android Head Unit |
| **SoC / Chipset** | Allwinner / Rockchip / MediaTek / UIS7862 |
| **Reported OS Version** | Android 15 (API 35/36 compatible) |
| **RAM Footprint** | 2 GB is a target profile; this repository does not include a measured PSS benchmark |
| **Storage** | 128 GB Internal Storage |
| **Factory Integrations** | 360° Reverse Camera, Bluetooth Handsfree/A2DP, GPS antenna |
| **Root Required?** | **No** (Standard standalone APK) |

---

## ⚡ Optimizations for 2 GB RAM & Automotive SoCs

- **Minute-Boundary Clock Updates:** Eliminates 59 unnecessary recompositions and CPU wakeups every minute.
- **Bitmap Clamping:** Album artwork is clamped to 512×512 pixels to prevent Out-Of-Memory crashes.
- **Vector HUD Animations:** Lightweight native Canvas drawing rather than heavy video or Lottie loops.
- **Zero Background Polling:** GPS and Bluetooth receivers strictly unregister when the app is paused or in background.

---

## 📲 Installation Guide

### Method 1: USB Flash Drive (Recommended)
1. Copy `app-debug.apk` to a USB flash drive (FAT32 or exFAT).
2. Plug the USB drive into your car's USB port.
3. Open the head unit's **File Manager** or **ApkInstaller**.
4. Select `app-debug.apk` and tap **Install**.
5. Launch **HOOPOE** from your app drawer.

### Method 2: Wireless or Wired ADB
```bash
adb connect <head-unit-ip>:5555
adb install -r app-debug.apk
```

---

## 🛠️ Building from Source

### Prerequisites
- **JDK:** OpenJDK 17
- **Android SDK:** Platform 36 (Android 16 DP/15+), Build-Tools 36.0.0
- **Gradle:** 9.4.1 (configured via wrapper)

### Build Command
```bash
./gradlew assembleDebug
```
The compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📚 Project Documentation
- [PROJECT_AUDIT.md](file:///home/mdus/Documents/Car%20app%20Hopoo/PROJECT_AUDIT.md) — Comprehensive technical audit & gap analysis.
- [ARCHITECTURE.md](file:///home/mdus/Documents/Car%20app%20Hopoo/ARCHITECTURE.md) — System architecture, HAL, and memory management.
- [FEATURE_ROADMAP.md](file:///home/mdus/Documents/Car%20app%20Hopoo/FEATURE_ROADMAP.md) — Module specifications & milestones.
- [DEVICE_COMPATIBILITY.md](file:///home/mdus/Documents/Car%20app%20Hopoo/DEVICE_COMPATIBILITY.md) — Hardware profile & 360° camera coexistence.
- [USER_GUIDE.md](file:///home/mdus/Documents/Car%20app%20Hopoo/USER_GUIDE.md) — Complete user manual & setup instructions.
- [KNOWN_LIMITATIONS.md](file:///home/mdus/Documents/Car%20app%20Hopoo/KNOWN_LIMITATIONS.md) — Hardware boundaries & fallback behaviors.

---

## 📄 License & Attribution

This project is licensed under the **[MIT License](LICENSE)**.
Maintained by **Mohamed Usman** ([@neotech4u](https://github.com/neotech4u)).
