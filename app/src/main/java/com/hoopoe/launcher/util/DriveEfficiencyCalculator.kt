package com.hoopoe.launcher.util

enum class DynamicDriveMode(val label: String, val badgeColorHex: Long) {
    IDLE("IDLE", 0xFF64748B),
    ECO("ECO", 0xFF00E676),
    PILOT("PILOT", 0xFF00E5FF),
    SPORT("SPORT", 0xFFFF1744)
}

/**
 * Standard automotive fuel types with official IPCC / DEFRA / EPA CO2 emission factors.
 */
enum class FuelType(
    val code: String,
    val displayName: String,
    val emissionFactorGramsPerUnit: Double, // grams of CO2 released per unit (L or kg)
    val defaultMileage: Double,             // nominal economy in km/unit
    val unit: String,                       // "L", "kg", "kWh"
    val consumptionRateUnit: String         // "L/h", "kg/h", "kW"
) {
    PETROL("PETROL", "Petrol (Gasoline)", 2310.0, 19.0, "L", "L/h"),      // Standard 2.31 kg CO2 / L
    DIESEL("DIESEL", "Diesel", 2680.0, 23.0, "L", "L/h"),                  // Standard 2.68 kg CO2 / L
    CNG("CNG", "CNG (Natural Gas)", 2750.0, 28.0, "kg", "kg/h"),          // Standard 2.75 kg CO2 / kg
    LPG("LPG", "LPG (Autogas)", 1510.0, 15.0, "L", "L/h"),                 // Standard 1.51 kg CO2 / L
    ELECTRIC("ELECTRIC", "Electric (EV)", 0.0, 7.5, "kWh", "kW");          // 0 direct tailpipe emission

    companion object {
        fun fromCode(code: String): FuelType {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: PETROL
        }
    }
}

data class DriveEfficiencyMetrics(
    val mode: DynamicDriveMode,
    val co2Display: String,
    val co2ValueGramsPerKm: Int?,
    val efficiencyAdvice: String
)

/**
 * DriveEfficiencyCalculator:
 * Uses standard automotive emission formulas:
 *   Nominal CO2 (g/km) = Emission Factor (g/unit) / Vehicle Mileage (km/unit)
 *
 * Speeds between 35-70 km/h with smooth throttle output achieve peak efficiency
 * (ECO mode, multiplier ~1.0x).
 */
object DriveEfficiencyCalculator {

    /**
     * Calculates real-time drive efficiency and CO2 emissions based on standard formula.
     *
     * @param speedKmh Current vehicle speed from GPS or OBD-II
     * @param accelerationMps2 Acceleration rate in m/s²
     * @param fuelType User-selected vehicle fuel type (default: Petrol)
     * @param vehicleMileage Vehicle mileage in km/L (or km/kg for CNG)
     */
    fun calculate(
        speedKmh: Float,
        accelerationMps2: Float = 0f,
        fuelType: FuelType = FuelType.PETROL,
        vehicleMileage: Double = 19.0
    ): DriveEfficiencyMetrics {
        if (fuelType == FuelType.ELECTRIC) {
            val mode = if (speedKmh <= 1.0f) DynamicDriveMode.IDLE
            else if (speedKmh > 90f || accelerationMps2 > 1.2f) DynamicDriveMode.SPORT
            else if (speedKmh > 70f) DynamicDriveMode.PILOT
            else DynamicDriveMode.ECO

            return DriveEfficiencyMetrics(
                mode = mode,
                co2Display = "EV (0 g/km)",
                co2ValueGramsPerKm = 0,
                efficiencyAdvice = "Zero tailpipe emissions • Clean energy"
            )
        }

        // Nominal baseline CO2 based on user-input vehicle mileage:
        // CO2 (g/km) = emissionFactor (g/L) / mileage (km/L)
        val validMileage = maxOf(5.0, vehicleMileage)
        val nominalCo2PerKm = fuelType.emissionFactorGramsPerUnit / validMileage

        if (speedKmh <= 1.0f) {
            val idleRateStr = when (fuelType) {
                FuelType.CNG -> "IDLE (0.5 kg/h)"
                else -> "IDLE (0.6 L/h)"
            }
            return DriveEfficiencyMetrics(
                mode = DynamicDriveMode.IDLE,
                co2Display = idleRateStr,
                co2ValueGramsPerKm = null,
                efficiencyAdvice = "Engine idling • Minimal fuel consumption"
            )
        }

        return when {
            // High speed or aggressive acceleration (> 1.2 m/s²)
            speedKmh > 90f || accelerationMps2 > 1.2f -> {
                val speedFactor = 1.35 + ((speedKmh - 90f) * 0.01) + (maxOf(0f, accelerationMps2) * 0.12)
                val co2 = (nominalCo2PerKm * speedFactor).toInt().coerceIn(120, 350)
                DriveEfficiencyMetrics(
                    mode = DynamicDriveMode.SPORT,
                    co2Display = "CO2 ~$co2 g/km",
                    co2ValueGramsPerKm = co2,
                    efficiencyAdvice = "High engine load • Increased emission"
                )
            }
            // Highway Cruising Band (70 to 90 km/h steady)
            speedKmh > 70f -> {
                val speedFactor = 1.12 + ((speedKmh - 70f) * 0.008)
                val co2 = (nominalCo2PerKm * speedFactor).toInt().coerceIn(100, 250)
                DriveEfficiencyMetrics(
                    mode = DynamicDriveMode.PILOT,
                    co2Display = "CO2 ~$co2 g/km",
                    co2ValueGramsPerKm = co2,
                    efficiencyAdvice = "Highway cruise • Steady aerodynamic efficiency"
                )
            }
            // Optimal Thermal Efficiency Band (35 to 70 km/h)
            speedKmh in 35f..70f && accelerationMps2 <= 1.0f -> {
                val deviation = kotlin.math.abs(speedKmh - 50f) * 0.003
                val speedFactor = 0.96 + deviation
                val co2 = (nominalCo2PerKm * speedFactor).toInt().coerceIn(80, 220)
                DriveEfficiencyMetrics(
                    mode = DynamicDriveMode.ECO,
                    co2Display = "CO2 ~$co2 g/km",
                    co2ValueGramsPerKm = co2,
                    efficiencyAdvice = "Optimal ECO band • Lowest carbon footprint"
                )
            }
            // City stop-and-go lower speed band (1 to 35 km/h)
            else -> {
                val crawlFactor = 1.15 + ((35f - speedKmh) * 0.008)
                val co2 = (nominalCo2PerKm * crawlFactor).toInt().coerceIn(90, 260)
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
