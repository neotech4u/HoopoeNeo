package com.hoopoe.launcher.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.data.AppSettings

@Composable
fun AutomationScreen(
    settings: AppSettings,
    accent: Color,
    isDayMode: Boolean = false,
    onUpdateSettings: (AppSettings.() -> AppSettings) -> Unit,
    onBackToCockpit: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Text("SMART AUTOMATION ENGINE", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Text(
            text = "Configure event-driven automations for boot startup, Bluetooth triggers, overspeed warnings, and prayer reminders.",
            color = contentColor.copy(alpha = 0.5f),
            fontSize = 9.5.sp
        )

        // Settings Scrollable List
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Automation 1: Auto launch on boot
            AutomationToggleRow(
                title = "Auto-Launch on Device Startup",
                subtitle = "Automatically opens HOOPOE when the head unit boots up (where supported by firmware).",
                checked = settings.autoLaunchOnBoot,
                onCheckedChange = { onUpdateSettings { copy(autoLaunchOnBoot = it) } },
                accent = accent,
                cardBg = cardBg,
                cardBorder = cardBorder,
                contentColor = contentColor
            )

            // Automation 2: Cold boot logon video
            AutomationToggleRow(
                title = "Play Logon Boot Animation",
                subtitle = "Plays the custom high-tech Hoopoe boot video on cold startup with tap-to-skip.",
                checked = settings.playLogonAnimation,
                onCheckedChange = { onUpdateSettings { copy(playLogonAnimation = it) } },
                accent = accent,
                cardBg = cardBg,
                cardBorder = cardBorder,
                contentColor = contentColor
            )

            // Automation 3: Travel Dua greeting
            AutomationToggleRow(
                title = "Display Travel Dua on Startup",
                subtitle = "Prompts driver to read the travel prayer before commencing journey with 'Fi Aman Allah' blessing.",
                checked = settings.showTravelDua,
                onCheckedChange = { onUpdateSettings { copy(showTravelDua = it) } },
                accent = accent,
                cardBg = cardBg,
                cardBorder = cardBorder,
                contentColor = contentColor
            )

            // Automation 4: Overspeed detection
            AutomationToggleRow(
                title = "Overspeed Warning Alert",
                subtitle = "Assistant Ring pulses urgent red and chimes when vehicle GPS speed exceeds threshold (${settings.overspeedThresholdKmh.toInt()} KM/H).",
                checked = settings.overspeedWarningEnabled,
                onCheckedChange = { onUpdateSettings { copy(overspeedWarningEnabled = it) } },
                accent = accent,
                cardBg = cardBg,
                cardBorder = cardBorder,
                contentColor = contentColor
            )

            // Automation 5: Bluetooth auto resume
            AutomationToggleRow(
                title = "Auto-Resume Bluetooth Media",
                subtitle = "Automatically sends playback play command when head unit connects to smartphone Bluetooth.",
                checked = settings.autoResumeBluetoothMedia,
                onCheckedChange = { onUpdateSettings { copy(autoResumeBluetoothMedia = it) } },
                accent = accent,
                cardBg = cardBg,
                cardBorder = cardBorder,
                contentColor = contentColor
            )

            // Automation 6: Prayer time reminder
            AutomationToggleRow(
                title = "Prayer Adhan Notification",
                subtitle = "Assistant Ring alerts driver within 15 minutes of upcoming prayer time.",
                checked = settings.prayerAlertsEnabled,
                onCheckedChange = { onUpdateSettings { copy(prayerAlertsEnabled = it) } },
                accent = accent,
                cardBg = cardBg,
                cardBorder = cardBorder,
                contentColor = contentColor
            )
        }
    }
}

@Composable
private fun AutomationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF07090E),
                    checkedTrackColor = accent
                )
            )
        }
    }
}
