package com.hoopoe.launcher.integration.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

data class ObdLiveTelemetry(
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    val coolantTempC: Int? = null,
    val fuelLevelPct: Float? = null,
    val engineLoadPct: Float? = null,
    val batteryVoltage: Float? = null,
    val dtcCodes: List<String> = emptyList(),
    val connectionState: ObdConnectionState = ObdConnectionState.DISCONNECTED,
    val statusMessage: String = "OBD-II Standalone"
)

enum class ObdConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

class ObdManager private constructor(private val context: Context) {

    companion object {
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val TAG = "HoopoeObdManager"

        @Volatile
        private var instance: ObdManager? = null

        fun getInstance(context: Context): ObdManager {
            return instance ?: synchronized(this) {
                instance ?: ObdManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _telemetry = MutableStateFlow(ObdLiveTelemetry())
    val telemetry: StateFlow<ObdLiveTelemetry> = _telemetry.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var queryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    fun connect() {
        if (_telemetry.value.connectionState == ObdConnectionState.CONNECTING ||
            _telemetry.value.connectionState == ObdConnectionState.CONNECTED
        ) return

        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null || !btAdapter.isEnabled) {
            _telemetry.value = _telemetry.value.copy(
                connectionState = ObdConnectionState.DISCONNECTED,
                statusMessage = "Bluetooth Offline"
            )
            return
        }

        scope.launch {
            try {
                _telemetry.value = _telemetry.value.copy(
                    connectionState = ObdConnectionState.SCANNING,
                    statusMessage = "Searching Paired OBD..."
                )

                val pairedDevices = btAdapter.bondedDevices
                val obdCandidate: BluetoothDevice? = pairedDevices?.firstOrNull { device ->
                    val name = device.name?.lowercase() ?: ""
                    name.contains("obd") || name.contains("elm") || name.contains("vgate") || name.contains("car")
                }

                if (obdCandidate == null) {
                    _telemetry.value = _telemetry.value.copy(
                        connectionState = ObdConnectionState.DISCONNECTED,
                        statusMessage = "No Paired OBD-II Adapter"
                    )
                    return@launch
                }

                _telemetry.value = _telemetry.value.copy(
                    connectionState = ObdConnectionState.CONNECTING,
                    statusMessage = "Connecting: ${obdCandidate.name}"
                )

                btAdapter.cancelDiscovery()
                val s = obdCandidate.createRfcommSocketToServiceRecord(SPP_UUID)
                s.connect()
                socket = s
                inputStream = s.inputStream
                outputStream = s.outputStream

                // Initialize ELM327 protocol
                sendCommand("ATZ")   // Reset
                delay(400)
                sendCommand("ATE0")  // Echo off
                delay(200)
                sendCommand("ATL0")  // Linefeeds off
                delay(200)
                sendCommand("ATSP0") // Auto protocol
                delay(300)

                _telemetry.value = _telemetry.value.copy(
                    connectionState = ObdConnectionState.CONNECTED,
                    statusMessage = "OBD-II Linked: ${obdCandidate.name}"
                )

                startPollingLoop()

            } catch (e: Exception) {
                Log.e(TAG, "OBD connection failed", e)
                disconnect("Connection Failed: ${e.localizedMessage ?: "Timeout"}")
            }
        }
    }

    private fun startPollingLoop() {
        queryJob?.cancel()
        queryJob = scope.launch {
            while (isActive && socket?.isConnected == true) {
                try {
                    // Query RPM (010C)
                    val rpmRaw = sendCommand("010C")
                    val rpm = parseRpm(rpmRaw)

                    // Query Speed (010D)
                    val speedRaw = sendCommand("010D")
                    val speed = parseSpeed(speedRaw)

                    // Query Coolant (0105)
                    val coolantRaw = sendCommand("0105")
                    val coolant = parseCoolant(coolantRaw)

                    // Query Fuel Level (012F)
                    val fuelRaw = sendCommand("012F")
                    val fuelPct = parseFuel(fuelRaw)

                    // Query Battery Voltage (ATRV)
                    val voltRaw = sendCommand("ATRV")
                    val volts = voltRaw.replace("[^0-9.]".toRegex(), "").toFloatOrNull() ?: 12.6f

                    _telemetry.value = _telemetry.value.copy(
                        rpm = rpm,
                        speedKmh = speed,
                        coolantTempC = coolant,
                        fuelLevelPct = fuelPct,
                        batteryVoltage = volts,
                        connectionState = ObdConnectionState.CONNECTED
                    )

                    delay(300) // 300ms polling interval for responsive telemetry without overwhelming bus
                } catch (e: Exception) {
                    Log.w(TAG, "OBD Poll loop error", e)
                    break
                }
            }
        }
    }

    private fun sendCommand(cmd: String): String {
        val out = outputStream ?: return ""
        val inStream = inputStream ?: return ""
        out.write((cmd + "\r").toByteArray())
        out.flush()

        val buffer = ByteArray(256)
        val sb = StringBuilder()
        var b: Int
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 1200) {
            if (inStream.available() > 0) {
                b = inStream.read(buffer)
                if (b > 0) {
                    val part = String(buffer, 0, b)
                    sb.append(part)
                    if (part.contains(">")) break
                }
            }
        }
        return sb.toString().trim()
    }

    private fun parseRpm(resp: String): Int? {
        val cleaned = resp.replace(" ", "")
        val idx = cleaned.indexOf("410C")
        if (idx >= 0 && cleaned.length >= idx + 8) {
            val a = cleaned.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val b = cleaned.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return ((a * 256) + b) / 4
        }
        return _telemetry.value.rpm
    }

    private fun parseSpeed(resp: String): Int? {
        val cleaned = resp.replace(" ", "")
        val idx = cleaned.indexOf("410D")
        if (idx >= 0 && cleaned.length >= idx + 6) {
            return cleaned.substring(idx + 4, idx + 6).toIntOrNull(16)
        }
        return _telemetry.value.speedKmh
    }

    private fun parseCoolant(resp: String): Int? {
        val cleaned = resp.replace(" ", "")
        val idx = cleaned.indexOf("4105")
        if (idx >= 0 && cleaned.length >= idx + 6) {
            val a = cleaned.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return a - 40
        }
        return _telemetry.value.coolantTempC
    }

    private fun parseFuel(resp: String): Float? {
        val cleaned = resp.replace(" ", "")
        val idx = cleaned.indexOf("412F")
        if (idx >= 0 && cleaned.length >= idx + 6) {
            val a = cleaned.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return (a * 100f) / 255f
        }
        return _telemetry.value.fuelLevelPct
    }

    fun requestDtcCodes() {
        scope.launch {
            try {
                val resp = sendCommand("03")
                val codes = mutableListOf<String>()
                // Parse standard DTC responses e.g. "43 01 33 00 00" -> P0133
                if (resp.contains("43")) {
                    val tokens = resp.split("\\s+".toRegex())
                    for (i in 1 until tokens.size step 2) {
                        if (i + 1 < tokens.size && tokens[i] != "00" && tokens[i] != "43") {
                            codes.add("P" + tokens[i] + tokens[i + 1])
                        }
                    }
                }
                _telemetry.value = _telemetry.value.copy(
                    dtcCodes = if (codes.isEmpty()) listOf("No DTC Fault Codes Detected (System OK)") else codes
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read DTCs", e)
            }
        }
    }

    fun disconnect(msg: String = "Disconnected") {
        queryJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        inputStream = null
        outputStream = null
        _telemetry.value = _telemetry.value.copy(
            rpm = null,
            speedKmh = null,
            coolantTempC = null,
            fuelLevelPct = null,
            engineLoadPct = null,
            batteryVoltage = null,
            connectionState = ObdConnectionState.DISCONNECTED,
            statusMessage = msg
        )
    }
}
