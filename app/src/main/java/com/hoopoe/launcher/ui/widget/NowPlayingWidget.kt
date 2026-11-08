package com.hoopoe.launcher.ui.widget

import android.content.Intent
import android.media.MediaMetadata
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.model.NowPlayingState
import com.hoopoe.launcher.service.MediaListenerService
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun NowPlayingWidget(
    state: NowPlayingState?,
    accent: Color,
    carPlayPackage: String,
    androidAutoPackage: String,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onTapToOpenApp: () -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    isDayMode: Boolean = false,
    hardwareRadio: com.hoopoe.launcher.viewmodel.LauncherViewModel.HardwareRadioState? = null,
    onLaunchHardwareRadio: () -> Unit = {},
    onStopHardwareRadio: () -> Unit = {},
    onRadioSeekUp: () -> Unit = {},
    onRadioSeekDown: () -> Unit = {},
    onRadioCycleFm: () -> Unit = {},
    onRadioSwitchAm: () -> Unit = {},
    onRadioTune: (band: String, freq: Float) -> Unit = { _, _ -> },
    onAssignRadio: () -> Unit = {}
) {
    val context     = LocalContext.current
    val isConnected by MediaListenerService.isConnected.collectAsState()
    val hasCarPlay  = carPlayPackage.isNotEmpty()
    val hasAutoApp  = androidAutoPackage.isNotEmpty()
    val hasContent  = state != null && state.title.isNotEmpty()

    var selectedSource by rememberSaveable { mutableStateOf("Any Player") }

    // Auto-switch to radio view when hardware radio is detected.
    val hasHardwareRadio = hardwareRadio != null
    LaunchedEffect(hasHardwareRadio) {
        if (hasHardwareRadio) selectedSource = "FM/AM Radio"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // 1. CONDITIONAL VIEW TOGGLE
        if (selectedSource == "FM/AM Radio") {
            // Real-tuner radio deck — mirrors the MCU or the radio app's session
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)
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
        } else {
            // Standard Elegant Modern Media Player
            StandardMinimalPlayer(
                state = state,
                accent = accent,
                hasContent = hasContent,
                isEditing = isEditing,
                isDayMode = isDayMode,
                isConnected = isConnected,
                hasCarPlay = hasCarPlay,
                hasAutoApp = hasAutoApp,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrev = onPrev,
                onLaunchCarPlay = onLaunchCarPlay,
                onLaunchAndroidAuto = onLaunchAndroidAuto,
                onTapToOpenApp = onTapToOpenApp,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. QUICK SOURCE TOGGLE (Top-Right, always overlayed)
        val isRadio = selectedSource == "FM/AM Radio"
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isRadio) accent.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.45f))
                .border(1.dp, if (isRadio) accent else Color(0xFF243048), RoundedCornerShape(8.dp))
                .clickable {
                    selectedSource = if (isRadio) "Any Player" else "FM/AM Radio"
                }
                .padding(horizontal = 7.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isRadio) Icons.Default.MusicNote else Icons.Default.Radio,
                    contentDescription = null,
                    tint = if (isRadio) accent else Color(0xFF94A3B8),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = if (isRadio) "PLAYER" else "FM RADIO",
                    color = if (isRadio) accent else Color(0xFFCBD5E1),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}


/**
 * Radio deck backed by a REAL tuner only — either the vendor MCU (full control,
 * canTune = true) or the radio app's MediaSession (seek/open/stop, read-only
 * band). No simulated static, no demo stations: when no source exists the deck
 * says so and offers to assign the unit's radio app.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RadioDeck(
    accent: Color,
    isDayMode: Boolean,
    hardwareRadio: com.hoopoe.launcher.viewmodel.LauncherViewModel.HardwareRadioState?,
    onLaunchHardwareRadio: () -> Unit,
    onStopHardwareRadio: () -> Unit,
    onRadioSeekUp: () -> Unit,
    onRadioSeekDown: () -> Unit,
    onRadioCycleFm: () -> Unit,
    onRadioSwitchAm: () -> Unit,
    onRadioTune: (band: String, freq: Float) -> Unit,
    onAssignRadio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dimColor     = if (isDayMode) Color(0xFF444444) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val borderColor  = if (isDayMode) Color(0xFF777777) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)

    // Standalone fallback tuner state so radio deck functions as a real widget
    var manualFreq by rememberSaveable { mutableFloatStateOf(98.3f) }
    var manualBand by rememberSaveable { mutableStateOf("FM1") }
    var manualPower by rememberSaveable { mutableStateOf(true) }

    val isConnected = hardwareRadio != null
    var isManuallyOff by remember { mutableStateOf(false) }
    LaunchedEffect(hardwareRadio) {
        if (isManuallyOff) isManuallyOff = false
    }
    val powerOn = if (isConnected) !isManuallyOff else manualPower

    val displayBand = if (isConnected) hardwareRadio!!.band.uppercase() else manualBand
    val displayFreq = if (isConnected) {
        val freqClean = hardwareRadio!!.freq.replace(Regex("[^0-9.]"), "")
        freqClean.ifEmpty { hardwareRadio!!.freq }
    } else {
        String.format(java.util.Locale.US, "%.1f", manualFreq)
    }
    val freqFloat = if (isConnected) displayFreq.toFloatOrNull() else manualFreq
    val displayUnit = if (isConnected && hardwareRadio!!.isAm) "kHz" else "MHz"

    val chipInactiveBg = if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val chipActiveBg   = if (isDayMode) Color(0xFF222222) else Color(0xFFDDDDDD)
    val chipActiveText = if (isDayMode) Color.White else Color(0xFF111111)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Row 1: band (chips when switchable) + power
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("FM1", "FM2", "FM3", "AM").forEach { b ->
                    val active = displayBand == b
                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (active) chipActiveBg else chipInactiveBg)
                            .border(1.dp, if (active) borderColor else borderColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .clickable {
                                if (!isConnected) manualBand = b
                                if (b == "AM") onRadioSwitchAm() else onRadioCycleFm()
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            b,
                            color = if (active) chipActiveText else dimColor,
                            fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (powerOn) chipActiveBg else chipInactiveBg)
                    .border(1.5.dp, borderColor, CircleShape)
                    .clickable {
                        if (powerOn) {
                            if (!isConnected) manualPower = false
                            onStopHardwareRadio()
                            isManuallyOff = true
                        } else {
                            if (!isConnected) manualPower = true
                            onLaunchHardwareRadio()
                            isManuallyOff = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew, "PWR",
                    tint = if (powerOn) chipActiveText else dimColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── Row 2: live frequency + station/RDS line ──────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    displayFreq,
                    color = if (powerOn) contentColor else contentColor.copy(alpha = 0.3f),
                    fontSize = 34.sp, fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace, letterSpacing = 0.sp
                )
                Text(
                    displayUnit,
                    color = dimColor, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = (if (!powerOn) "RADIO OFF" else (hardwareRadio?.stationName ?: "LIVE")).uppercase(),
                    color = if (powerOn) accent else dimColor.copy(alpha = 0.5f),
                    fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (powerOn) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent))
                }
            }
        }

        // ── Row 3: seek + open ────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RadioFlatButton(
                label = "◄ SEEK", enabled = powerOn, active = false,
                accent = accent, borderColor = borderColor, dimColor = dimColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!isConnected) {
                        if (manualBand.startsWith("FM")) {
                            manualFreq = (manualFreq - 0.2f).let { if (it < 87.5f) 108.0f else it }
                        } else {
                            manualFreq = (manualFreq - 10f).let { if (it < 530f) 1600f else it }
                        }
                    }
                    onRadioSeekDown()
                }
            )
            RadioFlatButton(
                label = "RADIO APP", enabled = true, active = false,
                accent = accent, borderColor = borderColor, dimColor = dimColor,
                modifier = Modifier.weight(1f),
                onClick = onLaunchHardwareRadio
            )
            RadioFlatButton(
                label = "SEEK ►", enabled = powerOn, active = false,
                accent = accent, borderColor = borderColor, dimColor = dimColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!isConnected) {
                        if (manualBand.startsWith("FM")) {
                            manualFreq = (manualFreq + 0.2f).let { if (it > 108.0f) 87.5f else it }
                        } else {
                            manualFreq = (manualFreq + 10f).let { if (it > 1600f) 530f else it }
                        }
                    }
                    onRadioSeekUp()
                }
            )
        }

        // ── Presets: Available both on hardware CAN-bus and standalone widget mode ──
        val canTuneDirectly = if (isConnected) hardwareRadio!!.canTune else true
        if (canTuneDirectly) {
            var fmPresets by rememberSaveable { mutableStateOf(listOf(88.5f, 91.1f, 98.3f, 101.9f, 104.3f, 107.5f)) }
            var amPresets by rememberSaveable { mutableStateOf(listOf(540f, 680f, 820f, 1040f, 1260f, 1420f)) }
            val isFm           = if (isConnected) !hardwareRadio!!.isAm else (displayBand.startsWith("FM"))
            val currentPresets = if (isFm) fmPresets else amPresets
            val tolerance      = if (isFm) 0.15f else 5f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (pIdx in 0 until 6) {
                    val presetFreq = currentPresets[pIdx]
                    val isTuned    = freqFloat != null && abs(freqFloat - presetFreq) < tolerance
                    val presetBg = when {
                        isTuned && isDayMode -> Color(0xFF222222)
                        isTuned              -> accent.copy(alpha = 0.12f)
                        else                 -> Color.Transparent
                    }
                    val presetBorderColor = when {
                        isTuned && isDayMode -> Color(0xFF222222)
                        isTuned              -> accent
                        isDayMode            -> Color(0xFFCCCCCC)
                        else                 -> Color(0xFF1D2024)
                    }
                    val presetNumColor = when {
                        isTuned && isDayMode -> Color.White
                        isTuned              -> accent
                        isDayMode            -> Color(0xFF444444)
                        else                 -> Color(0xFF777777)
                    }
                    val presetFreqColor = when {
                        isTuned && isDayMode -> Color.White.copy(alpha = 0.9f)
                        isTuned              -> accent.copy(alpha = 0.9f)
                        isDayMode            -> Color(0xFF666666)
                        else                 -> Color(0xFF777777).copy(alpha = 0.7f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .border(1.dp, presetBorderColor, RoundedCornerShape(2.dp))
                            .clip(RoundedCornerShape(2.dp))
                            .background(presetBg)
                            .combinedClickable(
                                enabled = powerOn,
                                onClick = {
                                    if (!isConnected) manualFreq = presetFreq
                                    onRadioTune(displayBand, presetFreq)
                                },
                                onLongClick = {
                                    // Store the current frequency on this preset
                                    freqFloat?.let { f ->
                                        if (isFm) fmPresets = fmPresets.toMutableList().also { it[pIdx] = f }
                                        else      amPresets = amPresets.toMutableList().also { it[pIdx] = f }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text("${pIdx + 1}", color = presetNumColor, fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(
                                if (isFm) "%.1f".format(presetFreq) else "%.0f".format(presetFreq),
                                color = presetFreqColor, fontSize = 6.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioFlatButton(
    label: String,
    enabled: Boolean,
    active: Boolean,
    accent: Color,
    borderColor: Color,
    dimColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .border(1.dp, if (active) accent else borderColor, RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
            .background(if (active) accent.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = when {
                !enabled -> dimColor.copy(alpha = 0.35f)
                active   -> accent
                else     -> dimColor
            },
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun StandardMinimalPlayer(
    state: NowPlayingState?,
    accent: Color,
    hasContent: Boolean,
    isEditing: Boolean,
    isDayMode: Boolean,
    isConnected: Boolean,
    hasCarPlay: Boolean,
    hasAutoApp: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onTapToOpenApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // UI Theme colors
    val idleIconColor = if (isDayMode) Color(0xFF555555) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)
    val idleTextColor = if (isDayMode) Color(0xFF555555) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)
    val contentTextColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subTextColor = if (isDayMode) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)

    val hasNotificationAccess = remember(context) {
        try {
            val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            flat != null && flat.contains(context.packageName)
        } catch (_: Exception) { false }
    }

    Box(modifier = modifier) {
        if (!hasContent) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Album cover placeholder + Track info (clickable to open music app)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .let { if (!isEditing) it.clickable { onTapToOpenApp() } else it },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!hasNotificationAccess) "Enable Media Access" else "Smart Music Player",
                            style = MaterialTheme.typography.titleMedium,
                            color = contentTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (!hasNotificationAccess) "Tap here to allow widget to read music" else "Tap Play to resume • Touch to browse",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!hasNotificationAccess) Color(0xFFFF9100) else subTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp
                        )
                    }
                }

                // Center: Quick Installed Music Apps Selector or Ambient Visualizer
                val installedMusicApps = remember(context) {
                    val pm = context.packageManager
                    val candidates = listOf(
                        "com.spotify.music" to "Spotify",
                        "org.videolan.vlc" to "VLC",
                        "com.google.android.apps.youtube.music" to "YT Music",
                        "com.amazon.mp3" to "Amazon",
                        "com.apple.android.music" to "Apple Music",
                        "com.jio.media.jiobeats" to "JioSaavn",
                        "com.gaana" to "Gaana",
                        "com.android.music" to "Music"
                    )
                    candidates.filter { (pkg, _) ->
                        try { pm.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }
                    }
                }

                if (installedMusicApps.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        installedMusicApps.take(3).forEach { (pkg, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accent.copy(alpha = 0.12f))
                                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        try {
                                            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                                            if (intent != null) {
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            }
                                        } catch (_: Exception) {}
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else if (!hasNotificationAccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF9100).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFFF9100).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .clickable {
                                try {
                                    val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "TAP TO GRANT NOTIFICATION ACCESS FOR LIVE METADATA",
                            color = Color(0xFFFF9100),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .padding(vertical = 2.dp)
                    ) {
                        val barCount = 20
                        val spacing = 2.dp.toPx()
                        val barWidth = (size.width - (barCount - 1) * spacing) / barCount
                        for (i in 0 until barCount) {
                            drawRect(
                                color = accent.copy(alpha = 0.25f),
                                topLeft = androidx.compose.ui.geometry.Offset(i * (barWidth + spacing), size.height - 2.5.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(barWidth, 2.5.dp.toPx())
                            )
                        }
                    }
                }

                // Bottom: Playback Controls (Prev, 48dp Play, Next)
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                ) {
                    IconButton(onClick = { if (!isEditing) onPrev() }, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Default.SkipPrevious, "Prev", tint = contentTextColor.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accent)
                    ) {
                        IconButton(onClick = { if (!isEditing) onPlayPause() }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color(0xFF07090E),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    IconButton(onClick = { if (!isEditing) onNext() }, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Default.SkipNext, "Next", tint = contentTextColor.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                    }
                }
            }
        } else {
            // Non-null playing track state
            val nonNullState = state!!
            var positionMs by remember { mutableLongStateOf(nonNullState.controller?.playbackState?.position ?: 0L) }
            val durationMs = nonNullState.controller?.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

            LaunchedEffect(nonNullState.isPlaying, nonNullState.title) {
                while (nonNullState.isPlaying) {
                    positionMs = nonNullState.controller?.playbackState?.position ?: positionMs
                    delay(500)
                }
            }

            // Draw Album Art as background with smooth blur overlay if present
            val hasAlbumArt = nonNullState.albumArt != null
            val useDarkTheme = hasAlbumArt || !isDayMode

            val currentTextColor = if (hasAlbumArt) Color.White else if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
            val currentSubTextColor = if (hasAlbumArt) Color.White.copy(alpha = 0.6f) else if (isDayMode) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            val currentProgressColor = if (useDarkTheme) accent else if (isDayMode) Color(0xFF111111) else accent
            val currentProgressTrack = currentTextColor.copy(alpha = 0.15f)
            val currentIconColor = currentTextColor.copy(alpha = 0.75f)
            val currentPlayBgColor = if (useDarkTheme) accent.copy(alpha = 0.9f) else if (isDayMode) Color(0xFF111111) else accent.copy(alpha = 0.9f)
            val currentPlayIconColor = if (useDarkTheme) Color.Black else Color.White

            if (hasAlbumArt) {
                // Prefer the full-resolution art URI when the source app provides
                // one — the metadata bitmap is often a downscaled notification
                // thumbnail that looks soft stretched across the widget. Falls back
                // to the bitmap if the URI fails to load, and renders with high
                // filter quality so upscaling stays smooth either way.
                coil.compose.AsyncImage(
                    model = nonNullState.artUri ?: nonNullState.albumArt,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                    error = nonNullState.albumArt?.let {
                        androidx.compose.ui.graphics.painter.BitmapPainter(it.asImageBitmap())
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // 25% dimming layer overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Track info (top — clickable to open app)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .let { if (!isEditing) it.clickable { onTapToOpenApp() } else it }
                ) {
                    Text(
                        text = nonNullState.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = currentTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    Text(
                        text = nonNullState.artist.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.bodySmall,
                        color = currentSubTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }

                // Progress + controls (bottom)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Live Audio Frequency Visualizer Spectrum
                    if (nonNullState.isPlaying) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp).padding(vertical = 2.dp)) {
                            val barCount = 20
                            val spacing = 2.dp.toPx()
                            val barWidth = (size.width - (barCount - 1) * spacing) / barCount
                            val time = System.currentTimeMillis()
                            for (i in 0 until barCount) {
                                val freq = kotlin.math.abs(
                                    kotlin.math.sin(time * 0.005 + i * 0.45) * 
                                    kotlin.math.cos(time * 0.003 - i * 0.25)
                                ).toFloat()
                                val barHeight = (freq * size.height).coerceIn(2.dp.toPx(), size.height)
                                drawRect(
                                    color = accent,
                                    topLeft = androidx.compose.ui.geometry.Offset(i * (barWidth + spacing), size.height - barHeight),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                                )
                            }
                        }
                    }

                    if (durationMs > 0) {
                        LinearProgressIndicator(
                            progress = { (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = currentProgressColor,
                            trackColor = currentProgressTrack
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatMs(positionMs), style = MaterialTheme.typography.labelSmall, color = currentSubTextColor.copy(alpha = 0.75f), fontSize = 9.sp)
                            Text(formatMs(durationMs), style = MaterialTheme.typography.labelSmall, color = currentSubTextColor.copy(alpha = 0.75f), fontSize = 9.sp)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    ) {
                        IconButton(onClick = { if (!isEditing) onPrev() }, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Default.SkipPrevious, "Prev", tint = currentIconColor, modifier = Modifier.size(24.dp))
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(currentPlayBgColor)
                        ) {
                            IconButton(onClick = { if (!isEditing) onPlayPause() }, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    imageVector = if (nonNullState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (nonNullState.isPlaying) "Pause" else "Play",
                                    tint = currentPlayIconColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        IconButton(onClick = { if (!isEditing) onNext() }, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Default.SkipNext, "Next", tint = currentIconColor, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

