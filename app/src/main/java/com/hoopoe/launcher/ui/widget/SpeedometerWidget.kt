package com.hoopoe.launcher.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.data.TripFuelRepository
import com.hoopoe.launcher.integration.obd.ObdConnectionState
import com.hoopoe.launcher.integration.obd.ObdManager
import com.hoopoe.launcher.util.LocationData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerWidget(
    locationFlow: StateFlow<LocationData?>,
    isMetric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    digitalOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val location by locationFlow.collectAsStateWithLifecycle()
    SpeedometerWidget(
        location = location,
        isMetric = isMetric,
        accent = accent,
        isDayMode = isDayMode,
        digitalOnly = digitalOnly,
        modifier = modifier
    )
}

@Composable
fun SpeedometerWidget(
    location: LocationData?,
    isMetric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    digitalOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fuelRepo = remember { TripFuelRepository.getInstance(context) }
    val fuelPct by fuelRepo.fuelPercent.collectAsState()
    val obdTelemetry by ObdManager.getInstance(context).telemetry.collectAsStateWithLifecycle()

    val maxSpeed     = if (isMetric) 200f else 124f
    val rawSpeed     = ((location?.speedMps ?: 0f) * if (isMetric) 3.6f else 2.237f).coerceAtLeast(0f)
    val speedDisplay by androidx.compose.animation.core.animateFloatAsState(
        targetValue = rawSpeed,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "gps_speed"
    )
    val unitLabel    = if (isMetric) "KM/H" else "MPH"
    val trackAlpha   = if (isDayMode) 0.18f else 0.12f

    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val tickBaseColor = if (isDayMode) Color(0xFF334155) else Color(0xFF94A3B8)

    // Interactive Metric Mode: "SPEED" | "RPM" | "PETROL"
    var metricMode by remember { mutableStateOf("SPEED") }
    // Interactive Gear State (Defaults to Drive "D")
    var currentGear by remember { mutableStateOf("D") }

    // Rolling Speed Telemetry History for Futuristic Neon Graph (Immutable List - Thread Safe)
    var speedHistory by remember { mutableStateOf(List(25) { 0f }) }
    var peakSpeed by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(rawSpeed) {
        if (rawSpeed > peakSpeed) peakSpeed = rawSpeed
        speedHistory = (speedHistory.drop(1) + rawSpeed)
    }

    val metricProgress = when (metricMode) {
        "RPM" -> {
            val isObd = obdTelemetry.connectionState == ObdConnectionState.CONNECTED
            val rpmVal = obdTelemetry.rpm
            if (isObd && rpmVal != null) {
                MetricItem(
                    progressVal = rpmVal.toFloat(),
                    maxVal = 7000f,
                    displayText = "$rpmVal",
                    displayUnit = "OBD RPM",
                    color = if (rpmVal > 5500) Color(0xFFFF1744) else accent
                )
            } else {
                MetricItem(
                    progressVal = 0f,
                    maxVal = 7000f,
                    displayText = "NA",
                    displayUnit = "RPM (NO OBD)",
                    color = Color(0xFF64748B)
                )
            }
        }
        "PETROL" -> {
            val isObd = obdTelemetry.connectionState == ObdConnectionState.CONNECTED
            val obdFuel = obdTelemetry.fuelLevelPct
            if (isObd && obdFuel != null) {
                MetricItem(
                    progressVal = obdFuel,
                    maxVal = 100f,
                    displayText = "%.0f%%".format(obdFuel),
                    displayUnit = "OBD FUEL",
                    color = if (obdFuel < 20f) Color(0xFFFF9100) else accent
                )
            } else if (fuelPct != null && fuelPct!! > 0f) {
                MetricItem(
                    progressVal = fuelPct!!,
                    maxVal = 100f,
                    displayText = "%.0f%%".format(fuelPct!!),
                    displayUnit = "LOGGED FUEL",
                    color = if (fuelPct!! < 20f) Color(0xFFFF9100) else accent
                )
            } else {
                MetricItem(
                    progressVal = 0f,
                    maxVal = 100f,
                    displayText = "NA",
                    displayUnit = "FUEL (NO OBD)",
                    color = Color(0xFF64748B)
                )
            }
        }
        else -> {
            MetricItem(
                progressVal = speedDisplay,
                maxVal = maxSpeed,
                displayText = "%.0f".format(speedDisplay),
                displayUnit = unitLabel,
                color = accent
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Sub-header: Cluster Title & Metric Ring Mode Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TELEMETRY",
                color = contentColor.copy(alpha = 0.45f),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Metric Switcher Chips: SPEED | RPM | PETROL
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color(0xFF243048), RoundedCornerShape(10.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("SPEED", "RPM", "PETROL").forEach { mode ->
                    val isSelected = metricMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accent else Color.Transparent)
                            .clickable {
                                TripFuelRepository.playTactileBeep()
                                metricMode = mode
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            color = if (isSelected) Color(0xFF07090E) else contentColor.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Center: Radial Dial or Digital Readout
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val currentVal = metricProgress.progressVal
            val maxVal = metricProgress.maxVal
            val displayUnit = metricProgress.displayUnit
            val gaugeColor = metricProgress.color
            val isNa = metricProgress.displayText == "NA"

            if (digitalOnly) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text          = metricProgress.displayText,
                        color         = contentColor,
                        fontSize      = 54.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = (-2).sp,
                        fontFamily    = FontFamily.Monospace
                    )
                    Text(
                        text          = displayUnit,
                        color         = gaugeColor,
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx         = size.width  / 2f
                    val cy         = size.height / 2f
                    val arcR       = minOf(size.width, size.height) * 0.42f
                    val trackW     = arcR * 0.12f
                    val startAngle = 145f
                    val sweepTotal = 250f
                    val progressSweep = if (isNa) 0f else (currentVal / maxVal).coerceIn(0f, 1f) * sweepTotal

                    val tl = Offset(cx - arcR, cy - arcR)
                    val sz = Size(arcR * 2f, arcR * 2f)

                    // Track Background
                    drawArc(
                        color      = contentColor.copy(alpha = trackAlpha),
                        startAngle = startAngle,
                        sweepAngle = sweepTotal,
                        useCenter  = false,
                        topLeft    = tl,
                        size       = sz,
                        style      = Stroke(width = trackW, cap = StrokeCap.Round)
                    )

                    // Active Glowing Progress Arc
                    if (progressSweep > 1f) {
                        drawArc(
                            color      = gaugeColor,
                            startAngle = startAngle,
                            sweepAngle = progressSweep,
                            useCenter  = false,
                            topLeft    = tl,
                            size       = sz,
                            style      = Stroke(width = trackW, cap = StrokeCap.Round)
                        )
                    }

                    // Dial Ticks
                    for (i in 0..10) {
                        val angle   = startAngle + i * (sweepTotal / 10f)
                        val rad     = Math.toRadians(angle.toDouble())
                        val isMajor = i % 2 == 0
                        val outerR  = arcR - trackW / 2f - 3.dp.toPx()
                        val innerR  = outerR - if (isMajor) 6.dp.toPx() else 3.dp.toPx()
                        drawLine(
                            color       = tickBaseColor.copy(alpha = if (isMajor) 0.6f else 0.3f),
                            start       = Offset(cx + (outerR * cos(rad)).toFloat(), cy + (outerR * sin(rad)).toFloat()),
                            end         = Offset(cx + (innerR * cos(rad)).toFloat(), cy + (innerR * sin(rad)).toFloat()),
                            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                        )
                    }
                }

                // Digital Center Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text          = metricProgress.displayText,
                        color         = contentColor,
                        fontSize      = if (isNa) 36.sp else 40.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = (-1.5).sp,
                        fontFamily    = FontFamily.Monospace
                    )
                    Text(
                        text          = if (metricMode == "SPEED") "GPS $displayUnit" else displayUnit,
                        color         = gaugeColor,
                        fontSize      = 9.5.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        // Futuristic Neon Speed vs Time Graph (Rolling Canvas)
        Box(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color(0xFF243048).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                .padding(6.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPEED VS TIME",
                        color = contentColor.copy(alpha = 0.45f),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "PEAK: %.0f %s".format(peakSpeed, unitLabel),
                        color = accent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 60FPS Speed Waveform Canvas
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val points = speedHistory
                    if (points.size < 2 || w <= 0 || h <= 0) return@Canvas

                    val stepX = w / (points.size - 1)
                    val maxValGraph = maxOf(maxSpeed, 100f)

                    val linePath = Path()
                    val areaPath = Path()

                    points.forEachIndexed { i, spd ->
                        val x = i * stepX
                        val y = h - (spd / maxValGraph).coerceIn(0f, 1f) * (h - 4.dp.toPx()) - 2.dp.toPx()
                        if (i == 0) {
                            linePath.moveTo(x, y)
                            areaPath.moveTo(x, h)
                            areaPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            areaPath.lineTo(x, y)
                        }
                    }

                    areaPath.lineTo(w, h)
                    areaPath.close()

                    // Glowing Area Gradient
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )

                    // Neon Stroke Line
                    drawPath(
                        path = linePath,
                        color = accent,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Bottom: Tactile PRND Gear Selector Bar & Range
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tactile PRND Pills
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color(0xFF243048), RoundedCornerShape(16.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("P", "R", "N", "D").forEach { gear ->
                    val isActive = currentGear == gear
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isActive) accent else Color.Transparent)
                            .clickable {
                                TripFuelRepository.playTactileBeep()
                                currentGear = gear
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = gear,
                            color = if (isActive) Color(0xFF07090E) else contentColor.copy(alpha = 0.45f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Estimated Range Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color(0xFF243048), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val rangeStr = if (fuelPct != null && fuelPct!! > 0f) {
                    val remainingL = (fuelPct!! / 100f) * 37.0f
                    "RANGE ${(remainingL * 21.0f).toInt()} KM"
                } else {
                    "RANGE NA"
                }
                Text(
                    text = rangeStr,
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private data class MetricItem(
    val progressVal: Float,
    val maxVal: Float,
    val displayText: String,
    val displayUnit: String,
    val color: Color
)
