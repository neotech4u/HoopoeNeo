package com.hoopoe.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hoopoe.launcher.data.TripFuelRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RefuelDialog(
    accent: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { TripFuelRepository.getInstance(context) }
    val isObdOnline by repo.isObdOnline.collectAsState()
    val defaultPrice by repo.fuelPricePerLiter.collectAsState()
    val currentOdo by repo.odometerKm.collectAsState()
    val refuelList by repo.refuelList.collectAsState()

    var liters by remember { mutableDoubleStateOf(15.0) }
    var pricePerLiter by remember { mutableDoubleStateOf(defaultPrice.toDouble()) }
    var odoReading by remember { mutableDoubleStateOf(currentOdo ?: 48290.0) }
    var isFullTank by remember { mutableStateOf(false) }

    val totalCost = liters * pricePerLiter

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F141F))
                    .border(1.5.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("LOG PETROL REFUEL", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(
                                    if (isObdOnline) "OBD-II LIVE LINK ACTIVE" else "STANDALONE LOG (SAVED FOR HOME ASSISTANT)",
                                    color = if (isObdOnline) accent else Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    HorizontalDivider(color = Color(0xFF243048))

                    // Quick Volume Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("QUICK SELECT VOLUME (LITERS)", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5.0, 10.0, 15.0, 20.0, 25.0, 30.0).forEach { preset ->
                                val isSelected = liters == preset && !isFullTank
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) accent else Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, if (isSelected) accent else Color(0xFF243048), RoundedCornerShape(10.dp))
                                        .clickable {
                                            liters = preset
                                            isFullTank = false
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${preset.toInt()}L",
                                        color = if (isSelected) Color(0xFF07090E) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            // Full Tank Button
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isFullTank) Color(0xFF00E676) else Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, if (isFullTank) Color(0xFF00E676) else Color(0xFF243048), RoundedCornerShape(10.dp))
                                    .clickable {
                                        liters = 37.0 // Dzire 2020 tank capacity
                                        isFullTank = true
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "FULL TANK",
                                    color = if (isFullTank) Color(0xFF07090E) else Color(0xFF00E676),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Stepper Controls Row: Volume & Price
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Liters Stepper
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, Color(0xFF243048), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("FUEL VOLUME", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (liters > 1.0) liters -= 1.0 },
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Icon(Icons.Default.Remove, null, tint = Color.White)
                                    }
                                    Text(
                                        "%.1f L".format(liters),
                                        color = accent,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    IconButton(
                                        onClick = { if (liters < 45.0) liters += 1.0 },
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color.White)
                                    }
                                }
                            }
                        }

                        // Price Stepper
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, Color(0xFF243048), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("PRICE / LITER", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (pricePerLiter > 10.0) pricePerLiter -= 1.0 },
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Icon(Icons.Default.Remove, null, tint = Color.White)
                                    }
                                    Text(
                                        "₹%.2f".format(pricePerLiter),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    IconButton(
                                        onClick = { pricePerLiter += 1.0 },
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Cost Calculation Summary Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF00E676).copy(alpha = 0.10f))
                            .border(1.dp, Color(0xFF00E676).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CALCULATED TOTAL COST", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "₹%.2f".format(totalCost),
                                    color = Color(0xFF00E676),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("ODOMETER", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("%.0f KM".format(odoReading), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Primary Save Button
                    Button(
                        onClick = {
                            if (liters > 0.0) {
                                repo.addRefuel(
                                    liters = liters,
                                    costPerLiter = pricePerLiter,
                                    odometer = odoReading,
                                    isFullTank = isFullTank
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF07090E), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SAVE REFUEL LOG ENTRY", color = Color(0xFF07090E), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }

                    // Recent History Section
                    if (refuelList.isNotEmpty()) {
                        HorizontalDivider(color = Color(0xFF243048))
                        Text("RECENT REFUEL HISTORY", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            refuelList.take(5).forEach { record ->
                                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .border(1.dp, Color(0xFF243048), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${record.liters} L • ₹%.2f".format(record.costTotal), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(dateStr, color = Color(0xFF64748B), fontSize = 10.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("%.0f KM".format(record.odometerKm), color = accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        IconButton(
                                            onClick = { repo.deleteRefuel(record.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
