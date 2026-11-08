package com.hoopoe.launcher.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ride/Trip Log Entry formatted for local persistence and Home Assistant ingestion.
 */
data class RideLogEntry(
    val tripId: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val distanceKm: Double,
    val durationMinutes: Int,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val estimatedCo2Grams: Double,
    val estimatedFuelLiters: Double,
    val startLocation: String = "GPS Recorded",
    val endLocation: String = "GPS Recorded",
    val vehicle: String = "Maruti Suzuki Dzire 2020",
    val syncedHomeAssistant: Boolean = false
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("trip_id", tripId)
            put("start_time_iso", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(startTimeMillis)))
            put("end_time_iso", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(endTimeMillis)))
            put("start_time_millis", startTimeMillis)
            put("end_time_millis", endTimeMillis)
            put("distance_km", String.format(Locale.US, "%.2f", distanceKm).toDouble())
            put("duration_minutes", durationMinutes)
            put("avg_speed_kmh", String.format(Locale.US, "%.1f", avgSpeedKmh).toDouble())
            put("max_speed_kmh", String.format(Locale.US, "%.1f", maxSpeedKmh).toDouble())
            put("estimated_co2_grams", String.format(Locale.US, "%.1f", estimatedCo2Grams).toDouble())
            put("estimated_fuel_liters", String.format(Locale.US, "%.2f", estimatedFuelLiters).toDouble())
            put("vehicle", vehicle)
            put("synced_home_assistant", syncedHomeAssistant)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): RideLogEntry {
            return RideLogEntry(
                tripId = obj.optString("trip_id", "ride_${System.currentTimeMillis()}"),
                startTimeMillis = obj.optLong("start_time_millis", System.currentTimeMillis()),
                endTimeMillis = obj.optLong("end_time_millis", System.currentTimeMillis()),
                distanceKm = obj.optDouble("distance_km", 0.0),
                durationMinutes = obj.optInt("duration_minutes", 0),
                avgSpeedKmh = obj.optDouble("avg_speed_kmh", 0.0),
                maxSpeedKmh = obj.optDouble("max_speed_kmh", 0.0),
                estimatedCo2Grams = obj.optDouble("estimated_co2_grams", 0.0),
                estimatedFuelLiters = obj.optDouble("estimated_fuel_liters", 0.0),
                vehicle = obj.optString("vehicle", "Maruti Suzuki Dzire 2020"),
                syncedHomeAssistant = obj.optBoolean("synced_home_assistant", false)
            )
        }
    }
}

/**
 * TripLogManager:
 * Records distance covered for each ride into a persistent local JSON & CSV file.
 * Formatted for direct Home Assistant integration once the car connects to home Wi-Fi.
 */
class TripLogManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "TripLogManager"
        private const val FILE_NAME = "rides_log.json"
        private const val CSV_FILE_NAME = "rides_log.csv"

        @Volatile
        private var instance: TripLogManager? = null

        fun getInstance(context: Context): TripLogManager {
            return instance ?: synchronized(this) {
                instance ?: TripLogManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Active trip session state
    private var activeTripStartTime: Long = 0L
    private var activeDistanceKm: Double = 0.0
    private var activeMaxSpeed: Double = 0.0
    private var activeSpeedSum: Double = 0.0
    private var activeSpeedSamples: Int = 0
    private var isTripActive: Boolean = false

    private val _rideHistory = MutableStateFlow<List<RideLogEntry>>(emptyList())
    val rideHistory: StateFlow<List<RideLogEntry>> = _rideHistory.asStateFlow()

    private val _currentActiveRideDistance = MutableStateFlow(0.0)
    val currentActiveRideDistance: StateFlow<Double> = _currentActiveRideDistance.asStateFlow()

    init {
        loadHistory()
    }

    /**
     * Call with live GPS/OBD speed (in km/h) and elapsed interval delta (seconds)
     */
    fun onSpeedUpdate(speedKmh: Double, deltaSeconds: Double) {
        if (speedKmh > 3.0) {
            // Vehicle is in motion
            if (!isTripActive) {
                // Auto-start ride
                isTripActive = true
                activeTripStartTime = System.currentTimeMillis()
                activeDistanceKm = 0.0
                activeMaxSpeed = speedKmh
                activeSpeedSum = speedKmh
                activeSpeedSamples = 1
            } else {
                // Accumulate distance: D = (v km/h / 3600) * deltaSeconds
                val addedKm = (speedKmh / 3600.0) * deltaSeconds
                activeDistanceKm += addedKm
                if (speedKmh > activeMaxSpeed) activeMaxSpeed = speedKmh
                activeSpeedSum += speedKmh
                activeSpeedSamples++
            }
            _currentActiveRideDistance.value = activeDistanceKm
        }
    }

    /**
     * Finalizes and logs the current active ride if minimum distance threshold (e.g. 0.1 km) was covered.
     */
    fun finishAndLogCurrentRide(): RideLogEntry? {
        if (!isTripActive || activeDistanceKm < 0.1) {
            isTripActive = false
            activeDistanceKm = 0.0
            _currentActiveRideDistance.value = 0.0
            return null
        }

        val endTime = System.currentTimeMillis()
        val durationMins = maxOf(1, ((endTime - activeTripStartTime) / 60000L).toInt())
        val avgSpeed = if (activeSpeedSamples > 0) activeSpeedSum / activeSpeedSamples else (activeDistanceKm / (durationMins / 60.0))
        
        // Dzire 1.2L petrol estimate: ~115g CO2/km average, ~0.048 L petrol per km (~21 km/L)
        val estimatedCo2 = activeDistanceKm * 115.0
        val estimatedFuel = activeDistanceKm / 21.0

        val entry = RideLogEntry(
            tripId = "ride_${activeTripStartTime}",
            startTimeMillis = activeTripStartTime,
            endTimeMillis = endTime,
            distanceKm = activeDistanceKm,
            durationMinutes = durationMins,
            avgSpeedKmh = avgSpeed,
            maxSpeedKmh = activeMaxSpeed,
            estimatedCo2Grams = estimatedCo2,
            estimatedFuelLiters = estimatedFuel,
            syncedHomeAssistant = false
        )

        // Reset active tracking
        isTripActive = false
        activeDistanceKm = 0.0
        _currentActiveRideDistance.value = 0.0

        // Persist
        saveRide(entry)
        return entry
    }

    private fun saveRide(entry: RideLogEntry) {
        scope.launch {
            val current = _rideHistory.value.toMutableList()
            current.add(0, entry)
            _rideHistory.value = current
            writeJsonFile(current)
            appendCsv(entry)
        }
    }

    private fun loadHistory() {
        scope.launch {
            try {
                val file = getStorageFile(FILE_NAME)
                if (file.exists()) {
                    val content = file.readText()
                    val arr = JSONArray(content)
                    val list = mutableListOf<RideLogEntry>()
                    for (i in 0 until arr.length()) {
                        list.add(RideLogEntry.fromJsonObject(arr.getJSONObject(i)))
                    }
                    _rideHistory.value = list
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading ride history", e)
            }
        }
    }

    private fun writeJsonFile(list: List<RideLogEntry>) {
        try {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJsonObject()) }
            val file = getStorageFile(FILE_NAME)
            file.writeText(arr.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Error saving ride history json", e)
        }
    }

    private fun appendCsv(entry: RideLogEntry) {
        try {
            val file = getStorageFile(CSV_FILE_NAME)
            val isNew = !file.exists() || file.length() == 0L
            file.appendText(buildString {
                if (isNew) {
                    append("TripId,StartTime,EndTime,DistanceKm,DurationMinutes,AvgSpeedKmh,MaxSpeedKmh,Co2Grams,FuelLiters,Synced\n")
                }
                append("${entry.tripId},${entry.startTimeMillis},${entry.endTimeMillis},${entry.distanceKm},${entry.durationMinutes},${entry.avgSpeedKmh},${entry.maxSpeedKmh},${entry.estimatedCo2Grams},${entry.estimatedFuelLiters},${entry.syncedHomeAssistant}\n")
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error saving ride csv", e)
        }
    }

    private fun getStorageFile(filename: String): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, filename)
    }

    /**
     * Payload ready to be synced with Home Assistant via MQTT or HTTP webhook
     */
    fun getHomeAssistantPayload(): String {
        val root = JSONObject()
        root.put("device", "HOOPOE_DZIRE_2020")
        root.put("timestamp", System.currentTimeMillis())
        val arr = JSONArray()
        _rideHistory.value.forEach { arr.put(it.toJsonObject()) }
        root.put("rides", arr)
        return root.toString(2)
    }
}
