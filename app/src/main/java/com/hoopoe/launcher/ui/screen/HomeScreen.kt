package com.hoopoe.launcher.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.hoopoe.launcher.data.AppSettings
import com.hoopoe.launcher.data.ClockStyle
import com.hoopoe.launcher.data.computeWidgetMove
import com.hoopoe.launcher.data.DashboardLayoutPreset
import com.hoopoe.launcher.data.GRID_COLS
import com.hoopoe.launcher.data.GRID_ROWS
import com.hoopoe.launcher.data.WidgetConfig
import com.hoopoe.launcher.model.NowPlayingState
import com.hoopoe.launcher.model.WeatherState
import kotlinx.coroutines.flow.StateFlow
import com.hoopoe.launcher.ui.theme.LocalDayMode
import com.hoopoe.launcher.ui.widget.*
import com.hoopoe.launcher.ui.components.MdusOsDock
import com.hoopoe.launcher.util.PrayerTimeCalculator
import java.util.Calendar
import com.hoopoe.launcher.util.LocationData

private val WIDGET_RADIUS = RoundedCornerShape(16.dp)

private data class WidgetTypeInfo(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val description: String
)

private val ALL_WIDGET_TYPES = listOf(
    WidgetTypeInfo("SPEEDOMETER",  "SPEEDOMETER",       Icons.Default.Speed,            "Standalone GPS speed readout"),
    WidgetTypeInfo("NAV_MAP",      "MAP (AUTO)",        Icons.Default.Navigation,       "Live embedded OpenStreetMap & GPS tracker"),
    WidgetTypeInfo("NOW_PLAYING",  "SMART MUSIC",       Icons.Default.MusicNote,        "Track metadata, art & playback controls"),
    WidgetTypeInfo("RADIO",        "AM/FM RADIO",       Icons.Default.Radio,            "Digital tuner display & live seek"),
    WidgetTypeInfo("FUEL_HUB",     "FUEL & TRIP",       Icons.Default.LocalGasStation,  "Trip & Fuel Telemetry Hub"),
    WidgetTypeInfo("PRAYER",       "PRAYER COMPANION",  Icons.Default.Mosque,           "Upcoming prayer countdown & schedule"),
    WidgetTypeInfo("TRIP_TRACKER", "TRIP METER",        Icons.Default.Route,            "Rolling odometer & 0-100 timer"),
    WidgetTypeInfo("ALTIMETER",    "ALTIMETER",         Icons.Default.FlightTakeoff,    "Elevation & GPS pitch/roll"),
    WidgetTypeInfo("VITALS",       "UNIT VITALS",       Icons.Default.Dns,              "Real-time CPU, RAM & temperature"),
    WidgetTypeInfo("TELEMETRY",    "GPS COMPASS",       Icons.Default.Explore,          "Heading & compass bearing"),
    WidgetTypeInfo("SOUNDBOARD",   "SOUNDBOARD",        Icons.Default.Piano,            "6 assignable sound & synth pads"),
    WidgetTypeInfo("WEATHER",      "WEATHER",           Icons.Default.Cloud,            "Dynamic connectivity weather"),
    WidgetTypeInfo("CLOCK",        "CLOCK",             Icons.Default.AccessTime,       "Large digital/analog clock")
)

private fun canAddWidget(settings: com.hoopoe.launcher.data.AppSettings): Boolean {
    val visibleIds = buildSet {
        if (settings.showClock) add("CLOCK")
        if (settings.showWeather) add("WEATHER")
        if (settings.showNowPlaying) add("NOW_PLAYING")
        if (settings.showRadio) add("RADIO")
        if (settings.showTelemetry) add("TELEMETRY")
        if (settings.showAltimeter) add("ALTIMETER")
        if (settings.showSpeedometer) add("SPEEDOMETER")
        if (settings.showNavMap) add("NAV_MAP")
        if (settings.showVitals) add("VITALS")
        if (settings.showTripTracker) add("TRIP_TRACKER")
        if (settings.showFuelHub) add("FUEL_HUB")
        if (settings.showPrayerWidget) add("PRAYER")
        if (settings.showSoundboard) add("SOUNDBOARD")
    }
    val activeWidgets = settings.widgetLayout.filter { it.enabled && it.id in visibleIds }
    val occupied = buildSet<Pair<Int, Int>> {
        activeWidgets.forEach { w ->
            for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) add(w.gridX + dx to w.gridY + dy)
        }
    }
    val hasFreeCell = (0 until com.hoopoe.launcher.data.GRID_ROWS).any { r ->
        (0 until com.hoopoe.launcher.data.GRID_COLS).any { c -> (c to r) !in occupied }
    }
    // Also true if any active widget spans >1 cell and can be shrunk to make room
    val hasShrinkable = activeWidgets.any { it.spanX * it.spanY > 1 }
    return hasFreeCell || hasShrinkable
}

@Composable
fun HomeScreen(
    settings: AppSettings,
    weather: WeatherState?,
    nowPlaying: NowPlayingState?,
    locationFlow: StateFlow<LocationData?>,
    bearingFlow: StateFlow<Float>,
    isWifi: Boolean,
    isData: Boolean,
    isDayMode: Boolean = false,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onAssignCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onClearCarPlay: () -> Unit,
    onClearAndroidAuto: () -> Unit,
    onAssignPip: () -> Unit,
    onClearPip: () -> Unit,
    onLaunchPip: () -> Unit,
    onTapNowPlaying: () -> Unit,
    onUpdateWidget: (id: String, spanX: Int, spanY: Int) -> Unit,
    onMoveWidget: (id: String, gridX: Int, gridY: Int) -> Unit,
    onAddWidget: (id: String) -> Unit,
    onRemoveWidget: (id: String) -> Unit,
    onSetClockStyle: (ClockStyle) -> Unit,
    onSetVitalsAsBars: (Boolean) -> Unit = {},
    onSetSpeedometerDigitalOnly: (Boolean) -> Unit = {},
    onUpdateSoundPad: (index: Int, pad: com.hoopoe.launcher.data.SoundPadConfig) -> Unit = { _, _ -> },
    hardwareRadio: com.hoopoe.launcher.viewmodel.LauncherViewModel.HardwareRadioState? = null,
    onLaunchHardwareRadio: () -> Unit = {},
    onStopHardwareRadio: () -> Unit = {},
    onRadioSeekUp: () -> Unit = {},
    onRadioSeekDown: () -> Unit = {},
    onRadioCycleFm: () -> Unit = {},
    onRadioSwitchAm: () -> Unit = {},
    onRadioTune: (band: String, freq: Float) -> Unit = { _, _ -> },
    onAssignRadio: () -> Unit = {},
    onOpenIslamic: () -> Unit = {},
    onOpenVoiceAssistant: () -> Unit = {},
    onOpenQuickControls: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    onOpenNavigationCentre: () -> Unit = {},
    onOpenMediaCentre: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accent       = Color(settings.accentColor)
    val gap          = 10.dp
    val hasWallpaper = settings.wallpaperUri.isNotEmpty()
    val widgetBg     = when {
        isDayMode    -> Color(0xFFFFFFFF)
        hasWallpaper -> Color(0xCC0F141F)
        else         -> Color(0xFF0F141F).copy(alpha = 0.82f)
    }
    val widgetBorder = when {
        isDayMode    -> Color(0xFFCBD5E1)
        hasWallpaper -> Color(0xFF243048).copy(alpha = 0.6f)
        else         -> Color(0xFF243048)
    }
    val headerTextColor   = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val statusIconColor   = if (isDayMode) Color(0xFF475569) else Color(0xFF64748B)
    val controlIconColor  = if (isDayMode) Color(0xFF475569) else Color(0xFF94A3B8)

    val currentLoc by locationFlow.collectAsState()
    val rawSpeedVal = (currentLoc?.speedMps ?: 0f) * (if (settings.unitSystem.name == "METRIC") 3.6f else 2.237f)
    val speedKmh = rawSpeedVal.toInt()
    val isOverspeed = settings.overspeedWarningEnabled && speedKmh > settings.overspeedThresholdKmh

    val homeContext = androidx.compose.ui.platform.LocalContext.current
    val tripLogManager = remember { com.hoopoe.launcher.data.TripLogManager.getInstance(homeContext) }
    LaunchedEffect(speedKmh) {
        tripLogManager.onSpeedUpdate(speedKmh.toDouble(), 1.0)
    }
    DisposableEffect(Unit) {
        onDispose {
            tripLogManager.finishAndLogCurrentRide()
        }
    }

    val prayerTimes = remember(currentLoc, settings.prayerCalculationMethod, settings.asrMethod) {
        val lat = currentLoc?.latitude ?: 28.6139
        val lon = currentLoc?.longitude ?: 77.2090
        PrayerTimeCalculator.calculateTimes(lat, lon, method = settings.prayerCalculationMethod, asrMethod = settings.asrMethod)
    }

    val isPrayerAlert = settings.prayerAlertsEnabled && prayerTimes.minutesToNextPrayer in 0..15
    val isSevereWeather = weather != null && (weather.weatherCode in listOf(95, 96, 99, 65, 75))

    val alertText = when {
        isOverspeed -> "OVERSPEED: $speedKmh KM/H"
        isPrayerAlert -> "${prayerTimes.nextPrayerName.uppercase()} IN ${prayerTimes.minutesToNextPrayer}M"
        isSevereWeather -> "WEATHER ALERT"
        else -> null
    }

    var resizingId    by remember { mutableStateOf<String?>(null) }
    var contextMenuId by remember { mutableStateOf<String?>(null) }

    val configuration    = LocalConfiguration.current
    val isLandscape      = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var editMode         by remember { mutableStateOf(false) }
    var widgetLibraryOpen by remember { mutableStateOf(false) }

    var activeDriveMode by remember { mutableStateOf("SPORT") }
    var isLeftBlinker by remember { mutableStateOf(false) }
    var isRightBlinker by remember { mutableStateOf(false) }
    var showRefuelDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

        // ── Futuristic Top Status Rail ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hoopoe Logo Emblem + Bottom MDUS OS Tech Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF243048), RoundedCornerShape(12.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
                    .clickable { onOpenQuickControls() }
            ) {
                Image(
                    painter = painterResource(com.hoopoe.launcher.R.drawable.hoopoe_logo),
                    contentDescription = "Hoopoe Logo",
                    modifier = Modifier.size(24.dp)
                )
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "HOOPOE",
                        color = headerTextColor,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 1.sp,
                        lineHeight = 12.sp
                    )
                    Text(
                        text = "MDUS OS",
                        color = accent,
                        fontSize = 8.5.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        lineHeight = 9.sp
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            val driveMetrics = remember(speedKmh) {
                com.hoopoe.launcher.util.DriveEfficiencyCalculator.calculate(speedKmh.toFloat())
            }

            // Dynamic Drive Profile & Real-Time CO2 Emissions Badge
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF243048), RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("ECO", "PILOT", "SPORT").forEach { modeName ->
                    val isModeActive = driveMetrics.mode.label == modeName
                    val badgeColor = when (modeName) {
                        "ECO" -> Color(0xFF00E676)
                        "PILOT" -> Color(0xFF00E5FF)
                        else -> Color(0xFFFF1744)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isModeActive) badgeColor else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = modeName,
                            color = if (isModeActive) Color(0xFF07090E) else statusIconColor,
                            fontSize = 9.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Dynamic CO2 Emission Readout
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(0.8.dp, Color(driveMetrics.mode.badgeColorHex).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = driveMetrics.co2Display,
                        color = Color(driveMetrics.mode.badgeColorHex),
                        fontSize = 9.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // OBD-II / CANbus Status Badge
            val context = androidx.compose.ui.platform.LocalContext.current
            val fuelRepo = remember { com.hoopoe.launcher.data.TripFuelRepository.getInstance(context) }
            val isObdOnline by fuelRepo.isObdOnline.collectAsState()

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isObdOnline) accent.copy(alpha = 0.15f) else Color(0xFFFF9100).copy(alpha = 0.15f))
                    .border(1.dp, if (isObdOnline) accent.copy(alpha = 0.5f) else Color(0xFFFF9100).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable { onOpenDiagnostics() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isObdOnline) accent else Color(0xFFFF9100))
                    )
                    Text(
                        text = if (isObdOnline) "ONLINE (OBD2/CAN)" else "OFFLINE / STANDALONE",
                        color = if (isObdOnline) accent else Color(0xFFFF9100),
                        fontSize = 9.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Low-Resource Holographic Assistant Ring with Notification Alerts
            com.hoopoe.launcher.ui.components.JarvisOrb(
                accent = accent,
                orbSize = 34.dp,
                isOverspeed = isOverspeed,
                isPrayerAlert = isPrayerAlert,
                isWeatherAlert = isSevereWeather,
                alertText = alertText,
                onClick = onOpenVoiceAssistant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.weight(1f))

            // Turn Signal Blinkers
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isLeftBlinker) Color(0xFF00E676).copy(alpha = 0.2f) else Color.Transparent)
                        .clickable {
                            isLeftBlinker = !isLeftBlinker
                            if (isLeftBlinker) isRightBlinker = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Left Blinker",
                        tint = if (isLeftBlinker) Color(0xFF00E676) else statusIconColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isRightBlinker) Color(0xFF00E676).copy(alpha = 0.2f) else Color.Transparent)
                        .clickable {
                            isRightBlinker = !isRightBlinker
                            if (isRightBlinker) isLeftBlinker = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Right Blinker",
                        tint = if (isRightBlinker) Color(0xFF00E676) else statusIconColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Exterior Weather Temp if available
            if (weather != null) {
                Text(
                    text = weather.temperatureDisplay(settings.unitSystem.name == "METRIC"),
                    color = Color(0xFFFF9100),
                    fontSize = 11.5.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
            }

            // Connectivity
            AnimatedVisibility(visible = isWifi, enter = fadeIn(), exit = fadeOut()) {
                Icon(Icons.Default.Wifi, "WiFi", tint = accent, modifier = Modifier.size(15.dp))
            }
            if (isWifi) Spacer(Modifier.width(6.dp))
            AnimatedVisibility(visible = isData, enter = fadeIn(), exit = fadeOut()) {
                Icon(Icons.Default.SignalCellularAlt, "5G Data", tint = accent, modifier = Modifier.size(15.dp))
            }

            if (isLandscape) {
                Spacer(Modifier.width(8.dp))
                if (editMode) {
                    IconButton(
                        onClick  = { widgetLibraryOpen = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Dashboard,
                            contentDescription = "Widget library",
                            tint               = controlIconColor,
                            modifier           = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                }
                IconButton(
                    onClick  = { editMode = !editMode },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Edit,
                        contentDescription = "Edit widgets",
                        tint               = if (editMode) accent else controlIconColor,
                        modifier           = Modifier.size(15.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF243048))

        // ── Widget Grid ─────────────────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(gap)
        ) {
            val cellW = (maxWidth  - gap * (GRID_COLS - 1)) / GRID_COLS
            val cellH = (maxHeight - gap * (GRID_ROWS - 1)) / GRID_ROWS
            val density = LocalDensity.current
            val cellStepXPx = with(density) { (cellW + gap).toPx() }
            val cellStepYPx = with(density) { (cellH + gap).toPx() }

            // WEATHER stays in the set even with no data: the commit path
            // (LauncherViewModel.moveWidgetConfig) computes against settings flags
            // only, so dropping it here would make the drop ghost and the committed
            // layout disagree. With no data the cell renders fully transparent.
            val visibleIds = buildSet {
                if (settings.showClock) add("CLOCK")
                if (settings.showWeather) add("WEATHER")
                if (settings.showNowPlaying) add("NOW_PLAYING")
                if (settings.showRadio) add("RADIO")
                if (settings.showTelemetry) add("TELEMETRY")
                if (settings.showAltimeter) add("ALTIMETER")
                if (settings.showSpeedometer) add("SPEEDOMETER")
                if (settings.showNavMap) add("NAV_MAP")
                if (settings.showFuelHub) add("FUEL_HUB")
                if (settings.showPrayerWidget) add("PRAYER")
                if (settings.showVitals) add("VITALS")
                if (settings.showTripTracker) add("TRIP_TRACKER")
                if (settings.showSoundboard) add("SOUNDBOARD")
            }

            // Keep only visible widgets exactly as configured in settings, allowing explicit resizing to dictate layout
            val visible = settings.widgetLayout.filter { it.enabled && it.id in visibleIds }
            val rendered = visible

            // ── Drag state ───────────────────────────────────────────────────
            var draggingId   by remember { mutableStateOf<String?>(null) }
            var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }

            // Compute snap target for the widget being dragged (uses original spanX)
            val draggingOriginal = if (draggingId != null) visible.find { it.id == draggingId } else null
            val targetGridX = draggingOriginal?.let {
                (it.gridX + (dragOffsetPx.x / cellStepXPx).roundToInt()).coerceIn(0, GRID_COLS - it.spanX)
            }
            val targetGridY = draggingOriginal?.let {
                (it.gridY + (dragOffsetPx.y / cellStepYPx).roundToInt()).coerceIn(0, GRID_ROWS - it.spanY)
            }

            // Compute proposed layout (push preview) while dragging
            val proposedLayout = if (draggingOriginal != null && targetGridX != null && targetGridY != null)
                computeWidgetMove(visible, draggingOriginal.id, targetGridX, targetGridY)
            else null

            // Drop ghost — rendered before widgets so it appears beneath them
            if (draggingOriginal != null && targetGridX != null && targetGridY != null) {
                val gX = (cellW + gap) * targetGridX
                val gY = (cellH + gap) * targetGridY
                val gW = cellW * draggingOriginal.spanX + gap * (draggingOriginal.spanX - 1)
                val gH = cellH * draggingOriginal.spanY + gap * (draggingOriginal.spanY - 1)
                Box(
                    modifier = Modifier
                        .absoluteOffset(x = gX, y = gY)
                        .size(gW, gH)
                        .background(accent.copy(alpha = 0.08f))
                        .border(1.dp, accent.copy(alpha = 0.5f), WIDGET_RADIUS)
                )
            }

            // Displacement ghosts — show where pushed widgets will land
            if (proposedLayout != null && draggingOriginal != null) {
                proposedLayout
                    .filter { it.id != draggingOriginal.id }
                    .forEach { proposed ->
                        val original = visible.find { it.id == proposed.id } ?: return@forEach
                        if (proposed.gridX != original.gridX || proposed.gridY != original.gridY) {
                            val dX = (cellW + gap) * proposed.gridX
                            val dY = (cellH + gap) * proposed.gridY
                            val dW = cellW * proposed.spanX + gap * (proposed.spanX - 1)
                            val dH = cellH * proposed.spanY + gap * (proposed.spanY - 1)
                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = dX, y = dY)
                                    .size(dW, dH)
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), WIDGET_RADIUS)
                            )
                        }
                    }
            }

            rendered.forEach { w ->
                val xOff   = (cellW + gap) * w.gridX
                val yOff   = (cellH + gap) * w.gridY
                val width  = cellW * w.spanX + gap * (w.spanX - 1)
                val height = cellH * w.spanY + gap * (w.spanY - 1)

                val label = when (w.id) {
                    "CLOCK"       -> clockTimeLabel(Calendar.getInstance())
                    "WEATHER"     -> "WEATHER"
                    "NOW_PLAYING" -> "NOW PLAYING"
                    "TELEMETRY"   -> "COMPASS"
                    "ALTIMETER"   -> "ALTIMETER"
                    "SPEEDOMETER" -> "SPEED"
                    "TRIP_TRACKER" -> "TRIP"
                    "FUEL_HUB"    -> "FUEL HUB"
                    "PRAYER"      -> "PRAYER"
                    "SOUNDBOARD"  -> "SOUND"
                    else          -> w.id
                }

                // Original (pre-auto-expand) spanX needed for drag boundary clamping
                val origSpanX  = visible.find { it.id == w.id }?.spanX ?: 1
                val isDragging = draggingId == w.id
                // Weather with no data reserves its cell but draws nothing
                // (still visible in edit mode so it can be moved/removed)
                val isGhost    = w.id == "WEATHER" && weather == null && !editMode
                val dragDpX    = if (isDragging) with(density) { dragOffsetPx.x.toDp() } else 0.dp
                val dragDpY    = if (isDragging) with(density) { dragOffsetPx.y.toDp() } else 0.dp

                @OptIn(ExperimentalFoundationApi::class)
                Box(
                    modifier = Modifier
                        .absoluteOffset(x = xOff + dragDpX, y = yOff + dragDpY)
                        .size(width, height)
                        .zIndex(if (isDragging) 1f else 0f)
                        .clip(WIDGET_RADIUS)
                        .background(if (isGhost) Color.Transparent else widgetBg)
                        .border(
                            width = if (editMode) 1.5.dp else 1.dp,
                            color = when {
                                editMode -> accent.copy(alpha = 0.45f)
                                isGhost  -> Color.Transparent
                                else     -> widgetBorder
                            },
                            shape = WIDGET_RADIUS
                        )
                        .combinedClickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick           = { if (editMode) contextMenuId = w.id },
                            onLongClick       = { if (!editMode) contextMenuId = w.id }
                        )
                        .then(
                            if (editMode) Modifier.pointerInput(editMode, w.id, w.gridX, w.gridY) {
                                var hasSignificantDrag = false
                                // Touch-slop gate: without it, sub-pixel jitter during a
                                // long-press counts as a drag and the context menu never opens
                                val slop = viewConfiguration.touchSlop
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { _ ->
                                        draggingId         = w.id
                                        dragOffsetPx       = Offset.Zero
                                        hasSignificantDrag = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetPx      += dragAmount
                                        if (!hasSignificantDrag && dragOffsetPx.getDistance() > slop) {
                                            hasSignificantDrag = true
                                        }
                                    },
                                    onDragEnd = {
                                        if (hasSignificantDrag) {
                                            val newX = (w.gridX + (dragOffsetPx.x / cellStepXPx).roundToInt())
                                                .coerceIn(0, GRID_COLS - origSpanX)
                                            val newY = (w.gridY + (dragOffsetPx.y / cellStepYPx).roundToInt())
                                                .coerceIn(0, GRID_ROWS - w.spanY)
                                            onMoveWidget(w.id, newX, newY)
                                        } else {
                                            contextMenuId = w.id
                                        }
                                        draggingId   = null
                                        dragOffsetPx = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggingId   = null
                                        dragOffsetPx = Offset.Zero
                                    }
                                )
                            } else Modifier
                        )
                ) {
                    when (w.id) {
                        "CLOCK" -> ClockWidget(
                            style      = settings.clockStyle,
                            accent     = accent,
                            isDayMode  = isDayMode,
                            modifier   = Modifier.fillMaxSize()
                        )
                        "WEATHER" -> WeatherWidget(
                            state      = weather,
                            accent     = accent,
                            metric     = settings.unitSystem.name == "METRIC",
                            isDayMode  = isDayMode,
                            modifier   = Modifier.fillMaxSize()
                        )
                        "NOW_PLAYING" -> NowPlayingWidget(
                            state               = nowPlaying,
                            accent              = accent,
                            carPlayPackage      = settings.carPlayPackage,
                            androidAutoPackage  = settings.androidAutoPackage,
                            onPlayPause         = onPlayPause,
                            onNext              = onNext,
                            onPrev              = onPrev,
                            onLaunchCarPlay     = onLaunchCarPlay,
                            onLaunchAndroidAuto = onLaunchAndroidAuto,
                            onTapToOpenApp      = onTapNowPlaying,
                            modifier            = Modifier.fillMaxSize(),
                            isEditing           = editMode,
                            isDayMode           = isDayMode,
                            hardwareRadio         = hardwareRadio,
                            onLaunchHardwareRadio = onLaunchHardwareRadio,
                            onStopHardwareRadio   = onStopHardwareRadio,
                            onRadioSeekUp         = onRadioSeekUp,
                            onRadioSeekDown       = onRadioSeekDown,
                            onRadioCycleFm        = onRadioCycleFm,
                            onRadioSwitchAm       = onRadioSwitchAm,
                            onRadioTune           = onRadioTune,
                            onAssignRadio         = onAssignRadio
                        )
                        "RADIO" -> Box(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            RadioDeck(
                                accent = accent,
                                isDayMode = isDayMode,
                                hardwareRadio = hardwareRadio,
                                onLaunchHardwareRadio = onLaunchHardwareRadio,
                                onStopHardwareRadio = onStopHardwareRadio,
                                onRadioSeekUp = onRadioSeekUp,
                                onRadioSeekDown = onRadioSeekDown,
                                onRadioCycleFm = onRadioCycleFm,
                                onRadioSwitchAm = onRadioSwitchAm,
                                onRadioTune = onRadioTune,
                                onAssignRadio = onAssignRadio,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        "TELEMETRY" -> TelemetryWidget(
                            locationFlow  = locationFlow,
                            bearingFlow   = bearingFlow,
                            compassOffset = settings.compassOffset,
                            accent        = accent,
                            isDayMode     = isDayMode,
                            modifier      = Modifier.fillMaxSize()
                        )
                        "ALTIMETER" -> AltimeterWidget(
                            locationFlow  = locationFlow,
                            isMetric      = settings.unitSystem == com.hoopoe.launcher.data.UnitSystem.METRIC,
                            accent        = accent,
                            isDayMode     = isDayMode,
                            modifier      = Modifier.fillMaxSize()
                        )
                        "SPEEDOMETER" -> SpeedometerWidget(
                            locationFlow  = locationFlow,
                            isMetric      = settings.unitSystem == com.hoopoe.launcher.data.UnitSystem.METRIC,
                            accent        = accent,
                            isDayMode     = isDayMode,
                            digitalOnly   = settings.speedometerDigitalOnly,
                            modifier      = Modifier.fillMaxSize()
                        )
                        "NAV_MAP" -> NavigationMapWidget(
                            locationFlow  = locationFlow,
                            bearingFlow   = bearingFlow,
                            compassOffset = settings.compassOffset,
                            accent        = accent,
                            isDayMode     = isDayMode,
                            modifier      = Modifier.fillMaxSize()
                        )
                        "VITALS" -> VitalsWidget(
                            accent    = accent,
                            isDayMode = isDayMode,
                            asBars    = settings.vitalsAsBars,
                            modifier  = Modifier.fillMaxSize()
                        )
                        "TRIP_TRACKER" -> TripTrackerWidget(
                            locationFlow  = locationFlow,
                            isMetric      = settings.unitSystem == com.hoopoe.launcher.data.UnitSystem.METRIC,
                            accent        = accent,
                            isDayMode     = isDayMode,
                            modifier      = Modifier.fillMaxSize()
                        )
                        "FUEL_HUB" -> FuelHubWidget(
                            accent    = accent,
                            isDayMode = isDayMode,
                            onOpenRefuel = { showRefuelDialog = true },
                            modifier  = Modifier.fillMaxSize()
                        )
                        "PRAYER" -> PrayerWidget(
                            prayerTimes = prayerTimes,
                            accent = accent,
                            isDayMode = isDayMode,
                            onClick = onOpenIslamic,
                            modifier = Modifier.fillMaxSize()
                        )
                        "SOUNDBOARD" -> SoundboardWidget(
                            pads      = settings.soundboardPads,
                            accent    = accent,
                            isDayMode = isDayMode,
                            isEditing = editMode,
                            onUpdatePad = onUpdateSoundPad,
                            modifier  = Modifier.fillMaxSize()
                        )
                    }

                    // Label — hide when album art fills the widget background
                    val labelColor = when {
                        isGhost -> Color.Transparent
                        w.id == "NOW_PLAYING" && nowPlaying?.albumArt != null && nowPlaying.title.isNotEmpty() -> Color.Transparent
                        isDayMode -> Color(0xFF999999)
                        else      -> Color(0xFF3A3A3A)
                    }
                    Text(
                        text          = label,
                        style         = MaterialTheme.typography.labelSmall,
                        color         = labelColor,
                        letterSpacing = 2.sp,
                        fontSize      = 8.sp,
                        modifier      = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 10.dp, top = 7.dp)
                    )
                }
            }
        }
    }

    // ── Widget context menu (long-press any cell) ────────────────────────────
    contextMenuId?.let { id ->
        WidgetContextMenu(
            widgetId            = id,
            accent              = accent,
            clockStyle          = settings.clockStyle,
            vitalsAsBars        = settings.vitalsAsBars,
            speedometerDigitalOnly = settings.speedometerDigitalOnly,
            carPlayPackage      = settings.carPlayPackage,
            androidAutoPackage  = settings.androidAutoPackage,
            pipAppPackage       = settings.pipAppPackage,
            isDayMode           = isDayMode,
            onResize            = { contextMenuId = null; resizingId = id },
            onAssignCarPlay     = { contextMenuId = null; onAssignCarPlay() },
            onAssignAndroidAuto = { contextMenuId = null; onAssignAndroidAuto() },
            onClearCarPlay      = { contextMenuId = null; onClearCarPlay() },
            onClearAndroidAuto  = { contextMenuId = null; onClearAndroidAuto() },
            onAssignPip         = { contextMenuId = null; onAssignPip() },
            onClearPip          = { contextMenuId = null; onClearPip() },
            onSetClockStyle     = { onSetClockStyle(it) },
            onSetVitalsAsBars   = { onSetVitalsAsBars(it) },
            onSetSpeedometerDigitalOnly = { onSetSpeedometerDigitalOnly(it) },
            onDismiss           = { contextMenuId = null }
        )
    }

    // ── Resize dialog ────────────────────────────────────────────────────────
    resizingId?.let { id ->
        val config = settings.widgetLayout.find { it.id == id }
        if (config != null) {
            WidgetResizeDialog(
                config    = config,
                accent    = accent,
                isDayMode = isDayMode,
                onDismiss = { resizingId = null },
                onConfirm = { sx, sy ->
                    onUpdateWidget(id, sx, sy)
                    resizingId = null
                }
            )
        }
    }

        // Lowest-Middle MDUS OS Automotive Logo Space & Controls Dock
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .zIndex(2f)
        ) {
            MdusOsDock(
                accent = accent,
                isDayMode = isDayMode,
                onClick = onOpenQuickControls
            )
        }

        // ── Widget library ────────────────────────────────────────────────────────
        if (widgetLibraryOpen) {
            WidgetLibraryDialog(
                settings  = settings,
                accent    = accent,
                isDayMode = isDayMode,
                onAdd     = { id -> onAddWidget(id) },
                onRemove  = { id -> onRemoveWidget(id) },
                onDismiss = { widgetLibraryOpen = false }
            )
        }

        if (showRefuelDialog) {
            com.hoopoe.launcher.ui.components.RefuelDialog(
                accent = accent,
                onDismiss = { showRefuelDialog = false }
            )
        }
    }
}

@Composable
private fun WidgetContextMenu(
    widgetId: String,
    accent: Color,
    clockStyle: ClockStyle,
    vitalsAsBars: Boolean,
    speedometerDigitalOnly: Boolean,
    carPlayPackage: String = "",
    androidAutoPackage: String = "",
    pipAppPackage: String = "",
    isDayMode: Boolean,
    onResize: () -> Unit,
    onAssignCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onClearCarPlay: () -> Unit,
    onClearAndroidAuto: () -> Unit,
    onAssignPip: () -> Unit,
    onClearPip: () -> Unit,
    onSetClockStyle: (ClockStyle) -> Unit,
    onSetVitalsAsBars: (Boolean) -> Unit,
    onSetSpeedometerDigitalOnly: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val menuBg    = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val menuBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF1E1E1E)
    val menuDivider = if (isDayMode) Color(0xFFF1F3F5) else Color(0xFF1A1A1A)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(menuBg)
                .border(1.dp, menuBorder, RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp)
                .width(200.dp)
        ) {
            val inactiveMenuTint = if (isDayMode) Color(0xFF777777) else Color(0xFF555555)
            ContextRow("RESIZE", Icons.Default.OpenWith, accent, onResize, isDayMode = isDayMode)
            if (widgetId == "CLOCK") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIGITAL",
                    icon    = Icons.Default.Schedule,
                    tint    = if (clockStyle == ClockStyle.DIGITAL) accent else inactiveMenuTint,
                    onClick = { onSetClockStyle(ClockStyle.DIGITAL); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "ANALOG",
                    icon    = Icons.Default.Watch,
                    tint    = if (clockStyle == ClockStyle.ANALOG) accent else inactiveMenuTint,
                    onClick = { onSetClockStyle(ClockStyle.ANALOG); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "VITALS") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIAL GAUGES",
                    icon    = Icons.Default.Adjust,
                    tint    = if (!vitalsAsBars) accent else inactiveMenuTint,
                    onClick = { onSetVitalsAsBars(false); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "BARS VIEW",
                    icon    = Icons.Default.FormatAlignLeft,
                    tint    = if (vitalsAsBars) accent else inactiveMenuTint,
                    onClick = { onSetVitalsAsBars(true); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "SPEEDOMETER") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIAL TRACK",
                    icon    = Icons.Default.Speed,
                    tint    = if (!speedometerDigitalOnly) accent else inactiveMenuTint,
                    onClick = { onSetSpeedometerDigitalOnly(false); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIGITAL ONLY",
                    icon    = Icons.Default.Dialpad,
                    tint    = if (speedometerDigitalOnly) accent else inactiveMenuTint,
                    onClick = { onSetSpeedometerDigitalOnly(true); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "NOW_PLAYING") {
                HorizontalDivider(color = menuDivider)
                ContextRow("ASSIGN CARPLAY APP",      Icons.Default.PhoneAndroid,  accent, onAssignCarPlay, isDayMode = isDayMode)
                if (carPlayPackage.isNotEmpty()) {
                    HorizontalDivider(color = menuDivider)
                    ContextRow("CLEAR CARPLAY APP", Icons.Default.PhoneAndroid, Color(0xFF884444), onClearCarPlay, isDayMode = isDayMode)
                }
                HorizontalDivider(color = menuDivider)
                ContextRow("ASSIGN ANDROID AUTO APP", Icons.Default.DirectionsCar, accent, onAssignAndroidAuto, isDayMode = isDayMode)
                if (androidAutoPackage.isNotEmpty()) {
                    HorizontalDivider(color = menuDivider)
                    ContextRow("CLEAR ANDROID AUTO APP", Icons.Default.DirectionsCar, Color(0xFF884444), onClearAndroidAuto, isDayMode = isDayMode)
                }
            }

        }
    }
}

@Composable
private fun ContextRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    isDayMode: Boolean = false
) {
    val finalTint = if (isDayMode) {
        if (tint == Color(0xFF884444)) {
            tint
        } else if (tint == Color(0xFF777777)) {
            Color(0xFF888888)
        } else {
            Color(0xFF111111)
        }
    } else {
        tint
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = finalTint, modifier = Modifier.size(16.dp))
        Text(label, color = finalTint, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun WidgetResizeDialog(
    config: WidgetConfig,
    accent: Color,
    isDayMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (spanX: Int, spanY: Int) -> Unit
) {
    var spanX by remember { mutableStateOf(config.spanX) }
    var spanY by remember { mutableStateOf(config.spanY) }

    val maxSpanX = GRID_COLS - config.gridX
    val maxSpanY = GRID_ROWS - config.gridY

    val dialogBg     = if (isDayMode) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.background
    val dialogText   = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val cancelColor  = if (isDayMode) Color(0xFF6C757D) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text          = config.id.replace('_', ' '),
                color         = dialogText,
                fontSize      = 11.sp,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SpanRow(label = "WIDTH",  value = spanX, min = 1, max = maxSpanX, accent = accent, isDayMode = isDayMode) { spanX = it }
                SpanRow(label = "HEIGHT", value = spanY, min = 1, max = maxSpanY, accent = accent, isDayMode = isDayMode) { spanY = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(spanX, spanY) }) {
                Text("APPLY", color = accent, fontSize = 11.sp, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = cancelColor, fontSize = 11.sp, letterSpacing = 1.sp)
            }
        },
        containerColor    = dialogBg,
        titleContentColor = dialogText,
        textContentColor  = dialogText
    )
}

@Composable
private fun SpanRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    accent: Color,
    isDayMode: Boolean,
    onChange: (Int) -> Unit
) {
    val textColor   = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dimColor    = if (isDayMode) Color(0xFF495057) else Color(0xFF666666)
    val disabledC   = if (isDayMode) Color(0xFFCED4DA) else Color(0xFF333333)
    val inactiveBg  = if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF2A2A2A)
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text          = label,
            color         = dimColor,
            fontSize      = 10.sp,
            letterSpacing = 1.sp,
            modifier      = Modifier.width(52.dp)
        )
        IconButton(
            onClick  = { if (value > min) onChange(value - 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Remove, null,
                tint     = if (value > min) textColor else disabledC,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text      = "$value",
            color     = textColor,
            fontSize  = 16.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.width(24.dp)
        )
        IconButton(
            onClick  = { if (value < max) onChange(value + 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Add, null,
                tint     = if (value < max) accent else disabledC,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(max) { i ->
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 10.dp)
                        .background(
                            if (i < value) accent.copy(alpha = 0.7f) else inactiveBg,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

// ── Widget Library ────────────────────────────────────────────────────────────

@Composable
private fun WidgetLibraryDialog(
    settings: AppSettings,
    accent: Color,
    isDayMode: Boolean,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dialogBg    = if (isDayMode) Color(0xFFEEEEEE) else Color(0xFF0C0C0C)
    val dialogBorder = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1E1E1E)
    val titleColor  = if (isDayMode) Color(0xFF495057) else Color(0xFF555555)
    val closeColor  = if (isDayMode) Color(0xFF495057) else Color(0xFF444444)

    val activeIds = buildSet {
        if (settings.showClock) add("CLOCK")
        if (settings.showWeather) add("WEATHER")
        if (settings.showNowPlaying) add("NOW_PLAYING")
        if (settings.showRadio) add("RADIO")
        if (settings.showTelemetry) add("TELEMETRY")
        if (settings.showAltimeter) add("ALTIMETER")
        if (settings.showSpeedometer) add("SPEEDOMETER")
        if (settings.showNavMap) add("NAV_MAP")
        if (settings.showVitals) add("VITALS")
        if (settings.showTripTracker) add("TRIP_TRACKER")
        if (settings.showFuelHub) add("FUEL_HUB")
        if (settings.showSoundboard) add("SOUNDBOARD")
    }
    val canAdd = canAddWidget(settings)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(4.dp))
                .padding(16.dp)
                .widthIn(min = 320.dp, max = 520.dp)
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text          = "WIDGET LIBRARY",
                    color         = titleColor,
                    fontSize      = 9.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = closeColor, modifier = Modifier.size(14.dp))
                }
            }

            LazyVerticalGrid(
                columns               = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                items(ALL_WIDGET_TYPES) { info ->
                    val isActive = info.id in activeIds
                    WidgetLibraryCard(
                        info     = info,
                        isActive = isActive,
                        canAdd   = canAdd,
                        accent   = accent,
                        isDayMode = isDayMode,
                        onToggle = { if (isActive) onRemove(info.id) else onAdd(info.id) }
                    )
                }
            }

            if (!canAdd) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text          = "ALL ${GRID_COLS * GRID_ROWS} CELLS OCCUPIED — REMOVE A WIDGET TO ADD MORE",
                    color         = if (isDayMode) Color(0xFFE03131) else Color(0xFF3A3A3A),
                    fontSize      = 8.sp,
                    letterSpacing = 1.sp,
                    modifier      = Modifier.fillMaxWidth(),
                    textAlign     = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WidgetLibraryCard(
    info: WidgetTypeInfo,
    isActive: Boolean,
    canAdd: Boolean,
    accent: Color,
    isDayMode: Boolean,
    onToggle: () -> Unit
) {
    val enabled    = isActive || canAdd
    val cardBorder = if (isActive) accent else if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1A1A1A)
    val cardBg     = if (isActive) accent.copy(alpha = 0.15f) else if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF0E0E0E)
    val iconTint   = if (isActive) accent else if (isDayMode) Color(0xFF495057) else Color(0xFF333333)
    val labelColor = if (isActive) accent else if (isDayMode) Color(0xFF212529) else Color(0xFF3A3A3A)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(info.icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(5.dp))
        Text(
            text          = info.label,
            color         = labelColor,
            fontSize      = 7.sp,
            letterSpacing = 1.sp,
            textAlign     = TextAlign.Center,
            maxLines      = 2,
            lineHeight    = 9.sp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text          = when {
                isActive -> "ACTIVE"
                !canAdd  -> "FULL"
                else     -> "ADD"
            },
            color         = when {
                isActive -> accent.copy(alpha = 0.75f)
                !canAdd  -> if (isDayMode) Color(0xFFADB5BD) else Color(0xFF282828)
                else     -> if (isDayMode) Color(0xFF495057) else Color(0xFF3A3A3A)
            },
            fontSize      = 6.sp,
            letterSpacing = 1.sp,
            textAlign     = TextAlign.Center
        )
    }
}
