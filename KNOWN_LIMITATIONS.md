# HOOPOE — Hardware Compatibility & Known Limitations

This document details the architectural boundaries, hardware dependencies, and known device-specific behaviors for **HOOPOE** operating on aftermarket Android head units (specifically tested for JGL / Allwinner / Rockchip / UIS7862 chipsets running Android 10 through Android 15 on vehicles such as the Maruti Suzuki Dzire 2020).

---

## 1. Bluetooth OBD-II (ELM327) Compatibility

### The Aftermarket Android Bluetooth Limitation
Many aftermarket car head units utilize proprietary or stripped-down Bluetooth chips (e.g., BC6, IVT, Realtek, GOC) designed specifically to handle **Bluetooth Hands-Free Profile (HFP)** and **A2DP Audio Streaming** connected to smartphones. 

On some head units:
- The standard Android Bluetooth Settings menu is hidden or replaced by a simplified OEM "Phone/BT" dialer app.
- Direct Bluetooth Serial Port Profile (SPP / RFCOMM UUID `00001101-0000-1000-8000-00805F9B34FB`) needed for standard ELM327 diagnostic adapters may be filtered by the head unit's MCU daemon.

### HOOPOE Safeguards & Fallbacks
1. **Zero Crash Guarantee:** If RFCOMM connection fails or Bluetooth is disabled, HOOPOE catches all `IOException` and security exceptions cleanly.
2. **Offline / Fallback Status:** Telemetry gauges clearly mark OBD status as `Disconnected` or `Unavailable`. HOOPOE never fabricates fake engine parameters.
3. **Alternative Connection Path:** If your head unit blocks Bluetooth SPP, drivers are advised to use **Wi-Fi OBD-II adapters** (connecting over TCP socket `192.168.0.10:35000`) or USB-to-UART ELM327 adapters, which bypass the head-unit Bluetooth restriction.

---

## 2. TPMS (Tyre Pressure Monitoring System) Hardware Variance

### The Hardware Reality
Aftermarket TPMS systems generally consist of:
- Four valve-stem or internal wheel sensors transmitting over 433.92 MHz radio frequency.
- A receiving module connected via either:
  1. **USB Receiver Dongle:** Communicates over USB-Serial or USB HID to a proprietary Android app.
  2. **Bluetooth Low Energy (BLE) Sensors:** Transmit raw BLE advertising packets with pressure/temperature payloads.

### HOOPOE Architecture
- HOOPOE provides a dedicated 4-wheel monitoring HUD and Bluetooth BLE scanning layer.
- Because USB-dongle TPMS manufacturers use proprietary obfuscated baud rates and packet structures, HOOPOE provides an extensible TPMS interface. If hardware sensors are not paired or an external dongle is unsupported, HOOPOE shows the last calibrated baseline pressures without generating false alarm warnings.

---

## 3. Factory 360-Degree Camera & Reverse Gear Interrupts

### Safety & Firmware Boundary
The Maruti Suzuki Dzire 2020 aftermarket setup includes a 360-degree bird's-eye camera system managed directly by an external camera control box and the head unit's CAN-bus/MCU hardware interrupt.

### Strict Non-Interference Policy
- **Safety Regulation Compliance:** When the driver shifts the vehicle into Reverse (R), the MCU immediately claims the display hardware and launches the 360-degree reversing camera view.
- HOOPOE **never** attempts to draw over system windows (`TYPE_APPLICATION_OVERLAY`) or prevent reverse camera invocation.
- HOOPOE enters the background gracefully on reverse gear, freeing hardware video decoders and CPU cycles to ensure the reversing camera feeds render at maximum frame rates.
- Upon shifting back into Drive (D), HOOPOE automatically resumes in the foreground.

---

## 4. Android 15 & OEM Background Execution Restrictions

### Cold Boot Autostart (`BootReceiver`)
- HOOPOE provides a `BootReceiver` listening for `ACTION_BOOT_COMPLETED`.
- Certain aggressive aftermarket head-unit power managers ("App Optimizer" or "Sleep Mode") put the head unit into an ultra-low-power standby rather than executing a full Linux cold boot. 
- In standby-wake cycles (`android.intent.action.SCREEN_ON`), standard boot broadcasts are not triggered. Drivers can tap the HOOPOE shortcut or configure their head unit's "Default Navigation/App on Wake" setting to target HOOPOE.

### Memory Restrictions (2 GB RAM)
To guarantee smooth operation on 2 GB RAM platforms:
- Background services are kept to an absolute minimum.
- Media album art is strictly downsampled and clamped to 512×512 pixels.
- The digital clock updates strictly on minute boundaries rather than every 1000ms.
- Holographic orb animations utilize native vector canvas drawing rather than heavy video or Lottie loops.

---

## 5. Satellite GPS Signal Acquisition

- Aftermarket head units depend on an external GPS antenna puck typically installed beneath the dashboard plastic or on the A-pillar.
- When parked in underground basements, covered carports, or heavy tunnels, GPS signal may be lost.
- HOOPOE clearly denotes `GPS Searching...` and retains the last-known latitude/longitude for solar prayer calculations, preventing calculation failures.

---

## 6. Offline Resilience

- HOOPOE is built on an **Offline-First** architecture.
- Speedometer, Altimeter, Trip Log, Qibla Compass, Prayer Calculations (Meeus astronomical algorithms), and Travel Dua are 100% functional without an active cellular or Wi-Fi connection.
- Weather updates and online map tile downloads gracefully pause when offline and resume automatically upon reconnection.
