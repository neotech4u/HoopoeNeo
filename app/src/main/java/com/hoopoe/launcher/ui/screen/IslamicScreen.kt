package com.hoopoe.launcher.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.data.AsrCalcMethod
import com.hoopoe.launcher.data.PrayerCalcMethod
import com.hoopoe.launcher.util.LocationData
import com.hoopoe.launcher.util.PrayerTimeCalculator
import com.hoopoe.launcher.util.PrayerTimes
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IslamicScreen(
    location: LocationData?,
    bearing: Float,
    method: PrayerCalcMethod,
    asrMethod: AsrCalcMethod,
    accent: Color,
    isDayMode: Boolean = false,
    onBackToCockpit: () -> Unit,
    onOpenDuaModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("PRAYER") } // "PRAYER" | "QIBLA" | "DUA"

    val lat = location?.latitude ?: 28.6139
    val lon = location?.longitude ?: 77.2090

    val prayerTimes = remember(lat, lon, method, asrMethod) {
        PrayerTimeCalculator.calculateTimes(lat, lon, method = method, asrMethod = asrMethod)
    }

    val qiblaBearing = remember(lat, lon) {
        PrayerTimeCalculator.calculateQiblaBearing(lat, lon)
    }

    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF0F141F).copy(alpha = 0.85f)

    fun launchMosqueSearch() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=mosque")).apply {
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
        // Top Header & Tab Switcher
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
                Text("ISLAMIC TRAVEL COMPANION", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            // Tabs: PRAYER | QIBLA | TRAVEL DUA
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(
                    "PRAYER" to "PRAYER TIMES",
                    "QIBLA" to "QIBLA COMPASS",
                    "DUA" to "TRAVEL DUA"
                ).forEach { (key, label) ->
                    val isSelected = activeTab == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) accent else Color.Transparent)
                            .clickable { activeTab = key }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF07090E) else contentColor.copy(alpha = 0.6f),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Tab Body
        when (activeTab) {
            "PRAYER" -> {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Left: Big Next Prayer Card + Mosque Shortcut
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.15f))
                                        .border(1.5.dp, accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Mosque, null, tint = accent, modifier = Modifier.size(34.dp))
                                }

                                Text("NEXT PRAYER", color = contentColor.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(prayerTimes.nextPrayerName.uppercase(), color = accent, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                                Text(prayerTimes.nextPrayerTime, color = contentColor, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("Time Remaining: ${prayerTimes.minutesToNextPrayer} Minutes", color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Find nearby mosque button
                        Button(
                            onClick = { launchMosqueSearch() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Explore, null, tint = Color(0xFF07090E), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("NAVIGATE TO NEARBY MOSQUE", color = Color(0xFF07090E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Right: Schedule List of 5 daily prayers
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf(
                            "Fajr (Dawn)" to prayerTimes.fajr,
                            "Sunrise" to prayerTimes.sunrise,
                            "Dhuhr (Noon)" to prayerTimes.dhuhr,
                            "Asr (Afternoon)" to prayerTimes.asr,
                            "Maghrib (Sunset)" to prayerTimes.maghrib,
                            "Isha (Night)" to prayerTimes.isha
                        ).forEach { (name, time) ->
                            val isNext = name.startsWith(prayerTimes.nextPrayerName, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isNext) accent.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, color = if (isNext) accent else contentColor, fontSize = 13.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold)
                                Text(time, color = if (isNext) accent else contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            HorizontalDivider(color = cardBorder.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            "QIBLA" -> {
                // Qibla Compass Dial
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Qibla Compass Canvas
                        Canvas(modifier = Modifier.size(240.dp)) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val r = size.width / 2f - 16.dp.toPx()

                            // Outer dial ring
                            drawCircle(color = cardBorder, radius = r, center = center, style = Stroke(width = 2.dp.toPx()))

                            // Cardinal marks
                            drawCircle(color = accent.copy(alpha = 0.1f), radius = r * 0.85f, center = center)

                            // Target Qibla needle (Gold/Emerald)
                            val relativeQiblaAngle = Math.toRadians((qiblaBearing - bearing).toDouble())
                            val qiblaX = center.x + (r * 0.75f * sin(relativeQiblaAngle)).toFloat()
                            val qiblaY = center.y - (r * 0.75f * cos(relativeQiblaAngle)).toFloat()

                            drawLine(
                                color = Color(0xFF00E676),
                                start = center,
                                end = Offset(qiblaX, qiblaY),
                                strokeWidth = 4.dp.toPx()
                            )
                            drawCircle(color = Color(0xFF00E676), radius = 6.dp.toPx(), center = Offset(qiblaX, qiblaY))

                            // Center pivot
                            drawCircle(color = accent, radius = 5.dp.toPx(), center = center)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("QIBLA NAVIGATION BEARING", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("${qiblaBearing.toInt()}° FROM NORTH", color = accent, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text("The green indicator points towards the Holy Kaaba in Makkah.", color = contentColor.copy(alpha = 0.7f), fontSize = 11.sp)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("SENSOR CALIBRATION", color = Color(0xFFFF9100), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("If the needle fluctuates inside the car, move the vehicle forward or perform a gentle figure-8 to recalibrate internal magnetometer.", color = contentColor.copy(alpha = 0.6f), fontSize = 9.sp)
                        }
                    }
                }
            }

            "DUA" -> {
                // Travel Dua with Quick Launch
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنْقَلِبُونَ",
                            color = Color(0xFFFF9100),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Subhanallazi sakh-khara lana haza wa ma kunna lahu muqrinin, wa inna ila Rabbina lamun-qaliboon.",
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "\"Glory to Him who has subjected this to us, and we could never have it by our efforts. And verily, unto our Lord we indeed are to return.\"",
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = onOpenDuaModal,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.6f).height(44.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, null, tint = Color(0xFF07090E), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("OPEN FULL TRAVEL DUA MODAL", color = Color(0xFF07090E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
