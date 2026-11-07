package com.hoopoe.launcher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.hoopoe.launcher.integration.obd.ObdConnectionState
import com.hoopoe.launcher.integration.obd.ObdManager

@Composable
fun ObdScreen(
    accent: Color,
    isDayMode: Boolean = false,
    onBackToCockpit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val obdManager = remember { ObdManager.getInstance(context) }
    val telemetry by obdManager.telemetry.collectAsState()

    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF0F141F).copy(alpha = 0.85f)

    val isConnected = telemetry.connectionState == ObdConnectionState.CONNECTED

    Row(modifier = modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {

        // Left 50%: Live Gauges
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text("OBD-II DIAGNOSTICS", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                // Connection status chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isConnected) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFFFF9100).copy(alpha = 0.15f))
                        .border(1.dp, if (isConnected) Color(0xFF00E676) else Color(0xFFFF9100), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = telemetry.statusMessage,
                        color = if (isConnected) Color(0xFF00E676) else Color(0xFFFF9100),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Connection action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { if (isConnected) obdManager.disconnect() else obdManager.connect() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isConnected) Color(0xFFFF1744) else accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothDisabled else Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        tint = if (isConnected) Color.White else Color(0xFF07090E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "DISCONNECT" else "CONNECT OBD-II",
                        color = if (isConnected) Color.White else Color(0xFF07090E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { obdManager.requestDtcCodes() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BugReport, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SCAN DTCS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Live Gauges Grid (RPM, Speed, Coolant, Load)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                // RPM
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("ENGINE RPM", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(if (isConnected) "${telemetry.rpm}" else "—", color = accent, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Text("RPM", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = cardBorder)

                // Speed
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("ECU VEHICLE SPEED", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(if (isConnected) "${telemetry.speedKmh}" else "—", color = contentColor, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Text("KM/H", color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = cardBorder)

                // Coolant & Battery
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("COOLANT TEMP", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(if (isConnected) "${telemetry.coolantTempC} °C" else "—", color = Color(0xFFFF9100), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("BATTERY VOLTAGE", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(if (isConnected) "${telemetry.batteryVoltage} V" else "—", color = Color(0xFF00E5FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Right 50%: Diagnostic Trouble Codes (DTC)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DIAGNOSTIC TROUBLE CODES", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
            }

            Text(
                text = "Reads standard Mode 03 OBD-II fault codes from powertrain control module (PCM).",
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 9.sp
            )

            if (telemetry.dtcCodes.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Verified, null, tint = Color(0xFF00E676), modifier = Modifier.size(42.dp))
                        Text("No Active DTC Faults", color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Tap 'SCAN DTCS' to query vehicle ECU", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(telemetry.dtcCodes) { code ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(code, color = if (code.startsWith("P")) Color(0xFFFF5252) else Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
