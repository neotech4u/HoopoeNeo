package com.hoopoe.launcher.ui.widget

import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.view.SoundEffectConstants
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoopoe.launcher.util.LocationData
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.File

/**
 * Android Auto (Coolwalk) style Embedded Live Map Widget.
 * Runs an actual interactive OpenStreetMap MapView directly inside the HUD slot.
 * Continuously tracks car GPS coordinates, centers on the vehicle, rotates vehicle marker,
 * and provides in-map zoom/recenter tactile buttons and one-tap launch to full navigation apps.
 *
 * Heavily optimized for low-RAM automotive head units:
 * - Thread pool clamped to 2 download threads + 1 cache thread
 * - Tile memory cache capped to 12 tiles
 * - Center position updates rate-limited to 3m displacement (eliminates ValueAnimator CPU spin)
 * - Vehicle arrow rotated via graphicsLayer draw phase
 */
@Composable
fun NavigationMapWidget(
    locationFlow: StateFlow<LocationData?>,
    bearingFlow: StateFlow<Float>,
    compassOffset: Float = 0f,
    accent: Color,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val location by locationFlow.collectAsStateWithLifecycle()
    val rawBearing by bearingFlow.collectAsStateWithLifecycle()
    val bearing = (rawBearing + compassOffset + 360f) % 360f
    NavigationMapWidget(
        location = location,
        bearing = bearing,
        accent = accent,
        isDayMode = isDayMode,
        modifier = modifier
    )
}

@Composable
fun NavigationMapWidget(
    location: LocationData?,
    bearing: Float,
    accent: Color,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    val hasGps = location != null && location.latitude != 0.0 && location.longitude != 0.0
    val currentLat = if (hasGps) location!!.latitude else 28.6139 // Default center if GPS not yet locked
    val currentLon = if (hasGps) location!!.longitude else 77.2090

    var isFollowing by remember { mutableStateOf(true) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var lastCenteredLat by remember { mutableDoubleStateOf(0.0) }
    var lastCenteredLon by remember { mutableDoubleStateOf(0.0) }
    var lastDayModeState by remember { mutableStateOf<Boolean?>(null) }

    // Initialize osmdroid storage & thread configuration safely for automotive SoCs
    remember {
        try {
            val config = Configuration.getInstance()
            config.osmdroidBasePath = context.noBackupFilesDir
            config.osmdroidTileCache = File(context.cacheDir, "osmdroid")
            config.userAgentValue = context.packageName
            config.cacheMapTileCount = 12.toShort()
            config.tileDownloadThreads = 2.toShort()
            config.tileFileSystemThreads = 1.toShort()
        } catch (_: Exception) {}
        true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048), RoundedCornerShape(16.dp))
    ) {
        // ── 1. Real Embedded OpenStreetMap Native View ────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    setBuiltInZoomControls(false)
                    controller.setZoom(16.5)

                    val startPoint = GeoPoint(currentLat, currentLon)
                    controller.setCenter(startPoint)
                    lastCenteredLat = currentLat
                    lastCenteredLon = currentLon

                    // Automotive OLED Night Mode Filter
                    if (!isDayMode) {
                        val nightMatrix = ColorMatrix(floatArrayOf(
                            -0.78f, 0f, 0f, 0f, 220f,
                            0f, -0.78f, 0f, 0f, 225f,
                            0f, 0f, -0.78f, 0f, 240f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(nightMatrix))
                    }

                    mapViewInstance = this
                }
            },
            update = { map ->
                // Update night/day matrix if day mode toggled
                if (lastDayModeState != isDayMode) {
                    lastDayModeState = isDayMode
                    if (!isDayMode) {
                        val nightMatrix = ColorMatrix(floatArrayOf(
                            -0.78f, 0f, 0f, 0f, 220f,
                            0f, -0.78f, 0f, 0f, 225f,
                            0f, 0f, -0.78f, 0f, 240f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        map.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(nightMatrix))
                    } else {
                        map.overlayManager.tilesOverlay.setColorFilter(null)
                    }
                }

                // Update position smoothly without CPU animator thrashing
                if (hasGps && isFollowing) {
                    val dist = FloatArray(1)
                    android.location.Location.distanceBetween(lastCenteredLat, lastCenteredLon, currentLat, currentLon, dist)
                    if (dist[0] >= 3.0f || (lastCenteredLat == 0.0 && lastCenteredLon == 0.0)) {
                        lastCenteredLat = currentLat
                        lastCenteredLon = currentLon
                        map.controller.setCenter(GeoPoint(currentLat, currentLon))
                    }
                }
            }
        )

        // Manage MapView Lifecycle safely to prevent memory / thread leaks
        DisposableEffect(mapViewInstance) {
            val map = mapViewInstance
            map?.onResume()
            onDispose {
                map?.onPause()
                map?.onDetach()
            }
        }

        // ── 2. Live Vehicle Marker Overlay (Center of Map) ────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient GPS pulse halo
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = accent.copy(alpha = 0.22f),
                    radius = size.minDimension / 2f
                )
                drawCircle(
                    color = accent.copy(alpha = 0.85f),
                    radius = size.minDimension / 4.5f
                )
            }

            // Directional Vehicle Arrow rotated to bearing via GPU layer (zero recompositions)
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Vehicle Heading",
                tint = Color.White,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer { rotationZ = bearing }
            )
        }

        // ── 3. Top Status & Navigation Rail ────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GPS Location / Direction Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F141F).copy(alpha = 0.88f))
                    .border(1.dp, Color(0xFF243048), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (hasGps) Color(0xFF00E676) else Color(0xFFFF9100))
                    )
                    Text(
                        text = if (hasGps) "GPS LOCKED" else "ACQUIRING GPS",
                        color = if (hasGps) Color(0xFF00E676) else Color(0xFFFF9100),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "•",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp
                    )
                    Text(
                        text = getHeadingDescription(bearing),
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Speed Limit / Speed indicator badge
            val speedKmh = ((location?.speedMps ?: 0f) * 3.6f).toInt()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F141F).copy(alpha = 0.88f))
                    .border(1.dp, Color(0xFF243048), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$speedKmh",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "KM/H",
                        color = Color(0xFF64748B),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── 3b. Google Maps Quick-Launch Shortcut Pill ──────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F141F).copy(alpha = 0.90f))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .clickable {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    launchFullNavApp(context, currentLat, currentLon)
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Open Maps",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "GOOGLE MAPS ↗",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // ── 4. Floating Tactical Automotive Controls (Right Side) ──────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Zoom In (+)
            MapControlButton(
                icon = Icons.Default.Add,
                contentDescription = "Zoom In",
                accent = accent,
                onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    mapViewInstance?.controller?.zoomIn()
                }
            )

            // Zoom Out (-)
            MapControlButton(
                icon = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                accent = accent,
                onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    mapViewInstance?.controller?.zoomOut()
                }
            )

            // Recenter on Vehicle (◎)
            MapControlButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "Center on Vehicle",
                accent = if (isFollowing) accent else Color(0xFF64748B),
                onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    isFollowing = true
                    if (hasGps) {
                        lastCenteredLat = currentLat
                        lastCenteredLon = currentLon
                        mapViewInstance?.controller?.setCenter(GeoPoint(currentLat, currentLon))
                    }
                }
            )

            // Open Full Navigation App (↗)
            MapControlButton(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open Full Navigation",
                accent = Color(0xFF00E5FF),
                onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    launchFullNavApp(context, currentLat, currentLon)
                }
            )
        }
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F141F).copy(alpha = 0.90f))
            .border(1.dp, Color(0xFF243048), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun getHeadingDescription(bearing: Float): String {
    val b = (bearing % 360 + 360) % 360
    return when {
        b >= 337.5 || b < 22.5 -> "NORTH"
        b < 67.5  -> "NE"
        b < 112.5 -> "EAST"
        b < 157.5 -> "SE"
        b < 202.5 -> "SOUTH"
        b < 247.5 -> "SW"
        b < 292.5 -> "WEST"
        else      -> "NW"
    }
}

private fun launchFullNavApp(context: Context, lat: Double, lon: Double) {
    try {
        // 1. Try launching Google Maps app directly with driving mode
        val gmapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lon&mode=d")).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (gmapsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(gmapsIntent)
            return
        }
    } catch (_: Exception) {}

    try {
        // 2. Try generic geo intent (Google Maps, OsmAnd, Organic Maps, Waze)
        val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Vehicle)")
        val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lon")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapsIntent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lon")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { context.startActivity(fallback) }
        }
    }
}
