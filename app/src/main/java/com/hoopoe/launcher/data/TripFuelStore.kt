package com.hoopoe.launcher.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TripRecord(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val avgSpeedKmh: Double,
    val fuelConsumedLiters: Double,
    val economyKmL: Double,
    val source: String // "OBD2_AUTO" | "GPS_MANUAL"
)

data class RefuelRecord(
    val id: String,
    val timestamp: Long,
    val odometerKm: Double,
    val liters: Double,
    val costTotal: Double,
    val costPerLiter: Double,
    val isFullTank: Boolean,
    val source: String // "AUTO_DETECTED" | "MANUAL"
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("timestamp", timestamp)
            put("date_iso", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(timestamp)))
            put("odometer_km", odometerKm)
            put("liters", liters)
            put("cost_total", costTotal)
            put("cost_per_liter", costPerLiter)
            put("is_full_tank", isFullTank)
            put("source", source)
        }
    }
}

class TripFuelRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("hoopoe_trip_fuel_store", Context.MODE_PRIVATE)

    private val _isObdOnline = MutableStateFlow(prefs.getBoolean("obd_online", false))
    val isObdOnline: StateFlow<Boolean> = _isObdOnline.asStateFlow()

    // Nullable fuelPercent: null means no authentic data yet (render "NA")
    private val _fuelPercent = MutableStateFlow<Float?>(
        if (prefs.contains("fuel_pct")) prefs.getFloat("fuel_pct", 0f) else null
    )
    val fuelPercent: StateFlow<Float?> = _fuelPercent.asStateFlow()

    // Nullable odometer: null means no authentic data yet (render "NA")
    private val _odometerKm = MutableStateFlow<Double?>(
        if (prefs.contains("odometer_km")) prefs.getFloat("odometer_km", 0f).toDouble() else null
    )
    val odometerKm: StateFlow<Double?> = _odometerKm.asStateFlow()

    private val _fuelPricePerLiter = MutableStateFlow(prefs.getFloat("fuel_price", 102.50f))
    val fuelPricePerLiter: StateFlow<Float> = _fuelPricePerLiter.asStateFlow()

    private val _refuelList = MutableStateFlow(loadRefuels())
    val refuelList: StateFlow<List<RefuelRecord>> = _refuelList.asStateFlow()

    private val _currentTrip = MutableStateFlow(
        TripRecord(
            id = "trip-active",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis(),
            distanceKm = 0.0,
            avgSpeedKmh = 0.0,
            fuelConsumedLiters = 0.0,
            economyKmL = 0.0,
            source = if (_isObdOnline.value) "OBD2_AUTO" else "GPS_MANUAL"
        )
    )
    val currentTrip: StateFlow<TripRecord> = _currentTrip.asStateFlow()

    fun setObdOnline(online: Boolean) {
        _isObdOnline.value = online
        prefs.edit().putBoolean("obd_online", online).apply()
        _currentTrip.value = _currentTrip.value.copy(
            source = if (online) "OBD2_AUTO" else "GPS_MANUAL"
        )
    }

    fun updateLiveObdFuel(pct: Float) {
        _fuelPercent.value = pct
        prefs.edit().putFloat("fuel_pct", pct).apply()
    }

    fun updateLiveOdometer(km: Double) {
        _odometerKm.value = km
        prefs.edit().putFloat("odometer_km", km.toFloat()).apply()
    }

    fun addRefuel(liters: Double, costPerLiter: Double, odometer: Double, isFullTank: Boolean) {
        val totalCost = liters * costPerLiter
        val newRecord = RefuelRecord(
            id = "refuel-${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            odometerKm = odometer,
            liters = liters,
            costTotal = totalCost,
            costPerLiter = costPerLiter,
            isFullTank = isFullTank,
            source = if (_isObdOnline.value) "AUTO_DETECTED" else "MANUAL"
        )

        val updatedList = listOf(newRecord) + _refuelList.value
        _refuelList.value = updatedList
        saveRefuels(updatedList)

        // 37.0 Litre tank capacity for Dzire 2020
        val addedPct = (liters / 37.0 * 100.0).toFloat()
        val current = _fuelPercent.value ?: 0f
        val newPct = if (isFullTank) 100f else (current + addedPct).coerceIn(0f, 100f)
        _fuelPercent.value = newPct
        _odometerKm.value = odometer

        prefs.edit()
            .putFloat("fuel_pct", newPct)
            .putFloat("odometer_km", odometer.toFloat())
            .putFloat("fuel_price", costPerLiter.toFloat())
            .apply()

        writeExternalLogs(updatedList)
    }

    fun deleteRefuel(id: String) {
        val updatedList = _refuelList.value.filter { it.id != id }
        _refuelList.value = updatedList
        saveRefuels(updatedList)
        writeExternalLogs(updatedList)
    }

    private fun loadRefuels(): List<RefuelRecord> {
        val jsonStr = prefs.getString("refuels_json", null) ?: return emptyList()
        val list = mutableListOf<RefuelRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RefuelRecord(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        odometerKm = obj.getDouble("odometerKm"),
                        liters = obj.getDouble("liters"),
                        costTotal = obj.getDouble("costTotal"),
                        costPerLiter = obj.getDouble("costPerLiter"),
                        isFullTank = obj.getBoolean("isFullTank"),
                        source = obj.getString("source")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveRefuels(list: List<RefuelRecord>) {
        try {
            val arr = JSONArray()
            list.take(30).forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp)
                    put("odometerKm", item.odometerKm)
                    put("liters", item.liters)
                    put("costTotal", item.costTotal)
                    put("costPerLiter", item.costPerLiter)
                    put("isFullTank", item.isFullTank)
                    put("source", item.source)
                }
                arr.put(obj)
            }
            prefs.edit().putString("refuels_json", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeExternalLogs(list: List<RefuelRecord>) {
        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val jsonFile = File(dir, "fuel_log.json")
            val arr = JSONArray()
            list.forEach { arr.put(it.toJsonObject()) }
            jsonFile.writeText(arr.toString(2))

            val csvFile = File(dir, "fuel_log.csv")
            val isNew = !csvFile.exists() || csvFile.length() == 0L
            csvFile.writeText(buildString {
                append("Id,Timestamp,DateIso,OdometerKm,Liters,CostTotal,CostPerLiter,IsFullTank,Source\n")
                list.forEach { r ->
                    val dateIso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(r.timestamp))
                    append("${r.id},${r.timestamp},\"$dateIso\",${r.odometerKm},${r.liters},${r.costTotal},${r.costPerLiter},${r.isFullTank},${r.source}\n")
                }
            })
        } catch (e: Exception) {
            Log.e("TripFuelRepository", "Error writing external fuel logs", e)
        }
    }

    companion object {
        @Volatile
        private var instance: TripFuelRepository? = null

        fun getInstance(context: Context): TripFuelRepository =
            instance ?: synchronized(this) {
                instance ?: TripFuelRepository(context.applicationContext).also { instance = it }
            }

        fun playTactileBeep() {
            // Safe no-op
        }

        fun playConfirmBeep() {
            // Safe no-op
        }
    }
}
