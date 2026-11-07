package com.hoopoe.launcher.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.model.AppInfo
import com.hoopoe.launcher.model.NowPlayingState
import com.hoopoe.launcher.viewmodel.LauncherViewModel

@Composable
fun MediaScreen(
    nowPlaying: NowPlayingState?,
    hardwareRadio: LauncherViewModel.HardwareRadioState?,
    installedApps: List<AppInfo>,
    accent: Color,
    isDayMode: Boolean = false,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onRadioSeekUp: () -> Unit,
    onRadioSeekDown: () -> Unit,
    onRadioCycleFm: () -> Unit,
    onRadioSwitchAm: () -> Unit,
    onLaunchMediaApp: (String) -> Unit,
    onBackToCockpit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF0F141F).copy(alpha = 0.85f)

    var activeSourceTab by remember { mutableStateOf(if (hardwareRadio != null) "RADIO" else "NOW_PLAYING") }

    val isPlaying = nowPlaying?.isPlaying == true

    Row(modifier = modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {

        // Left 50%: Big Album Art & Ambient Visualizer
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (activeSourceTab == "NOW_PLAYING" && nowPlaying?.albumArt != null) {
                // Blurred Ambient Backdrop
                Image(
                    bitmap = nowPlaying.albumArt.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(24.dp)
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

                // Centered Sharp Artwork
                Image(
                    bitmap = nowPlaying.albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                )
            } else if (activeSourceTab == "RADIO") {
                // Radio Tuner Art / Dial Graphic
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.15f))
                            .border(2.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Radio, "Radio", tint = accent, modifier = Modifier.size(54.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = hardwareRadio?.band ?: "FM1",
                        color = accent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = (hardwareRadio?.freq ?: "91.1") + " MHz",
                        color = contentColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = hardwareRadio?.stationName ?: "STEREO LIVE TUNER",
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // Standby Vinyl Plate
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.1f))
                            .border(1.5.dp, accent.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = accent, modifier = Modifier.size(56.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("NO MEDIA ACTIVE", color = contentColor.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Right 50%: Player Controls & Source Switcher
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation & Tabs
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
                    Text("MEDIA CENTRE", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                // Tabs: STREAMING | RADIO
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    listOf("NOW_PLAYING" to "PLAYERS", "RADIO" to "FM RADIO").forEach { (tab, label) ->
                        val isSelected = activeSourceTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) accent else Color.Transparent)
                                .clickable { activeSourceTab = tab }
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

            // Track Information
            Column(modifier = Modifier.fillMaxWidth()) {
                val title = if (activeSourceTab == "RADIO") {
                    hardwareRadio?.display ?: "FM Radio Tuner"
                } else {
                    nowPlaying?.title?.ifEmpty { "Ready to Play" } ?: "Select Media Source"
                }
                val artist = if (activeSourceTab == "RADIO") {
                    hardwareRadio?.stationName ?: "Broadcast Receiver"
                } else {
                    nowPlaying?.artist?.ifEmpty { "Bluetooth / Streaming" } ?: "Tap app shortcut to launch"
                }

                Text(
                    text = title,
                    color = contentColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tactile 64dp+ Master In-Car Transport Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev
                IconButton(
                    onClick = { if (activeSourceTab == "RADIO") onRadioSeekDown() else onPrev() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = contentColor, modifier = Modifier.size(34.dp))
                }

                Spacer(Modifier.width(18.dp))

                // Play / Pause Master Button (72dp touch target)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF07090E),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.width(18.dp))

                // Next
                IconButton(
                    onClick = { if (activeSourceTab == "RADIO") onRadioSeekUp() else onNext() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.SkipNext, "Next", tint = contentColor, modifier = Modifier.size(34.dp))
                }
            }

            // Radio Bands / Extra Shortcuts
            if (activeSourceTab == "RADIO") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("FM1", "FM2", "FM3", "AM").forEach { band ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (band == "AM") onRadioSwitchAm() else onRadioCycleFm()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(band, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Media App Switcher Row
                val mediaApps = installedApps.filter {
                    val p = it.packageName.lowercase()
                    p.contains("spotify") || p.contains("music") || p.contains("youtube") || p.contains("vlc") || p.contains("audio")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mediaApps.take(4).forEach { app ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .clickable { onLaunchMediaApp(app.packageName) }
                            .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(app.appName.take(10), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
