package com.hoopoe.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.view.SoundEffectConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Low-Resource Holographic Assistant Ring:
 * Monitors app notification events:
 * 1. Overspeed Detection (Red pulse)
 * 2. Prayer Reminders (Emerald pulse)
 * 3. Weather Alerts (Amber pulse)
 *
 * Lightweight infinite transition, low CPU overhead on 2GB RAM SoCs.
 */
@Composable
fun JarvisOrb(
    accent: Color,
    modifier: Modifier = Modifier,
    orbSize: Dp = 34.dp,
    isOverspeed: Boolean = false,
    isPrayerAlert: Boolean = false,
    isWeatherAlert: Boolean = false,
    alertText: String? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Alert-driven dynamic color
    val activeRingColor = when {
        isOverspeed -> Color(0xFFFF1744) // Urgent Crimson
        isPrayerAlert -> Color(0xFF00E676) // Sacred Emerald
        isWeatherAlert -> Color(0xFFFF9100) // Caution Amber
        else -> accent
    }

    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(isOverspeed, isPrayerAlert, isWeatherAlert) {
        if (isOverspeed || isPrayerAlert || isWeatherAlert) {
            showTooltip = true
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_anim")

    // Outer ring rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isOverspeed) 2000 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Core pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = if (isOverspeed) 1.25f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isOverspeed) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isOverspeed) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(orbSize)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    if (onClick != null) {
                        onClick()
                    } else {
                        launchGoogleAssistant(context)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (minOf(size.width, size.height) / 2f) - 2.dp.toPx()

                // Ambient Radial Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(activeRingColor.copy(alpha = glowAlpha * 0.45f), Color.Transparent),
                        center = center,
                        radius = radius * 1.25f
                    ),
                    radius = radius * 1.15f,
                    center = center
                )

                // Segmented Arc Reactor Ring
                val arcLength = 65f
                val strokeW = 1.75.dp.toPx()
                for (i in 0..2) {
                    val startAngle = rotation + (i * 120f)
                    drawArc(
                        color = activeRingColor.copy(alpha = 0.85f),
                        startAngle = startAngle,
                        sweepAngle = arcLength,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // Inner Core
                val innerR = radius * 0.52f * pulseScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.95f), activeRingColor.copy(alpha = 0.6f), Color.Transparent),
                        center = center,
                        radius = innerR * 1.4f
                    ),
                    radius = innerR,
                    center = center
                )

                drawCircle(
                    color = activeRingColor.copy(alpha = glowAlpha),
                    radius = innerR,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            Icon(
                imageVector = if (isOverspeed || isPrayerAlert || isWeatherAlert) Icons.Default.NotificationsActive else Icons.Default.Mic,
                contentDescription = "Assistant Ring",
                tint = Color(0xFF07090E),
                modifier = Modifier.size(orbSize * 0.38f)
            )
        }

        // Active Alert Notification Pill
        AnimatedVisibility(
            visible = (isOverspeed || isPrayerAlert || isWeatherAlert) && !alertText.isNullOrEmpty(),
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeRingColor.copy(alpha = 0.2f))
                    .border(1.dp, activeRingColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .clickable { showTooltip = !showTooltip }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(activeRingColor)
                    )
                    Text(
                        text = alertText ?: "",
                        color = activeRingColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

fun launchGoogleAssistant(context: Context) {
    try {
        val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(voiceIntent)
    } catch (_: Exception) {
        try {
            val assistIntent = Intent("android.intent.action.ASSIST").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(assistIntent)
        } catch (_: Exception) {
            try {
                val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(searchIntent)
            } catch (_: Exception) {}
        }
    }
}
