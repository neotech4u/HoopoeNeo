package com.hoopoe.launcher.util

enum class DynamicDriveMode(val label: String, val badgeColorHex: Long) {
    IDLE("IDLE", 0xFF64748B),
    ECO("ECO", 0xFF00E676),
    PILOT("PILOT", 0xFF00E5FF),
    SPORT("SPORT", 0xFFFF1744)
}

data class DriveEfficiencyMetrics(
    val mode: DynamicDriveMode,
    val co2Display: String,
    val co2ValueGramsPerKm: Int?,
    val efficiencyAdvice: String
)

/**
 * DriveEfficiencyCalculator:
 * Evaluates vehicle speed and acceleration against the BS6 1.2L petrol engine
 * efficiency map of the Maruti Suzuki Dzire 2020.
 *
 * Speeds between 35-70 km/h with smooth throttle output achieve peak thermal
 * efficiency (~20-23 km/L) and minimal CO2 emission (~105-120 g/km).
 */
object DriveEfficiencyCalculator {

    /**
     * @param speedKmh Current speed in km/h from GPS or OBD-II
     * @param accelerationMps2 Acceleration in m/s² (positive = accelerating, negative = braking/coasting)
     */
    fun calculate(speedKmh: Float, accelerationMps2: Float = 0f): DriveEfficiencyMetrics {
        if (speedKmh <= 1.0f) {
            return DriveEfficiencyMetrics(
                mode = DynamicDriveMode.IDLE,
                co2Display = "IDLE (0.6 L/h)",
                co2ValueGramsPerKm = null,
                efficiencyAdvice = "Engine idling • Minimal emission"
            )
        }

        return when {
            // High speed or aggressive acceleration (> 1.2 m/s²)
            speedKmh > 90f || accelerationMps2 > 1.2f -> {
                val co2 = (165 + (speedKmh - 90f) * 1.2f + (accelerationMps2 * 15f)).toInt().coerceIn(155, 230)
                DriveEfficiencyMetrics(
                    mode = DynamicDriveMode.SPORT,
                    co2Display = "CO2 ~$co2 g/km",
                    co2ValueGramsPerKm = co2,
                    efficiencyAdvice = "High engine load • High fuel consumption"
                )
            }
            // Highway Cruising Band (70 to 90 km/h steady)
            speedKmh > 70f -> {
                val co2 = (128 + (speedKmh - 70f) * 1.1f).toInt().coerceIn(125, 155)
                DriveEfficiencyMetrics(
                    mode = DynamicDriveMode.PILOT,
                    co2Display = "CO2 ~$co2 g/km",
                    co2ValueGramsPerKm = co2,
                    efficiencyAdvice = "Highway cruise • Steady aerodynamic efficiency"
                )
            }
            // Optimal Thermal Efficiency Band (30 to 70 km/h)
            speedKmh in 30f..70f && accelerationMps2 <= 1.0f -> {
                val co2 = (108 + (speedKmh - 45f).let { kotlin.math.abs(it) } * 0.4f).toInt().coerceIn(105, 125)
                DriveEfficiencyMetrics(
                    mode = DynamicDriveMode.ECO,
                    co2Display = "CO2 ~$co2 g/km",
                    co2ValueGramsPerKm = co2,
                    efficiencyAdvice = "Optimal efficiency • Lowest CO2 footprint"
                )
            }
            // City stop-and-go lower speed band (1 to 30 km/h)
            else -> {
                val co2 = (135 + (30f - speedKmh) * 0.8f).toInt().coerceIn(125, 165)
                DriveEfficiencyMetrics(
                    mode = DynamicDriveMode.ECO,
                    co2Display = "CO2 ~$co2 g/km",
                    co2ValueGramsPerKm = co2,
                    efficiencyAdvice = "City driving • Maintain smooth throttle"
                )
            }
        }
    }
}
