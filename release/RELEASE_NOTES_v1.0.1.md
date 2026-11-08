# HOOPOE Neo v1.0.1 — Standalone Automotive Companion

### 🦅 Official Release Highlights
HOOPOE Neo transforms the aftermarket in-car infotainment experience into a premium, non-invasive cockpit environment engineered specifically for Android head units (JGL, Allwinner, Rockchip, MediaTek, UIS7862 running Android 10 through Android 15+).

---

### 🚀 Key Features & Updates in v1.0.1

- **Authentic ECU Telemetry & Zero-Fabrication Guarantee:**
  - Removed simulated RPM curves and placeholder fuel numbers. If live OBD-II data is received, authentic ECU values are displayed; otherwise, parameters strictly denote `NA`.
  - Distinguishes GPS satellite triangulation from vehicle ECU data.

- **Dynamic Drive Efficiency & Real-Time CO2 Emission:**
  - Automatically assesses vehicle speed and driving efficiency for the Maruti Suzuki Dzire 2020 (1.2L petrol engine).
  - Highlights optimal `ECO` speed band (35–70 km/h) with lowest carbon footprint (~105–125 g/km), `PILOT` cruising band, and `SPORT` / high-acceleration band.
  - Displays real-time estimated CO2 emissions (`CO2 ~114 g/km` or `IDLE (0.6 L/h)`).

- **Google Maps 1-Tap Shortcut:**
  - Integrated high-contrast `GOOGLE MAPS ↗` shortcut pill directly inside the dashboard navigation widget.
  - Launches Google Maps driving navigation mode via direct intent (`com.google.android.apps.maps`) with graceful fallback to geo intents.

- **HOOPOE Emblem & `MDUS OS` Branding:**
  - Replaced legacy text with official HOOPOE bird logo emblem accompanied by `HOOPOE` and `MDUS OS` tech branding.

- **Streamlined Refuel Log & Home Assistant Ride Logger:**
  - Simple, mobile-friendly refuel logger with quick volume chips (`5L`, `10L`, `15L`, `20L`, `25L`, `30L`, `Full Tank 37L`), stepper adjustment, and entry deletion.
  - Automatic trip distance and ride tracking saved to persistent `rides_log.json` and `rides_log.csv` (and `fuel_log.json`), ready for automatic synchronization with Home Assistant when the vehicle connects to home Wi-Fi.

- **Standalone Architecture & Reverse Camera Protection:**
  - Runs cleanly as a standard application without hijacking the factory launcher or modifying system firmware.
  - Non-invasive lifecycle yielding: automatically pauses (`onPause()`) when reverse gear engages the factory 360° camera, resuming smoothly on Drive.

- **Complete In-Cockpit Suites:**
  - Bluetooth ELM327 OBD-II live telemetry, offline Meeus astronomical prayer calculations, Qibla compass, Travel Dua with TTS, media controls (`MediaSession`), voice assistant, and 4 master themes.

---

### 📦 Release Assets
- `hoopoe-neo-v1.0.1-release.apk` (19 MB — Optimized and signed release APK)
- `hoopoe-neo-v1.0.1-debug.apk` (25 MB — Development and testing APK)
