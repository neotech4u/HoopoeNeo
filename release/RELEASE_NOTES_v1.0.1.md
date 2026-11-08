# HOOPOE Neo v1.0.1 — Standalone Automotive Companion

### 🦅 Official Release Highlights
HOOPOE Neo transforms aftermarket in-car Android infotainment head units (JGL, UIS7862, Rockchip, Allwinner, MediaTek running Android 10 through Android 15+) into a high-performance, non-invasive digital cockpit companion.

---

### 🚀 Key Features & Updates in v1.0.1

1. **Real In-Widget OpenStreetMap Navigation:**
   - Replaced static placeholder with a live, high-resolution OpenStreetMap (OSM) widget directly on the home dashboard.
   - Fixed previous color filter artifacts to provide clear, authentic street and terrain rendering.
   - Destination searches and POI shortcuts (`⛽ PETROL`, `🕌 MOSQUE`, `🏥 HOSPITAL`) draw destination markers and route polylines directly inside the widget, accompanied by a distance & ETA HUD, without kicking the driver out into external applications unless explicitly chosen (`GOOGLE MAPS ↗`).

2. **Interactive Music & Radio Widgets (Not Mere Shortcuts):**
   - **FM/AM Radio Deck:** Operates as a live in-dash tuner widget with band switching (`FM1`, `FM2`, `FM3`, `AM`), `◄ SEEK`, `SEEK ►`, frequency stepping, power control, and 6 tactile memory presets (`88.5`, `91.1`, `98.3`, `101.9`, `104.3`, `107.5`). Seamlessly communicates with factory car radio apps.
   - **Smart Music Player:** Directly hosts playback controls (`Prev`, `Play/Pause`, `Next`), track metadata, album artwork, and real-time audio spectrum visualizer. Features quick launcher chips for installed media players (`Spotify`, `VLC`, `YouTube Music`, `Amazon`, etc.).

3. **Authentic Vehicle Data & Strict "NA" Fallback:**
   - Completely eliminated simulated RPM and placeholder fuel percentages.
   - Live parameters reflect authentic ECU data when OBD-II adapter is connected; when disconnected, values strictly denote `NA` to avoid misleading the driver.
   - Clear distinction between GPS-derived parameters and vehicle ECU sensors.

4. **Standard IPCC/DEFRA $\text{CO}_2$ Emission Formula & Fuel Selector:**
   - Standardized automotive emission calculations based on IPCC and DEFRA factors:
     - **Petrol (Default):** $2,310\text{ g } \text{CO}_2/\text{L}$
     - **Diesel:** $2,680\text{ g } \text{CO}_2/\text{L}$
     - **CNG:** $2,750\text{ g } \text{CO}_2/\text{kg}$
     - **LPG:** $1,510\text{ g } \text{CO}_2/\text{L}$
     - **Electric:** $0\text{ g } \text{CO}_2/\text{km}$
   - Real-time dynamic driving efficiency modes: `ECO` (optimal low-emission cruising), `PILOT`, `DYNAMIC`, and `SPORT`.

5. **Trip Distance Logger (Turn-On to Turn-Off):**
   - Accurately tracks and accumulates distance covered for every ride between app launch (`onStart`) and exit (`onStop`).
   - Automatically writes each finished ride into persistent `rides_log.json` and `rides_log.csv` files with start/end timestamps, distance (km), duration (mins), average speed, fuel consumed, and $\text{CO}_2$ emissions.
   - Formatted for automated Home Assistant synchronization when the vehicle connects to home Wi-Fi.

6. **Vehicle Mileage & Odometer Setup:**
   - Dedicated `VehicleMileageDialog` allowing the driver to select fuel type, adjust mileage (km/L or km/kg) with steppers and presets (`Maruti Suzuki Dzire 19.0 km/L`, Hatchback `18.0`, Sedan `15.0`, Hybrid `24.0`, CNG `28.0`), and input manual odometer readings if OBD is unavailable.

7. **Clean Unobstructed Cockpit UI:**
   - Official HOOPOE emblem and `MDUS OS` tech branding positioned in the top-left cockpit rail.
   - Removed redundant bottom-center map logo overlay to keep the live map and navigation unobstructed.
   - Full reverse camera preservation: pauses smoothly on reverse gear engagement and resumes on Drive.

---

### 📦 Release Assets
- `hoopoe-neo-v1.0.1-release.apk` (19 MB — Optimized, ProGuard-processed and signed production release APK)
- `hoopoe-neo-v1.0.1-debug.apk` (26 MB — Development and debugging APK with extended logging)
