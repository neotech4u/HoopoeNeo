package com.hoopoe.launcher.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class ClockStyle { DIGITAL, ANALOG }
enum class UnitSystem { METRIC, IMPERIAL }
enum class AppFont { SYSTEM, JETBRAINS_MONO, SOURCE_CODE_PRO }
enum class DayNightMode { DARK, LIGHT, AUTO, SYSTEM }
enum class SidebarPosition { LEFT, RIGHT, BOTTOM }
enum class GradientDirection { TOP_TO_BOTTOM, LEFT_TO_RIGHT, DIAGONAL, RADIAL }

/** 3 Distinct Automotive Dashboard Layouts */
enum class DashboardLayoutPreset {
    THREE_ZONE_HUD,    // Zone 1: Speedometer, Zone 2: Navigation Map, Zone 3: Media & Fuel Hub
    MINIMAL_DASHBOARD, // High-readability large speed, minimal clock, weather, next prayer, media
    COCKPIT_QUAD       // 4 balanced instruments: Speedometer, Osmdroid Map, Media Player, Diagnostics
}

/** 4 Master Automotive Color & Design Themes */
enum class ThemePreset {
    DARK_AUTOMOTIVE,      // Deep Obsidian #07090E, Neon Cyan #00E5FF
    BLACK_CHAMPAGNE_GOLD, // Piano Black #050505, Champagne Gold #D4AF37
    GRAPHITE_TECH,        // Dark Anthracite #121417, Titanium Silver #E0E6ED & Ice Blue #64B5F6
    MINIMAL_DASHBOARD     // Zero-Light Black #000000, Warm White / Muted Amber #FF9100
}

/** Recognized Islamic Prayer Calculation Conventions */
enum class PrayerCalcMethod(val displayName: String) {
    MUSLIM_WORLD_LEAGUE("Muslim World League (MWL)"),
    ISNA("Islamic Society of North America (ISNA)"),
    EGYPT("Egyptian General Authority"),
    MAKKAH("Umm al-Qura University, Makkah"),
    KARACHI("University of Islamic Sciences, Karachi")
}

/** Juristic Asr Calculation Method */
enum class AsrCalcMethod(val displayName: String) {
    SHAFI("Shafi'i, Maliki, Hanbali (Standard)"),
    HANAFI("Hanafi (Later Asr)")
}

enum class DefaultShortcutIcon {
    NONE,
    // Navigation & vehicle
    RADIO, CAMERA, PHONE, MAP, NAVIGATION, CAR, GAS_STATION, DASHBOARD,
    // Audio & media
    MUSIC, SPEAKER, HEADSET, EQUALIZER, VOLUME_UP,
    // Connectivity
    BLUETOOTH, WIFI,
    // Lighting & climate
    LIGHTBULB, BRIGHTNESS, AC, THERMOSTAT,
    // General utility
    TV, VIDEOCAM, STAR, MESSAGE, TIMER, LOCK, SETTINGS, FAVORITE,
    // Web / location
    GLOBE,
    // Islamic & Diagnostics
    MOSQUE, DIAGNOSTICS, TPMS
}

data class SoundPadConfig(
    val label: String,
    val audioUri: String = "",
    val synthType: String = "BEEP"
)

fun defaultSoundboardPads() = listOf(
    SoundPadConfig("mario_jump",   synthType = "mario_jump"),
    SoundPadConfig("mario_coin",   synthType = "mario_coin"),
    SoundPadConfig("boom",         synthType = "boom"),
    SoundPadConfig("loud_fart",    synthType = "loud_fart"),
    SoundPadConfig("+",            synthType = ""),
    SoundPadConfig("+",            synthType = "")
)

data class ShortcutConfig(
    val packageName: String = "",
    val label: String = "",
    val isDefault: Boolean = false,
    val defaultIcon: DefaultShortcutIcon = DefaultShortcutIcon.NONE,
    // null = native app icon; non-null = override with this vector icon
    val customIconOverride: DefaultShortcutIcon? = null
)

const val GRID_COLS = 3
const val GRID_ROWS = 2

data class WidgetConfig(
    val id: String,          // "CLOCK" | "WEATHER" | "TELEMETRY" | "NOW_PLAYING" | "SPEEDOMETER" | "PRAYER"
    val gridX: Int,          // column 0..(GRID_COLS-1)
    val gridY: Int,          // row    0..(GRID_ROWS-1)
    val spanX: Int = 1,
    val spanY: Int = 1,
    val enabled: Boolean = true
)

data class AppSettings(
    val vehicleName: String = "DZIRE",
    val accentColor: Int = Color(0xFF00E5FF).toArgb(), // Cyber Electric Cyan
    val backgroundColor: Int = Color(0xFF07090E).toArgb(), // Deep Obsidian OLED Dark
    val fontColor: Int = Color(0xFFF8FAFC).toArgb(),
    val wallpaperUri: String = "",
    val fontBold: Boolean = false,
    val textScale: Float = 1.2f,
    val uiScale: Float = 1.0f,
    val clockStyle: ClockStyle = ClockStyle.DIGITAL,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val appFont: AppFont = AppFont.JETBRAINS_MONO,
    val showWeather: Boolean = true,
    val showClock: Boolean = true,
    val showTelemetry: Boolean = true,
    val showNowPlaying: Boolean = true,
    val shortcuts: List<ShortcutConfig> = defaultShortcuts(),
    val widgetLayout: List<WidgetConfig> = defaultWidgetLayout(),
    val carPlayPackage: String = "",
    val androidAutoPackage: String = "",
    val useGradient: Boolean = false,
    val gradientEndColor: Int = Color(0xFF07090E).toArgb(),
    val wallpaperDim: Float = 0.55f,
    val sidebarPosition: SidebarPosition = SidebarPosition.RIGHT,
    val bottomBarShortcutsRight: Boolean = false,
    val showAltimeter: Boolean = false,
    val showSpeedometer: Boolean = true,
    val showNavMap: Boolean = true,
    val showFuelHub: Boolean = true,
    val showRadio: Boolean = false,
    val dayNightMode: DayNightMode = DayNightMode.DARK,
    val showPip: Boolean = false,
    val pipAppPackage: String = "",
    // Head unit's radio app — mirrored & controlled via its MediaSession
    val radioPackage: String = "",
    val onboardingCompleted: Boolean = false,
    val showVitals: Boolean = false,
    val showTripTracker: Boolean = false,
    val compassOffset: Float = 0f,
    val showSoundboard: Boolean = false,
    val soundboardPads: List<SoundPadConfig> = defaultSoundboardPads(),
    val vitalsAsBars: Boolean = false,
    val speedometerDigitalOnly: Boolean = false,
    val gradientDirection: GradientDirection = GradientDirection.DIAGONAL,
    val useCustomBackgroundColor: Boolean = false,
    val playLogonAnimation: Boolean = true,
    val showTravelDua: Boolean = true,

    // Standalone & Automotive Companion Extensions
    val autoLaunchOnBoot: Boolean = false,
    val dashboardLayout: DashboardLayoutPreset = DashboardLayoutPreset.THREE_ZONE_HUD,
    val activeTheme: ThemePreset = ThemePreset.DARK_AUTOMOTIVE,
    val prayerCalculationMethod: PrayerCalcMethod = PrayerCalcMethod.MUSLIM_WORLD_LEAGUE,
    val asrMethod: AsrCalcMethod = AsrCalcMethod.SHAFI,
    val prayerAlertsEnabled: Boolean = true,
    val showPrayerWidget: Boolean = true,
    val overspeedWarningEnabled: Boolean = true,
    val overspeedThresholdKmh: Float = 80f,
    val autoResumeBluetoothMedia: Boolean = true,
    val homeAddress: String = "",
    val workAddress: String = ""
)

fun defaultShortcuts() = listOf(
    ShortcutConfig(label = "Radio", isDefault = true, defaultIcon = DefaultShortcutIcon.RADIO),
    ShortcutConfig(label = "Navi", isDefault = true, defaultIcon = DefaultShortcutIcon.NAVIGATION),
    ShortcutConfig(label = "Music", isDefault = true, defaultIcon = DefaultShortcutIcon.MUSIC),
    ShortcutConfig(label = "Phone", isDefault = true, defaultIcon = DefaultShortcutIcon.PHONE),
    ShortcutConfig(label = "Camera", isDefault = true, defaultIcon = DefaultShortcutIcon.CAMERA)
)

fun defaultWidgetLayout() = listOf(
    // Zone 1 (Left): Driver Telemetry Speedometer & Gauge Cluster (1x2)
    WidgetConfig("SPEEDOMETER", gridX = 0, gridY = 0, spanX = 1, spanY = 2),
    // Zone 2 (Center): Dynamic Visualizer & Neon Vector Navigation Map (1x2)
    WidgetConfig("NAV_MAP",     gridX = 1, gridY = 0, spanX = 1, spanY = 2),
    // Zone 3 (Right - Top): Modular Glanceable Media Player & FM Radio Card (1x1)
    WidgetConfig("NOW_PLAYING", gridX = 2, gridY = 0, spanX = 1, spanY = 1),
    // Zone 3 (Right - Bottom): Smart Fuel & Live Trip Telemetry Hub (1x1)
    WidgetConfig("FUEL_HUB",    gridX = 2, gridY = 1, spanX = 1, spanY = 1)
)

fun AppSettings.activeWidgetIds(): Set<String> = buildSet {
    if (showSpeedometer) add("SPEEDOMETER")
    if (showNavMap) add("NAV_MAP")
    if (showNowPlaying) add("NOW_PLAYING")
    if (showRadio) add("RADIO")
    if (showFuelHub) add("FUEL_HUB")
    if (showPrayerWidget) add("PRAYER")
    if (showClock) add("CLOCK")
    if (showWeather) add("WEATHER")
    if (showTelemetry) add("TELEMETRY")
    if (showAltimeter) add("ALTIMETER")
    if (showVitals) add("VITALS")
    if (showTripTracker) add("TRIP_TRACKER")
    if (showSoundboard) add("SOUNDBOARD")
}

/**
 * Moves [movingId] to ([targetX], [targetY]) and pushes any displaced widgets to the
 * first available free cell, cascading until all conflicts are resolved.
 */
fun computeWidgetMove(
    layout: List<WidgetConfig>,
    movingId: String,
    targetX: Int,
    targetY: Int
): List<WidgetConfig> {
    val moving = layout.find { it.id == movingId } ?: return layout
    val placed = moving.copy(
        gridX = targetX.coerceIn(0, GRID_COLS - moving.spanX),
        gridY = targetY.coerceIn(0, GRID_ROWS - moving.spanY)
    )

    val others  = layout.filter { it.id != movingId }
    val result  = mutableListOf(placed)
    val occupied = buildOccupied(result).toMutableSet()

    val (stable, displaced) = others.partition { w -> result.none { widgetsOverlap(it, w) } }

    for (w in stable) {
        result.add(w)
        for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) occupied.add(w.gridX + dx to w.gridY + dy)
    }

    for (w in displaced) {
        val pos = firstFreeGridPos(w.spanX, w.spanY, occupied)
        val resolved = if (pos != null) w.copy(gridX = pos.first, gridY = pos.second) else w
        result.add(resolved)
        for (dx in 0 until resolved.spanX) for (dy in 0 until resolved.spanY) occupied.add(resolved.gridX + dx to resolved.gridY + dy)
    }

    return result
}

private fun buildOccupied(widgets: List<WidgetConfig>) = buildSet<Pair<Int, Int>> {
    widgets.forEach { w -> for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) add(w.gridX + dx to w.gridY + dy) }
}

private fun widgetsOverlap(a: WidgetConfig, b: WidgetConfig): Boolean =
    a.gridX < b.gridX + b.spanX && a.gridX + a.spanX > b.gridX &&
    a.gridY < b.gridY + b.spanY && a.gridY + a.spanY > b.gridY

private fun firstFreeGridPos(spanX: Int, spanY: Int, occupied: Set<Pair<Int, Int>>): Pair<Int, Int>? {
    for (row in 0 until GRID_ROWS) for (col in 0 until GRID_COLS) {
        if (col + spanX > GRID_COLS || row + spanY > GRID_ROWS) continue
        if ((0 until spanX).all { dx -> (0 until spanY).all { dy -> (col + dx to row + dy) !in occupied } })
            return col to row
    }
    return null
}
