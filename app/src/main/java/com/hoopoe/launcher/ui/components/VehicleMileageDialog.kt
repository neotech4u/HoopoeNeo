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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hoopoe.launcher.util.FuelType
import java.util.Locale

@Composable
fun VehicleMileageDialog(
    currentFuelType: String,
    currentMileageKmL: Float,
    currentOdoKm: Float?,
    accent: Color,
    isDayMode: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (fuelType: String, mileage: Float, manualOdo: Float?) -> Unit
) {
    var selectedFuelType by remember { mutableStateOf(FuelType.fromCode(currentFuelType)) }
    var mileageValue by remember { mutableDoubleStateOf(currentMileageKmL.toDouble().coerceIn(5.0, 60.0)) }
    var odometerText by remember { mutableStateOf(currentOdoKm?.let { String.format(Locale.US, "%.0f", it) } ?: "") }

    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFF8FAFC) else Color(0xFF0D121D)

    // Standard formula calculation preview:
    val theoreticalCo2 = if (selectedFuelType == FuelType.ELECTRIC) 0
    else (selectedFuelType.emissionFactorGramsPerUnit / mileageValue).toInt()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Speed, null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "VEHICLE FUEL & MILEAGE SETUP",
                                color = contentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Standard emission release formula & manual OBD fallback",
                                color = contentColor.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = contentColor.copy(alpha = 0.6f))
                    }
                }

                HorizontalDivider(color = cardBorder)

                // ── 1. SELECT FUEL TYPE ──────────────────────────────────────────
                Text(
                    text = "SELECT VEHICLE FUEL TYPE",
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FuelType.entries.forEach { fuel ->
                        val isSelected = selectedFuelType == fuel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) accent.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f))
                                .border(1.dp, if (isSelected) accent else cardBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedFuelType = fuel
                                    mileageValue = fuel.defaultMileage
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (fuel == FuelType.ELECTRIC) Icons.Default.ElectricCar else Icons.Default.LocalGasStation,
                                    contentDescription = null,
                                    tint = if (isSelected) accent else contentColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = fuel.code,
                                    color = if (isSelected) accent else contentColor.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ── 2. INPUT VEHICLE MILEAGE ─────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAR FUEL ECONOMY / MILEAGE (${selectedFuelType.unit.let { "KM/$it" }})",
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Dzire 2020 Standard: 19.0 KM/L",
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Big Numeric Display & Steppers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { mileageValue = (mileageValue - 1.0).coerceAtLeast(5.0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("-1.0", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { mileageValue = (mileageValue - 0.1).coerceAtLeast(5.0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("-0.1", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", mileageValue),
                            color = accent,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "KM/${selectedFuelType.unit}",
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { mileageValue = (mileageValue + 0.1).coerceAtMost(60.0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+0.1", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { mileageValue = (mileageValue + 1.0).coerceAtMost(60.0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+1.0", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick Preset Mileage Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15.0, 18.0, 19.0, 22.0, 25.0, 28.0).forEach { preset ->
                        val isPreset = kotlin.math.abs(mileageValue - preset) < 0.05
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPreset) accent.copy(alpha = 0.2f) else cardBg)
                                .border(1.dp, if (isPreset) accent else cardBorder, RoundedCornerShape(8.dp))
                                .clickable { mileageValue = preset }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset.toInt()} km",
                                color = if (isPreset) accent else contentColor.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ── 3. STANDARD FORMULA CO2 EMISSION SUMMARY ─────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.08f))
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "IPCC STANDARD CO2 RELEASE ESTIMATION",
                                color = accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (selectedFuelType == FuelType.ELECTRIC) "0 g/km" else "~$theoreticalCo2 g/km",
                                color = Color(0xFF00E676),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Formula: ${selectedFuelType.emissionFactorGramsPerUnit.toInt()} g CO2/${selectedFuelType.unit} ÷ ${String.format(Locale.US, "%.1f", mileageValue)} km/${selectedFuelType.unit} = $theoreticalCo2 g/km nominal tailpipe emission",
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 8.5.sp
                        )
                    }
                }

                // ── 4. MANUAL ODOMETER FALLBACK (Optional) ────────────────────────
                Text(
                    text = "VEHICLE TOTAL ODOMETER (OPTIONAL OBD FALLBACK)",
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 48500 KM (if OBD odometer not available)", color = contentColor.copy(alpha = 0.4f), fontSize = 11.sp) },
                    trailingIcon = { Text("KM", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 10.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = cardBorder,
                        focusedTextColor = contentColor,
                        unfocusedTextColor = contentColor
                    )
                )

                // ── 5. SAVE BUTTON ────────────────────────────────────────────────
                Button(
                    onClick = {
                        val manualOdo = odometerText.toFloatOrNull()
                        onSave(selectedFuelType.code, mileageValue.toFloat(), manualOdo)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SAVE VEHICLE PROFILE & MILEAGE",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
