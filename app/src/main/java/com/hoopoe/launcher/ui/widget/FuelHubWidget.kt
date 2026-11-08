package com.hoopoe.launcher.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.data.TripFuelRepository
import com.hoopoe.launcher.data.TripLogManager
import com.hoopoe.launcher.data.SettingsRepository
import com.hoopoe.launcher.util.FuelType
import java.util.Locale

@Composable
fun FuelHubWidget(
    accent: Color,
    isDayMode: Boolean = false,
    onOpenRefuel: () -> Unit,
    onOpenMileageSetup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { TripFuelRepository.getInstance(context) }
    val tripLogManager = remember { TripLogManager.getInstance(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settingsFlow.collectAsState(initial = com.hoopoe.launcher.data.AppSettings())

    val isObdOnline by repo.isObdOnline.collectAsState()
    val fuelPct by repo.fuelPercent.collectAsState()
    val trip by repo.currentTrip.collectAsState()
    val odoKm by repo.odometerKm.collectAsState()
    val fuelPrice by repo.fuelPricePerLiter.collectAsState()

    // Real-time trip session (turn on to turn off)
    val activeTripDistKm by tripLogManager.currentActiveRideDistance.collectAsState()
    val activeTripDurationMins by tripLogManager.currentTripDurationMins.collectAsState()
    val activeTripCo2Grams by tripLogManager.currentTripCo2Grams.collectAsState()

    var activeTab by remember { mutableStateOf("LIVE_TRIP") } // "LIVE_TRIP" or "FUEL_MGR"
    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF0F141F).copy(alpha = 0.85f)

    val currentFuelType = FuelType.fromCode(settings.fuelType)
    val currentMileage = settings.vehicleMileageKmL.toDouble()

    // Authentic tank calculations (Dzire 2020: 37.0 Liters capacity)
    val remainingLiters = fuelPct?.let { (it / 100f) * 37.0f }
    val estRangeKm = if (remainingLiters != null && currentMileage > 0.0) (remainingLiters * currentMileage).toInt() else null
    val tripCost = if (currentMileage > 0.0) (activeTripDistKm / currentMileage) * fuelPrice else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header & Tab Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (activeTab == "LIVE_TRIP") Icons.Default.Route else Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "TRIP & FUEL HUB",
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Tabs: LIVE TRIP | FUEL MGR
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("LIVE_TRIP" to "CURRENT RIDE", "FUEL_MGR" to "FUEL & CO2").forEach { (key, label) ->
                    val isSelected = activeTab == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) accent else Color.Transparent)
                            .clickable {
                                TripFuelRepository.playTactileBeep()
                                activeTab = key
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF07090E) else contentColor.copy(alpha = 0.6f),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Center Content Body
        if (activeTab == "LIVE_TRIP") {
            // ── Current Ride View ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Current Trip Distance (Calculated between turn on & turn off)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text("RIDE DISTANCE", color = contentColor.copy(alpha = 0.45f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", activeTripDistKm),
                                    color = contentColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("KM", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Economy / Mileage
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .clickable { onOpenMileageSetup() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CAR MILEAGE", color = contentColor.copy(alpha = 0.45f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.Tune, null, tint = accent, modifier = Modifier.size(11.dp))
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", currentMileage),
                                    color = Color(0xFF00E676),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("KM/${currentFuelType.unit}", color = contentColor.copy(alpha = 0.6f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Estimated Trip CO2 Output (Standard formula)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text("TRIP CO2 OUTPUT", color = contentColor.copy(alpha = 0.45f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                val co2Kg = activeTripCo2Grams / 1000.0
                                Text(
                                    text = if (co2Kg < 10.0) String.format(Locale.US, "%.2f", co2Kg) else String.format(Locale.US, "%.1f", co2Kg),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("KG CO2", color = contentColor.copy(alpha = 0.6f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Trip Duration
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text("RIDE DURATION", color = contentColor.copy(alpha = 0.45f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$activeTripDurationMins",
                                    color = accent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("MIN", color = contentColor.copy(alpha = 0.6f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // ── Fuel Manager View ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Tank Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${currentFuelType.code} TANK", color = contentColor.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        val tankPctText = fuelPct?.let { "%.0f%%".format(it) } ?: "NA"
                        Text(
                            text = tankPctText,
                            color = if (fuelPct != null && fuelPct!! < 20f) Color(0xFFFF9100) else accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Visual Tank Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        if (fuelPct != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = (fuelPct!! / 100f).coerceIn(0.05f, 1f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(accent, if (fuelPct!! < 20f) Color(0xFFFF9100) else Color(0xFF00E676))
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val litersStr = remainingLiters?.let { "%.1f L / 37.0 L".format(it) } ?: "NA / 37.0 L"
                        val rangeStr = estRangeKm?.let { "EST. RANGE $it KM" } ?: "RANGE: NA"
                        Text(
                            text = litersStr,
                            color = contentColor.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = rangeStr,
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Dual Action Buttons: Refuel Log + Mileage / Fuel Type Setup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            TripFuelRepository.playTactileBeep()
                            onOpenRefuel()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF07090E), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "LOG REFUEL",
                            color = Color(0xFF07090E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            TripFuelRepository.playTactileBeep()
                            onOpenMileageSetup()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Tune, null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "MILEAGE",
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom Sub-Footer: Connection Source & Odometer
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val odoDisplay = when {
                odoKm != null -> "ODO: %.0f KM".format(odoKm)
                settings.manualOdometerKm > 0f -> "ODO: %.0f KM (USER)".format(settings.manualOdometerKm)
                else -> "ODO: NA"
            }
            Text(
                text = "FUEL: ${currentFuelType.code} • ${String.format(Locale.US, "%.1f", currentMileage)} KM/${currentFuelType.unit}",
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = odoDisplay,
                color = accent,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
