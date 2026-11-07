package com.hoopoe.launcher.ui.screen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hoopoe.launcher.service.MediaListenerService

private data class PermissionItem(
    val title: String,
    val purpose: String,
    val isGranted: Boolean,
    val isOptional: Boolean,
    val icon: ImageVector,
    val onAction: () -> Unit
)

@Composable
fun PermissionsScreen(
    accent: Color,
    isDayMode: Boolean = false,
    onBackToCockpit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refreshCount by remember { mutableIntStateOf(0) }

    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF0F141F).copy(alpha = 0.85f)

    fun hasPerm(perm: String): Boolean =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    val hasLocation = remember(refreshCount) {
        hasPerm(android.Manifest.permission.ACCESS_FINE_LOCATION) || hasPerm(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    val hasBluetooth = remember(refreshCount) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPerm(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPerm(android.Manifest.permission.BLUETOOTH)
        }
    }

    val hasMic = remember(refreshCount) {
        hasPerm(android.Manifest.permission.RECORD_AUDIO)
    }

    val isMediaConnected by MediaListenerService.isConnected.collectAsState()

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshCount++ }

    val permissionList = listOf(
        PermissionItem(
            "GPS Satellite Location",
            "Powers digital speedometer, compass heading, dynamic weather, and astronomical prayer calculations.",
            hasLocation,
            false,
            Icons.Default.LocationOn
        ) {
            permLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        PermissionItem(
            "Notification Listener (MediaSession)",
            "Required for Now Playing widget to extract track title, artist, and album art from Spotify, VLC, and local radio.",
            isMediaConnected,
            false,
            Icons.Default.NotificationsActive
        ) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { context.startActivity(intent) }
        },
        PermissionItem(
            "Bluetooth & Nearby Devices",
            "Optional: Enables wireless communication with ELM327 OBD-II diagnostic adapters and TPMS receivers.",
            hasBluetooth,
            true,
            Icons.Default.Bluetooth
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            } else {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching { context.startActivity(intent) }
            }
        },
        PermissionItem(
            "Microphone (Push-to-Talk)",
            "Optional: Enables hands-free in-car voice commands when tapping the Assistant Ring.",
            hasMic,
            true,
            Icons.Default.Mic
        ) {
            permLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
        }
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
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
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
            Spacer(Modifier.width(8.dp))
            Text("PERMISSIONS & DEVICE COMPATIBILITY PROFILE", color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Permissions List
            LazyColumn(
                modifier = Modifier.weight(1.1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(permissionList) { perm ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable { perm.onAction() }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (perm.isGranted) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFFFF9100).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(perm.icon, null, tint = if (perm.isGranted) Color(0xFF00E676) else Color(0xFFFF9100), modifier = Modifier.size(18.dp))
                                }

                                Column {
                                    Text(perm.title, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(perm.purpose, color = contentColor.copy(alpha = 0.5f), fontSize = 9.sp)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (perm.isGranted) Color(0xFF00E676).copy(alpha = 0.15f) else accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (perm.isGranted) "ACTIVE" else "GRANT",
                                    color = if (perm.isGranted) Color(0xFF00E676) else accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Right: Hardware Compatibility Profile Card
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("HARDWARE INTEGRITY ASSURANCE", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "• Standalone Application: Does not replace default home launcher.\n" +
                               "• 360° Camera Safe: Zero interference with factory reverse gear video switch.\n" +
                               "• 2 GB RAM Tuning: Clamped tile memory, zero CPU polling leaks.\n" +
                               "• Non-Root Architecture: Zero modification to system firmware or partitions.\n" +
                               "• Offline-First: GPS gauges and prayer calculations function with zero internet.",
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OPEN APP SYSTEM PERMISSIONS", color = contentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
