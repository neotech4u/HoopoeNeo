package com.hoopoe.launcher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.data.AppSettings
import com.hoopoe.launcher.data.ClockStyle
import com.hoopoe.launcher.data.DashboardLayoutPreset
import com.hoopoe.launcher.data.ThemePreset

private data class ThemeCardItem(
    val preset: ThemePreset,
    val name: String,
    val subtitle: String,
    val primaryBg: Color,
    val surfaceColor: Color,
    val accentColor: Color,
    val fontColor: Color
)

@Composable
fun ThemeCustomizationScreen(
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

    val themes = listOf(
        ThemeCardItem(
            ThemePreset.DARK_AUTOMOTIVE,
            "DARK AUTOMOTIVE",
            "Obsidian Black & Electric Cyan Accent",
            Color(0xFF07090E),
            Color(0xFF0F141F),
            Color(0xFF00E5FF),
            Color(0xFFF8FAFC)
        ),
        ThemeCardItem(
            ThemePreset.BLACK_CHAMPAGNE_GOLD,
            "BLACK & CHAMPAGNE GOLD",
            "Piano Black & Luxury Bronze Accent",
            Color(0xFF050505),
            Color(0xFF141416),
            Color(0xFFD4AF37),
            Color(0xFFF5F5F0)
        ),
        ThemeCardItem(
            ThemePreset.GRAPHITE_TECH,
            "GRAPHITE TECH",
            "Anthracite Carbon & Ice Blue Accent",
            Color(0xFF121417),
            Color(0xFF1E2229),
            Color(0xFF64B5F6),
            Color(0xFFFFFFFF)
        ),
        ThemeCardItem(
            ThemePreset.MINIMAL_DASHBOARD,
            "MINIMAL DASHBOARD",
            "Zero-Glare Black & Warm Amber Accent",
            Color(0xFF000000),
            Color(0xFF0C0C0D),
            Color(0xFFFF9100),
            Color(0xFFCBD5E1)
        )
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
            Text("THEME & DASHBOARD CUSTOMIZATION", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: Dashboard Layout Presets
            Text("1. DASHBOARD LAYOUT PRESETS", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple(DashboardLayoutPreset.THREE_ZONE_HUD, "3-ZONE CYBER HUD", "Speedometer + Map + Media/Fuel Hub"),
                    Triple(DashboardLayoutPreset.MINIMAL_DASHBOARD, "MINIMAL DASHBOARD", "Ultra-clean big speed + Clock + Prayer"),
                    Triple(DashboardLayoutPreset.COCKPIT_QUAD, "COCKPIT QUAD GRID", "4 Balanced Instrument Widgets")
                ).forEach { (layout, title, desc) ->
                    val isSelected = settings.dashboardLayout == layout
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accent.copy(alpha = 0.12f) else cardBg)
                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) accent else cardBorder, RoundedCornerShape(12.dp))
                            .clickable { onUpdateSettings { copy(dashboardLayout = layout) } }
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, color = if (isSelected) accent else contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(desc, color = contentColor.copy(alpha = 0.5f), fontSize = 9.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Section 2: Master Themes
            Text("2. AUTOMOTIVE COLOR THEMES", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(themes) { item ->
                    val isSelected = settings.activeTheme == item.preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(item.surfaceColor)
                            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) item.accentColor else cardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                onUpdateSettings {
                                    copy(
                                        activeTheme = item.preset,
                                        accentColor = item.accentColor.toArgb(),
                                        backgroundColor = item.primaryBg.toArgb(),
                                        fontColor = item.fontColor.toArgb()
                                    )
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, color = item.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(item.subtitle, color = item.fontColor.copy(alpha = 0.6f), fontSize = 9.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(item.accentColor)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Section 3: Cockpit Instrument Styles
            Text("3. INSTRUMENT GAUGE STYLES", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Clock style
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            val next = if (settings.clockStyle == ClockStyle.DIGITAL) ClockStyle.ANALOG else ClockStyle.DIGITAL
                            onUpdateSettings { copy(clockStyle = next) }
                        }
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("CLOCK STYLE", color = contentColor.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(settings.clockStyle.name, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Speedometer digital only toggle
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            onUpdateSettings { copy(speedometerDigitalOnly = !speedometerDigitalOnly) }
                        }
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("SPEEDOMETER GAUGE", color = contentColor.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(if (settings.speedometerDigitalOnly) "DIGITAL ONLY" else "ANALOG RING + NEON WAVE", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
