# HOOPOE DEVICE COMPATIBILITY & HARDWARE SPECIFICATION

**Primary Target Hardware:** JGL Aftermarket Android Head Unit  
**Operating System:** Android 15 (Reported by Device Firmware)  
**System Memory:** 2 GB RAM  
**Internal Storage:** 128 GB Flash  
**Display:** Automotive Landscape Touchscreen (1024×600 / 1280×720 / 1920×720)  
**Target Vehicle:** Maruti Suzuki Dzire 2020 (Right-Hand Drive, India)  
**Auxiliary Hardware:** Factory 360° Camera System, Bluetooth Handsfree/Audio, External GPS Antenna  

---

## 1. Hardware Compatibility Matrix

| Hardware Feature | Head Unit Implementation | Standard Android Support | HOOPOE Implementation & Fallback Strategy |
| :--- | :--- | :--- | :--- |
| **Reverse / 360° Camera** | Dedicated MCU hardware trigger (Reverse wire + CANbus); switches video input directly to screen or fires factory camera app. | Not standard across AOSP. Managed by vendor HAL / MCU service. | **Passive Coexistence:** HOOPOE does not attempt to draw over or capture the reverse camera feed. When reverse gear engages, HOOPOE pauses gracefully and resumes cleanly when camera exits. Manual camera shortcut routes to installed vendor AVIN/Camera app. |
| **Bluetooth OBD-II (ELM327)** | Bluetooth Classic (BR/EDR). Some aftermarket car ROMs restrict Bluetooth in userspace to Phone/A2DP only. | `BluetoothSocket` over RFCOMM (UUID `00001101-0000-1000-8000-00805F9B34FB`). | **Multi-tier Detection:** 1) Scan paired devices for ELM327/OBD adapters; 2) If found, establish RFCOMM SPP stream; 3) If unavailable or blocked by head unit Bluetooth stack, display clean "OBD-II Hardware Disconnected" status card and derive speed, distance, and acceleration metrics strictly from GPS. |
| **TPMS (Tyre Pressure)** | External RF receiver (USB dongle or Bluetooth BLE sensors). | USB Serial or Bluetooth GATT (vendor dependent). | **Diagnostic Screen & Calibration Guide:** Interface renders 4-wheel pressures/temperatures. If hardware receiver is absent, shows "TPMS Hardware Not Detected" with sensor pairing guide and optional test diagnostics. |
| **GPS & Satellite Telemetry** | External ceramic GPS antenna connected to rear head-unit SMA port. | Android `LocationManager` (`GPS_PROVIDER`, `NETWORK_PROVIDER`). | **NMEA & Sensor Fusion:** Direct satellite coordinate tracking, speed calculation, and bearing heading via low-pass circular filter. Offline caching of last valid location. |
| **AM/FM Hardware Radio** | On-board Si4754 / NXP TEF6686 tuner driven by MCU microprocessor. | No public Android FM API. Accessible via vendor broadcast or MediaSession. | **Dual-backend Tuner:** 1) szchoiceway MCU broadcast commands (`ACTION_MCU_CMD_EVENT`); 2) Universal MediaSession mirroring of the factory radio app (extracting frequency from RDS/session metadata and routing seek to media buttons). |
| **Steering Wheel Controls** | Key1 / Key2 resistive analog ladder connected to MCU. | MCU maps keys to Android KeyEvents (`KEYCODE_MEDIA_NEXT`, `KEYCODE_VOLUME_UP`, etc.). | **Standard Media Key Handler:** Handled natively via `MediaSession` and `KeyEvent` dispatch in `NowPlaying` and `MainActivity`. |

---

## 2. 2 GB RAM Automotive Constraints & Engineering Rules

Most aftermarket head units running budget SoCs (Allwinner T3/T5/A133, Rockchip PX5/PX6, MediaTek MT8667) suffer from memory exhaustion when modern heavy Android apps run. HOOPOE is strictly engineered to maintain a memory footprint under **85 MB**:

1. **Recomposition Elimination:**
   - Clock widgets update strictly on minute boundaries rather than every second (saving ~59 recompositions/min).
   - Speedometer gauge animations are throttled to 30–60 FPS with hardware canvas acceleration.
2. **Bitmap Clamping:**
   - Media album art loaded from streaming apps is strictly clamped to `512×512` pixels using Coil Compose.
   - Large wallpapers are downsampled before loading into memory.
3. **Map Cache Constraints:**
   - Osmdroid tile cache is clamped to a maximum of 12 tiles in RAM; older tiles are dropped to flash disk cache.
   - Map thread pool is limited to 2 worker threads to avoid CPU core contention.
4. **Lifecycle-Aware Component Suspension:**
   - When HOOPOE is placed in the background (e.g., driver opens Google Maps or reverse camera engages), GPS listeners, sensor listeners, and canvas render loops are immediately paused in `onStop()`.
   - Audio players and video decoder handles are freed immediately upon screen exit.

---

## 3. Android 15 Platform Compatibility & Permission Guards

| Android 15 Guard | Impact on Car Companion | HOOPOE Architecture Solution |
| :--- | :--- | :--- |
| **Background Activity Start Gate** | Background services cannot launch activities arbitrarily. | `BootReceiver` does not launch activities directly from the background; instead, it posts a high-priority launch notification or starts the companion Activity through standard permitted intent flags (`FLAG_ACTIVITY_NEW_TASK`). |
| **Notification Listener Policy** | Android requires explicit permission in System Settings to read `MediaSession` metadata. | HOOPOE detects permission status and displays an intuitive 1-tap shortcut to the system `ACTION_NOTIFICATION_LISTENER_SETTINGS` screen, with a fallback display when ungranted. |
| **Nearby Devices & Bluetooth Permissions** | Android 12+ requires `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` runtime permissions with `neverForLocation` flags. | Declares runtime permissions gracefully; if user denies Bluetooth permissions, app operates without OBD-II and shows an explanatory card without crashing. |
| **Foreground Service Types** | Android 14+ enforces explicit `foregroundServiceType` declarations. | HOOPOE avoids unnecessary long-running background services. Background processing is limited to `NotificationListenerService` which is managed by the Android system framework. |
