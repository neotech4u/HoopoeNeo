package com.hoopoe.launcher.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.model.NavDestination

@Composable
fun QuickControlsScreen(
    accent: Color,
    isDayMode: Boolean = false,
    onNavigateDestination: (NavDestination) -> Unit,
    onBackToCockpit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF0F141F).copy(alpha = 0.85f)

    var brightnessSlider by remember { mutableFloatStateOf(0.85f) }

    fun setScreenBrightness(level: Float) {
        val activity = context as? Activity ?: return
        val lp = activity.window.attributes
        lp.screenBrightness = level.coerceIn(0.1f, 1.0f)
        activity.window.attributes = lp
    }

    fun exitToSystemLauncher() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(homeIntent) }
    }

    fun openSystemSettings(action: String) {
        val intent = Intent(action).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

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
                Text("QUICK CONTROLS & SYSTEM DRAWER", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            // High Priority Safe Exit to System Launcher Button
            Button(
                onClick = { exitToSystemLauncher() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("EXIT TO CAR SYSTEM LAUNCHER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Quick Sliders & Grid
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: In-Car Brightness Slider & Sound Controls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SCREEN BRIGHTNESS", color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${(brightnessSlider * 100).toInt()}%", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = brightnessSlider,
                        onValueChange = {
                            brightnessSlider = it
                            setScreenBrightness(it)
                        },
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }

                HorizontalDivider(color = cardBorder)

                // Connectivity Quick Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { openSystemSettings(Settings.ACTION_WIFI_SETTINGS) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Wifi, null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("WI-FI", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Bluetooth, null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("BLUETOOTH", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // System Settings
                Button(
                    onClick = { openSystemSettings(Settings.ACTION_SETTINGS) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Settings, null, tint = contentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ANDROID SYSTEM SETTINGS", color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Right: Navigation Module Shortcuts
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("MODULE SHORTCUTS", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                listOf(
                    Triple("NAVIGATION CENTRE", Icons.Default.Navigation, NavDestination.NAVIGATION_CENTRE),
                    Triple("MEDIA CENTRE", Icons.Default.MusicNote, NavDestination.MEDIA_CENTRE),
                    Triple("ISLAMIC COMPANION", Icons.Default.Mosque, NavDestination.ISLAMIC_COMPANION),
                    Triple("OBD-II DIAGNOSTICS", Icons.Default.Troubleshoot, NavDestination.OBD_DIAGNOSTICS),
                    Triple("TYRE PRESSURE (TPMS)", Icons.Default.TireRepair, NavDestination.TPMS_DASHBOARD),
                    Triple("THEME CUSTOMIZATION", Icons.Default.Palette, NavDestination.THEME_CUSTOMIZATION)
                ).forEach { (label, icon, dest) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                            .clickable { onNavigateDestination(dest) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
                            Text(label, color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
