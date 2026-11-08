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

@Composable
fun FuelHubWidget(
    accent: Color,
    isDayMode: Boolean = false,
    onOpenRefuel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { TripFuelRepository.getInstance(context) }
    val isObdOnline by repo.isObdOnline.collectAsState()
    val fuelPct by repo.fuelPercent.collectAsState()
    val trip by repo.currentTrip.collectAsState()
    val odoKm by repo.odometerKm.collectAsState()
    val fuelPrice by repo.fuelPricePerLiter.collectAsState()

    var activeTab by remember { mutableStateOf("LIVE_TRIP") } // "LIVE_TRIP" or "FUEL_MGR"
    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF0F141F).copy(alpha = 0.85f)

    // Authentic tank calculations (Dzire 2020: 37.0 Liters capacity)
    val remainingLiters = fuelPct?.let { (it / 100f) * 37.0f }
    val estRangeKm = if (remainingLiters != null && trip.economyKmL > 0.0) (remainingLiters * trip.economyKmL).toInt() else null
    val tripCost = if (trip.economyKmL > 0.0) (trip.distanceKm / trip.economyKmL) * fuelPrice else 0.0

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
                    text = "TELEMETRY HUB",
                    color = contentColor.copy(alpha = 0.5f),
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
                listOf("LIVE_TRIP" to "LIVE TRIP", "FUEL_MGR" to "FUEL MGR").forEach { (key, label) ->
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
            // ── Live Trip View ──────────────────────────────────────────
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
                    // Distance
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text("TRIP DISTANCE", color = contentColor.copy(alpha = 0.45f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "%.1f".format(trip.distanceKm),
                                    color = contentColor,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("KM", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Economy
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text("AVG ECONOMY", color = contentColor.copy(alpha = 0.45f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "%.1f".format(trip.economyKmL),
                                    color = Color(0xFF00E676),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("KM/L", color = contentColor.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cost
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text("EST. TRIP COST", color = contentColor.copy(alpha = 0.45f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "₹%.1f".format(tripCost),
                                color = contentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Instant Rate
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text("INSTANT RATE", color = contentColor.copy(alpha = 0.45f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isObdOnline) "OBD LIVE" else "NA",
                                color = accent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
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
                        Text("PETROL TANK", color = contentColor.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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

                // Quick Refuel Trigger Button
                Button(
                    onClick = {
                        TripFuelRepository.playTactileBeep()
                        onOpenRefuel()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF07090E),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "+ QUICK REFUEL",
                        color = Color(0xFF07090E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Bottom Sub-Footer: Connection Source
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SOURCE: ${trip.source}",
                color = contentColor.copy(alpha = 0.4f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ODO: %.0f KM".format(odoKm),
                color = contentColor.copy(alpha = 0.6f),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
