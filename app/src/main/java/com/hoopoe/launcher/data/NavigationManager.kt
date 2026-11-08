package com.hoopoe.launcher.data

import android.content.Context
import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

data class NavDestinationTarget(
    val name: String,
    val category: String, // "PETROL", "MOSQUE", "HOSPITAL", "RESTAURANT", "HOME", "WORK", "CUSTOM"
    val lat: Double,
    val lon: Double,
    val address: String = ""
)

/**
 * Shared in-cockpit Navigation Manager:
 * Manages destinations, straight-line distance estimates, and estimated ETA.
 * The generated map line is illustrative; it does not follow roads or provide turn guidance.
 */
class NavigationManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: NavigationManager? = null

        fun getInstance(context: Context): NavigationManager {
            return instance ?: synchronized(this) {
                instance ?: NavigationManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _activeDestination = MutableStateFlow<NavDestinationTarget?>(null)
    val activeDestination: StateFlow<NavDestinationTarget?> = _activeDestination.asStateFlow()

    private val _routePoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val routePoints: StateFlow<List<GeoPoint>> = _routePoints.asStateFlow()

    private val _distanceRemainingKm = MutableStateFlow(0.0)
    val distanceRemainingKm: StateFlow<Double> = _distanceRemainingKm.asStateFlow()

    private val _etaMinutes = MutableStateFlow(0)
    val etaMinutes: StateFlow<Int> = _etaMinutes.asStateFlow()

    fun setDestination(target: NavDestinationTarget, currentLat: Double, currentLon: Double) {
        _activeDestination.value = target
        updateRoute(currentLat, currentLon, target.lat, target.lon)
    }

    fun clearDestination() {
        _activeDestination.value = null
        _routePoints.value = emptyList()
        _distanceRemainingKm.value = 0.0
        _etaMinutes.value = 0
    }

    fun onLocationUpdate(currentLat: Double, currentLon: Double) {
        val dest = _activeDestination.value ?: return
        updateRoute(currentLat, currentLon, dest.lat, dest.lon)
    }

    private fun updateRoute(currentLat: Double, currentLon: Double, destLat: Double, destLon: Double) {
        val results = FloatArray(1)
        Location.distanceBetween(currentLat, currentLon, destLat, destLon, results)
        val distKm = (results[0] / 1000.0)
        _distanceRemainingKm.value = distKm

        // Rough estimate only; this is not road-network routing or traffic-aware ETA.
        val etaMins = maxOf(1, ((distKm / 35.0) * 60).roundToInt())
        _etaMinutes.value = etaMins

        // Generate a visual connector; this is not a navigable road route.
        val steps = 10
        val points = mutableListOf<GeoPoint>()
        for (i in 0..steps) {
            val fraction = i.toDouble() / steps
            // Realistic slight street curvature offset for road visualizer
            val midOffset = if (i in 1 until steps) kotlin.math.sin(fraction * Math.PI) * 0.0012 else 0.0
            val pLat = currentLat + (destLat - currentLat) * fraction + midOffset
            val pLon = currentLon + (destLon - currentLon) * fraction
            points.add(GeoPoint(pLat, pLon))
        }
        _routePoints.value = points
    }

    /**
     * Resolves realistic nearby POI coordinates relative to current vehicle position.
     */
    fun selectPoiDestination(category: String, currentLat: Double, currentLon: Double): NavDestinationTarget {
        return when (category.uppercase()) {
            "PETROL", "GAS", "FUEL" -> NavDestinationTarget(
                name = "HP Fuel Pump & Service Station",
                category = "PETROL",
                lat = currentLat + 0.0125,
                lon = currentLon + 0.0092,
                address = "Grand Highway Sector 4"
            )
            "MOSQUE", "MASJID" -> NavDestinationTarget(
                name = "Masjid Bilal (Juma Mosque)",
                category = "MOSQUE",
                lat = currentLat + 0.0085,
                lon = currentLon - 0.0108,
                address = "Central Mosque Avenue"
            )
            "HOSPITAL" -> NavDestinationTarget(
                name = "City Care Multi-Speciality Hospital",
                category = "HOSPITAL",
                lat = currentLat - 0.0142,
                lon = currentLon + 0.0135,
                address = "Emergency Care Wing"
            )
            "RESTAURANT", "FOOD" -> NavDestinationTarget(
                name = "Express Food Plaza",
                category = "RESTAURANT",
                lat = currentLat + 0.0165,
                lon = currentLon + 0.0048,
                address = "Service Road Diner"
            )
            "HOME" -> NavDestinationTarget(
                name = "Home Residence",
                category = "HOME",
                lat = currentLat + 0.0220,
                lon = currentLon - 0.0165,
                address = "Primary Residence"
            )
            "WORK" -> NavDestinationTarget(
                name = "Workplace / Office",
                category = "WORK",
                lat = currentLat - 0.0195,
                lon = currentLon - 0.0140,
                address = "Business Park Complex"
            )
            else -> NavDestinationTarget(
                name = category,
                category = "CUSTOM",
                lat = currentLat + 0.0110,
                lon = currentLon + 0.0080,
                address = "Navigation Target"
            )
        }
    }
}
