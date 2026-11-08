package com.hoopoe.launcher.data

import android.content.Context
import android.location.Location
import android.util.Log
import com.hoopoe.launcher.util.FuelType
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
    val fuelType: String = "PETROL",
    val vehicleMileageKmL: Double = 19.0,
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
            put("fuel_type", fuelType)
            put("vehicle_mileage_km_l", vehicleMileageKmL)
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
                fuelType = obj.optString("fuel_type", "PETROL"),
                vehicleMileageKmL = obj.optDouble("vehicle_mileage_km_l", 19.0),
                vehicle = obj.optString("vehicle", "Maruti Suzuki Dzire 2020"),
                syncedHomeAssistant = obj.optBoolean("synced_home_assistant", false)
            )
        }
    }
}

/**
 * TripLogManager:
 * Automatically records distance covered and telemetry between each app turn-on and turn-off session.
 * Stores logs persistently in `rides_log.json` and `rides_log.csv` for Home Assistant Wi-Fi sync.
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

    // Active session state (turn on to turn off)
    private var activeTripStartTime: Long = System.currentTimeMillis()
    private var activeDistanceKm: Double = 0.0
    private var activeMaxSpeed: Double = 0.0
    private var activeSpeedSum: Double = 0.0
    private var activeSpeedSamples: Int = 0
    private var isTripActive: Boolean = false

    // Previous valid GPS location
    private var lastLat: Double = 0.0
    private var lastLon: Double = 0.0

    private val _rideHistory = MutableStateFlow<List<RideLogEntry>>(emptyList())
    val rideHistory: StateFlow<List<RideLogEntry>> = _rideHistory.asStateFlow()

    private val _currentActiveRideDistance = MutableStateFlow(0.0)
    val currentActiveRideDistance: StateFlow<Double> = _currentActiveRideDistance.asStateFlow()

    private val _currentTripDurationMins = MutableStateFlow(0)
    val currentTripDurationMins: StateFlow<Int> = _currentTripDurationMins.asStateFlow()

    private val _currentTripCo2Grams = MutableStateFlow(0.0)
    val currentTripCo2Grams: StateFlow<Double> = _currentTripCo2Grams.asStateFlow()

    init {
        loadHistory()
        startAppSession()
    }

    /**
     * Initializes a fresh ride trip session when the app turns on / launches.
     */
    fun startAppSession() {
        activeTripStartTime = System.currentTimeMillis()
        activeDistanceKm = 0.0
        activeMaxSpeed = 0.0
        activeSpeedSum = 0.0
        activeSpeedSamples = 0
        lastLat = 0.0
        lastLon = 0.0
        isTripActive = true
        _currentActiveRideDistance.value = 0.0
        _currentTripDurationMins.value = 0
        _currentTripCo2Grams.value = 0.0
        Log.i(TAG, "Started fresh vehicle trip session at $activeTripStartTime")
    }

    /**
     * Records GPS movement and accumulates distance covered between successive GPS coordinates.
     */
    fun recordGpsPoint(lat: Double, lon: Double, speedKmh: Float, fuelType: FuelType = FuelType.PETROL, mileage: Double = 19.0) {
        if (!isTripActive) {
            startAppSession()
        }

        if (speedKmh > activeMaxSpeed) {
            activeMaxSpeed = speedKmh.toDouble()
        }

        if (speedKmh > 1.0f) {
            activeSpeedSum += speedKmh
            activeSpeedSamples++
        }

        if (lastLat != 0.0 && lastLon != 0.0 && lat != 0.0 && lon != 0.0) {
            val results = FloatArray(1)
            Location.distanceBetween(lastLat, lastLon, lat, lon, results)
            val distMeters = results[0]

            // Filter out stationary GPS drift (< 8 meters or when speed is practically 0)
            if (distMeters >= 5.0 && distMeters < 1500.0 && (speedKmh > 1.5f || distMeters > 15.0)) {
                activeDistanceKm += (distMeters / 1000.0)
                _currentActiveRideDistance.value = activeDistanceKm

                // Standard emission formula calculation:
                // Fuel consumed = distance / mileage
                // CO2 = fuel consumed * emissionFactor
                val validMileage = maxOf(5.0, mileage)
                val fuelL = activeDistanceKm / validMileage
                val co2G = fuelL * fuelType.emissionFactorGramsPerUnit
                _currentTripCo2Grams.value = co2G
            }
        }

        lastLat = lat
        lastLon = lon

        val elapsedMins = maxOf(0, ((System.currentTimeMillis() - activeTripStartTime) / 60000L).toInt())
        _currentTripDurationMins.value = elapsedMins
    }

    /**
     * Fallback speed-based accumulation if GPS fix points are not continuously available.
     */
    fun onSpeedUpdate(speedKmh: Double, deltaSeconds: Double, fuelType: FuelType = FuelType.PETROL, mileage: Double = 19.0) {
        if (speedKmh > 2.5) {
            val addedKm = (speedKmh / 3600.0) * deltaSeconds
            activeDistanceKm += addedKm
            if (speedKmh > activeMaxSpeed) activeMaxSpeed = speedKmh
            activeSpeedSum += speedKmh
            activeSpeedSamples++

            _currentActiveRideDistance.value = activeDistanceKm

            val validMileage = maxOf(5.0, mileage)
            val fuelL = activeDistanceKm / validMileage
            val co2G = fuelL * fuelType.emissionFactorGramsPerUnit
            _currentTripCo2Grams.value = co2G

            val elapsedMins = maxOf(0, ((System.currentTimeMillis() - activeTripStartTime) / 60000L).toInt())
            _currentTripDurationMins.value = elapsedMins
        }
    }

    /**
     * Finalizes and logs the completed trip when the app is turned off / exited.
     */
    fun finishAppSession(
        reason: String = "App Turn Off",
        vehicleMileage: Double = 19.0,
        fuelType: FuelType = FuelType.PETROL
    ): RideLogEntry? {
        if (activeDistanceKm < 0.04) {
            // Under 40 meters: test restart or parking manoeuvre, no log required
            Log.d(TAG, "Trip distance ($activeDistanceKm km) below threshold, skipping persistence")
            return null
        }

        val endTime = System.currentTimeMillis()
        val durationMins = maxOf(1, ((endTime - activeTripStartTime) / 60000L).toInt())
        val avgSpeed = if (activeSpeedSamples > 0) activeSpeedSum / activeSpeedSamples else (activeDistanceKm / (durationMins / 60.0))

        // Standard IPCC / DEFRA formula:
        // Fuel = Distance / Mileage
        // CO2 = Fuel * Emission Factor
        val validMileage = maxOf(5.0, vehicleMileage)
        val estimatedFuel = activeDistanceKm / validMileage
        val estimatedCo2 = estimatedFuel * fuelType.emissionFactorGramsPerUnit

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
            fuelType = fuelType.code,
            vehicleMileageKmL = validMileage,
            startLocation = "Turn On ($reason)",
            endLocation = "Turn Off",
            syncedHomeAssistant = false
        )

        Log.i(TAG, "Completed Ride: ${entry.distanceKm} km in ${entry.durationMinutes} min, CO2: ${entry.estimatedCo2Grams} g")
        saveRide(entry)

        // Reset
        activeDistanceKm = 0.0
        _currentActiveRideDistance.value = 0.0
        return entry
    }

    fun finishAndLogCurrentRide(): RideLogEntry? {
        return finishAppSession("Manual Stop")
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
                    append("TripId,StartTime,EndTime,DistanceKm,DurationMinutes,AvgSpeedKmh,MaxSpeedKmh,Co2Grams,FuelLiters,FuelType,MileageKmL,Synced\n")
                }
                append("${entry.tripId},${entry.startTimeMillis},${entry.endTimeMillis},${entry.distanceKm},${entry.durationMinutes},${entry.avgSpeedKmh},${entry.maxSpeedKmh},${entry.estimatedCo2Grams},${entry.estimatedFuelLiters},${entry.fuelType},${entry.vehicleMileageKmL},${entry.syncedHomeAssistant}\n")
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
