package com.hoopoe.launcher.util

import com.hoopoe.launcher.data.AsrCalcMethod
import com.hoopoe.launcher.data.PrayerCalcMethod
import java.util.*
import kotlin.math.*

data class PrayerTimes(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val nextPrayerName: String,
    val nextPrayerTime: String,
    val minutesToNextPrayer: Long
)

/**
 * Offline Astronomical Islamic Prayer Times Calculator.
 * Operates 100% offline using standard Jean Meeus solar equations.
 */
object PrayerTimeCalculator {

    private const val KAABA_LAT = 21.4225241
    private const val KAABA_LON = 39.8261818

    fun calculateQiblaBearing(latitude: Double, longitude: Double): Float {
        val lat1Rad = Math.toRadians(latitude)
        val lat2Rad = Math.toRadians(KAABA_LAT)
        val dLonRad = Math.toRadians(KAABA_LON - longitude)

        val y = sin(dLonRad)
        val x = cos(lat1Rad) * tan(lat2Rad) - sin(lat1Rad) * cos(dLonRad)
        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360.0) % 360.0
        return bearing.toFloat()
    }

    fun calculateTimes(
        latitude: Double,
        longitude: Double,
        calendar: Calendar = Calendar.getInstance(),
        method: PrayerCalcMethod = PrayerCalcMethod.MUSLIM_WORLD_LEAGUE,
        asrMethod: AsrCalcMethod = AsrCalcMethod.SHAFI
    ): PrayerTimes {
        val timeZoneOffsetHours = (calendar.timeZone.rawOffset +
                if (calendar.timeZone.inDaylightTime(calendar.time)) calendar.timeZone.dstSavings else 0) / 3600000.0

        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        // Julian date
        val d = dayOfYear - 1.0

        // Approximate solar parameters
        val gamma = 2.0 * Math.PI / 365.0 * (d + (12.0 - longitude / 15.0) / 24.0)

        // Equation of time in minutes
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2.0 * gamma) - 0.040849 * sin(2.0 * gamma))

        // Solar declination in radians
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
                0.006758 * cos(2.0 * gamma) + 0.000907 * sin(2.0 * gamma) -
                0.002697 * cos(3.0 * gamma) + 0.00148 * sin(3.0 * gamma)

        val latRad = Math.toRadians(latitude)

        // Solar noon in hours (local time)
        val noonMinutes = 720.0 - (4.0 * longitude) - eqTime + (timeZoneOffsetHours * 60.0)
        val solarNoonHours = noonMinutes / 60.0

        fun hourAngle(zenithDeg: Double): Double {
            val zenithRad = Math.toRadians(zenithDeg)
            val cosHa = (cos(zenithRad) - sin(latRad) * sin(decl)) / (cos(latRad) * cos(decl))
            if (cosHa < -1.0) return 180.0 // Sun never sets (polar day)
            if (cosHa > 1.0) return 0.0   // Sun never rises (polar night)
            return Math.toDegrees(acos(cosHa))
        }

        // Zenith angles
        val fajrAngle = when (method) {
            PrayerCalcMethod.MUSLIM_WORLD_LEAGUE -> 18.0
            PrayerCalcMethod.ISNA -> 15.0
            PrayerCalcMethod.EGYPT -> 19.5
            PrayerCalcMethod.MAKKAH -> 18.5
            PrayerCalcMethod.KARACHI -> 18.0
        }

        val ishaAngle = when (method) {
            PrayerCalcMethod.MUSLIM_WORLD_LEAGUE -> 17.0
            PrayerCalcMethod.ISNA -> 15.0
            PrayerCalcMethod.EGYPT -> 17.5
            PrayerCalcMethod.MAKKAH -> 18.0 // Note: Makkah uses fixed 90 min after Maghrib
            PrayerCalcMethod.KARACHI -> 18.0
        }

        val sunriseHourAngle = hourAngle(90.8333) // Standard refraction
        val fajrHourAngle = hourAngle(90.0 + fajrAngle)

        // Asr angle based on juristic shadow factor
        val shadowFactor = if (asrMethod == AsrCalcMethod.HANAFI) 2.0 else 1.0
        val asrZenithRad = atan(shadowFactor + tan(abs(latRad - decl)))
        val asrHourAngle = hourAngle(Math.toDegrees(asrZenithRad))

        val fajrTime = solarNoonHours - (fajrHourAngle / 15.0)
        val sunriseTime = solarNoonHours - (sunriseHourAngle / 15.0)
        val dhuhrTime = solarNoonHours + (2.0 / 60.0) // 2 mins after solar noon as safety margin
        val asrTime = solarNoonHours + (asrHourAngle / 15.0)
        val maghribTime = solarNoonHours + (sunriseHourAngle / 15.0) + (2.0 / 60.0)

        val ishaTime = if (method == PrayerCalcMethod.MAKKAH) {
            maghribTime + 1.5 // 90 minutes after Maghrib
        } else {
            val ishaHA = hourAngle(90.0 + ishaAngle)
            solarNoonHours + (ishaHA / 15.0)
        }

        fun formatHours(h: Double): String {
            val normalized = (h + 24.0) % 24.0
            val hours = normalized.toInt()
            val mins = ((normalized - hours) * 60.0).roundToInt().coerceIn(0, 59)
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hours)
                set(Calendar.MINUTE, mins)
            }
            val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
            val ampm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            return String.format(Locale.US, "%02d:%02d %s", hour12, mins, ampm)
        }

        fun toMinutes(h: Double): Int {
            val norm = (h + 24.0) % 24.0
            return (norm * 60.0).toInt()
        }

        val nowMins = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val prayerSchedule = listOf(
            "Fajr" to fajrTime,
            "Sunrise" to sunriseTime,
            "Dhuhr" to dhuhrTime,
            "Asr" to asrTime,
            "Maghrib" to maghribTime,
            "Isha" to ishaTime
        )

        var nextName = "Fajr"
        var nextTimeHours = fajrTime
        var minsRemaining: Long = 0

        val upcoming = prayerSchedule.firstOrNull { toMinutes(it.second) > nowMins }
        if (upcoming != null) {
            nextName = upcoming.first
            nextTimeHours = upcoming.second
            minsRemaining = (toMinutes(nextTimeHours) - nowMins).toLong()
        } else {
            // Next is tomorrow's Fajr
            nextName = "Fajr"
            nextTimeHours = fajrTime
            minsRemaining = ((1440 - nowMins) + toMinutes(fajrTime)).toLong()
        }

        return PrayerTimes(
            fajr = formatHours(fajrTime),
            sunrise = formatHours(sunriseTime),
            dhuhr = formatHours(dhuhrTime),
            asr = formatHours(asrTime),
            maghrib = formatHours(maghribTime),
            isha = formatHours(ishaTime),
            nextPrayerName = nextName,
            nextPrayerTime = formatHours(nextTimeHours),
            minutesToNextPrayer = minsRemaining
        )
    }
}
