package com.hoopoe.launcher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.data.TripFuelRepository
import com.hoopoe.launcher.integration.obd.ObdManager
import com.hoopoe.launcher.util.LocationData
import kotlinx.coroutines.flow.StateFlow

private data class VehicleParamCard(
    val title: String,
    val value: String,
    val unit: String,
    val source: String, // "GPS" or "ECU/OBD"
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun VehicleInfoScreen(
    location: LocationData?,
    accent: Color,
    isDayMode: Boolean = false,
    onBackToCockpit: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tripRepo = remember { TripFuelRepository.getInstance(context) }
    val trip by tripRepo.currentTrip.collectAsState()
    val odoKm by tripRepo.odometerKm.collectAsState()
    val fuelPct by tripRepo.fuelPercent.collectAsState()

    val obdManager = remember { ObdManager.getInstance(context) }
    val obdTelemetry by obdManager.telemetry.collectAsState()

    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF0F141F).copy(alpha = 0.85f)

    val currentSpeedKmh = ((location?.speedMps ?: 0f) * 3.6f).toInt()
    val altitudeM = location?.altitude?.toInt() ?: 0

    val odoDisplay = if (odoKm != null) String.format(java.util.Locale.US, "%.1f", odoKm!!) else "NA"
    val fuelDisplay = if (obdTelemetry.fuelLevelPct != null) {
        String.format(java.util.Locale.US, "%.0f", obdTelemetry.fuelLevelPct!!)
    } else if (fuelPct != null) {
        String.format(java.util.Locale.US, "%.0f", fuelPct!!)
    } else {
        "NA"
    }
    val fuelSource = if (obdTelemetry.fuelLevelPct != null) "ECU OBD" else if (fuelPct != null) "LOGGED" else "NA"
    val batteryDisplay = if (obdTelemetry.batteryVoltage != null && obdTelemetry.batteryVoltage!! > 0f) {
        String.format(java.util.Locale.US, "%.1f", obdTelemetry.batteryVoltage!!)
    } else {
        "NA"
    }
    val coolantDisplay = if (obdTelemetry.coolantTempC != null && obdTelemetry.coolantTempC!! > 0) {
        "${obdTelemetry.coolantTempC}"
    } else {
        "NA"
    }

    val vehicleCards = listOf(
        VehicleParamCard("VEHICLE SPEED", currentSpeedKmh.toString(), "KM/H", "GPS", Icons.Default.Speed, accent),
        VehicleParamCard("TOTAL ODOMETER", odoDisplay, "KM", if (odoKm != null) "TRIP METER" else "NA", Icons.Default.LinearScale, Color(0xFF64B5F6)),
        VehicleParamCard("TRIP DISTANCE", String.format(java.util.Locale.US, "%.1f", trip.distanceKm), "KM", "GPS", Icons.Default.Route, Color(0xFF00E676)),
        VehicleParamCard("AVERAGE SPEED", String.format(java.util.Locale.US, "%.1f", trip.avgSpeedKmh), "KM/H", "GPS", Icons.Default.Timelapse, Color(0xFFFF9100)),
        VehicleParamCard("FUEL REMAINING", fuelDisplay, "%", fuelSource, Icons.Default.LocalGasStation, Color(0xFFFFB300)),
        VehicleParamCard("ALTITUDE (SEA LEVEL)", altitudeM.toString(), "METERS", "GPS NMEA", Icons.Default.Landscape, Color(0xFFB388FF)),
        VehicleParamCard("BATTERY VOLTAGE", batteryDisplay, "VOLTS", if (obdTelemetry.batteryVoltage != null) "ECU OBD" else "NA", Icons.Default.BatteryChargingFull, Color(0xFF00E5FF)),
        VehicleParamCard("COOLANT TEMP", coolantDisplay, "°C", if (obdTelemetry.coolantTempC != null) "ECU OBD" else "NA", Icons.Default.Thermostat, Color(0xFFFF5252))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f))
                        .clickable { onBackToCockpit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = accent, modifier = Modifier.size(18.dp))
                }
                Text("VEHICLE TELEMETRY & COMPUTER", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onOpenDiagnostics() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Troubleshoot, null, tint = accent, modifier = Modifier.size(14.dp))
                    Text("LIVE OBD-II DIAGNOSTICS", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Subheader disclaimer ensuring zero fabrication
        Text(
            text = "GPS-derived metrics are calculated directly from satellite triangulation. ECU parameters reflect real-time live data via connected OBD-II hardware.",
            color = contentColor.copy(alpha = 0.5f),
            fontSize = 9.sp
        )

        // Telemetry Grid (4x2 cards)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(vehicleCards) { card ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(card.title, color = contentColor.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(card.accentColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(card.source, color = card.accentColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = card.value,
                                color = contentColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = card.unit,
                                color = card.accentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
