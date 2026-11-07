package com.hoopoe.launcher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.integration.tpms.TpmsManager
import com.hoopoe.launcher.integration.tpms.TyreStatus

@Composable
fun TpmsScreen(
    accent: Color,
    isDayMode: Boolean = false,
    onBackToCockpit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tpmsState by TpmsManager.tpmsState.collectAsState()

    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF0F141F).copy(alpha = 0.85f)

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
                Text("TYRE PRESSURE MONITORING SYSTEM (TPMS)", color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { TpmsManager.calibrateSensors() },
                    colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CALIBRATE", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { TpmsManager.toggleHardwareConnection() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (tpmsState.isHardwareConnected) Color(0xFF00E676) else accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (tpmsState.isHardwareConnected) "RECEIVER SYNCED" else "SYNC RECEIVER",
                        color = if (tpmsState.isHardwareConnected) Color(0xFF07090E) else Color(0xFF07090E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Status banner
        Text(
            text = tpmsState.statusSummary,
            color = if (tpmsState.isHardwareConnected) Color(0xFF00E676) else Color(0xFFFF9100),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )

        // 4-Wheel Vehicle Graphic & Cards
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Wheels (FL & RL)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TyreCard(tpmsState.frontLeft, accent, cardBg, cardBorder, contentColor)
                TyreCard(tpmsState.rearLeft, accent, cardBg, cardBorder, contentColor)
            }

            // Center Vehicle Silhouette representation
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, cardBorder, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Car Silhouette",
                        tint = accent.copy(alpha = 0.7f),
                        modifier = Modifier.size(90.dp)
                    )
                    Text("DZIRE 2020", color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("NOMINAL: 32-34 PSI", color = contentColor.copy(alpha = 0.5f), fontSize = 9.sp)
                }
            }

            // Right Wheels (FR & RR)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TyreCard(tpmsState.frontRight, accent, cardBg, cardBorder, contentColor)
                TyreCard(tpmsState.rearRight, accent, cardBg, cardBorder, contentColor)
            }
        }
    }
}

@Composable
private fun TyreCard(
    tyre: TyreStatus,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tyre.position.uppercase(), color = contentColor.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (tyre.isAlert) Color(0xFFFF1744) else Color(0xFF00E676))
                )
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", tyre.pressurePsi),
                    color = contentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text("PSI", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 3.dp))
                Spacer(Modifier.width(8.dp))
                Text("${tyre.temperatureC} °C", color = contentColor.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}
