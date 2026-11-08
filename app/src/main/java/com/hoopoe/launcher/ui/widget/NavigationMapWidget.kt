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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoopoe.launcher.data.NavigationManager
import com.hoopoe.launcher.util.LocationData
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.util.Locale

/**
 * Android Auto (Coolwalk) style Embedded Live Map Widget.
 * Runs an actual interactive OpenStreetMap MapView directly inside the HUD slot.
 * Continuously tracks car GPS coordinates, centers on the vehicle, rotates vehicle marker,
 * renders active in-widget routes & destination pins, and provides quick POI routing.
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
    val navManager = remember { NavigationManager.getInstance(context) }

    val activeDestination by navManager.activeDestination.collectAsStateWithLifecycle()
    val routePoints by navManager.routePoints.collectAsStateWithLifecycle()
    val distRemainingKm by navManager.distanceRemainingKm.collectAsStateWithLifecycle()
    val etaMinutes by navManager.etaMinutes.collectAsStateWithLifecycle()

    val hasGps = location != null && location.latitude != 0.0 && location.longitude != 0.0
    val currentLat = if (hasGps) location!!.latitude else 28.6139 // Default center if GPS not locked
    val currentLon = if (hasGps) location!!.longitude else 77.2090

    var isFollowing by remember { mutableStateOf(true) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var lastCenteredLat by remember { mutableDoubleStateOf(0.0) }
    var lastCenteredLon by remember { mutableDoubleStateOf(0.0) }
    var lastDayModeState by remember { mutableStateOf<Boolean?>(null) }
    var routePolylineRef by remember { mutableStateOf<Polyline?>(null) }
    var destMarkerRef by remember { mutableStateOf<Marker?>(null) }

    // Initialize osmdroid storage & standard User-Agent so OSM servers never block tiles
    remember {
        try {
            val config = Configuration.getInstance()
            config.osmdroidBasePath = context.noBackupFilesDir
            config.osmdroidTileCache = File(context.cacheDir, "osmdroid")
            config.userAgentValue = "HoopoeNeo/1.0.1 (${context.packageName}; automotive infotainment)"
            config.cacheMapTileCount = 16.toShort()
            config.tileDownloadThreads = 2.toShort()
            config.tileFileSystemThreads = 1.toShort()
        } catch (_: Exception) {}
        true
    }

    // Keep route updated when car coordinates advance
    LaunchedEffect(currentLat, currentLon) {
        if (hasGps && activeDestination != null) {
            navManager.onLocationUpdate(currentLat, currentLon)
        }
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

                    // Night filter: subtle dimming preserving real road and geography colors
                    if (!isDayMode) {
                        val nightFilter = ColorMatrix(floatArrayOf(
                            0.84f, 0f, 0f, 0f, -8f,
                            0f, 0.84f, 0f, 0f, -8f,
                            0f, 0f, 0.88f, 0f, -4f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(nightFilter))
                    }

                    // Create routing overlays
                    val poly = Polyline(this).apply {
                        outlinePaint.color = android.graphics.Color.parseColor("#00E5FF")
                        outlinePaint.strokeWidth = 14f
                    }
                    val marker = Marker(this).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    routePolylineRef = poly
                    destMarkerRef = marker

                    mapViewInstance = this
                }
            },
            update = { map ->
                // Update day/night tile filter
                if (lastDayModeState != isDayMode) {
                    lastDayModeState = isDayMode
                    if (!isDayMode) {
                        val nightFilter = ColorMatrix(floatArrayOf(
                            0.84f, 0f, 0f, 0f, -8f,
                            0f, 0.84f, 0f, 0f, -8f,
                            0f, 0f, 0.88f, 0f, -4f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        map.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(nightFilter))
                    } else {
                        map.overlayManager.tilesOverlay.setColorFilter(null)
                    }
                }

                // Smooth position following
                if (hasGps && isFollowing) {
                    val dist = FloatArray(1)
                    android.location.Location.distanceBetween(lastCenteredLat, lastCenteredLon, currentLat, currentLon, dist)
                    if (dist[0] >= 3.0f || (lastCenteredLat == 0.0 && lastCenteredLon == 0.0)) {
                        lastCenteredLat = currentLat
                        lastCenteredLon = currentLon
                        map.controller.setCenter(GeoPoint(currentLat, currentLon))
                    }
                }

                // Render in-widget route polyline and destination marker
                val poly = routePolylineRef
                val marker = destMarkerRef
                if (poly != null && marker != null) {
                    if (routePoints.isNotEmpty() && activeDestination != null) {
                        poly.setPoints(routePoints)
                        if (!map.overlays.contains(poly)) map.overlays.add(poly)

                        marker.position = GeoPoint(activeDestination!!.lat, activeDestination!!.lon)
                        marker.title = activeDestination!!.name
                        marker.snippet = activeDestination!!.address
                        if (!map.overlays.contains(marker)) map.overlays.add(marker)

                        map.invalidate()
                    } else {
                        if (map.overlays.contains(poly)) {
                            map.overlays.remove(poly)
                            map.overlays.remove(marker)
                            map.invalidate()
                        }
                    }
                }
            }
        )

        // Lifecycle management
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
                .size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = accent.copy(alpha = 0.22f), radius = size.minDimension / 2f)
                drawCircle(color = accent.copy(alpha = 0.85f), radius = size.minDimension / 4.5f)
            }
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Vehicle Heading",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = bearing }
            )
        }

        // ── 3. Active Destination HUD or Idle Status Rail ──────────────────────
        if (activeDestination != null) {
            // In-Widget Active Navigation Guidance Banner
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0A0F1A).copy(alpha = 0.94f))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (activeDestination!!.category) {
                                    "PETROL" -> Icons.Default.LocalGasStation
                                    "MOSQUE" -> Icons.Default.Mosque
                                    "HOSPITAL" -> Icons.Default.LocalHospital
                                    "RESTAURANT" -> Icons.Default.Restaurant
                                    "HOME" -> Icons.Default.Home
                                    "WORK" -> Icons.Default.Work
                                    else -> Icons.Default.LocationOn
                                },
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeDestination!!.name,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f KM", distRemainingKm),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("•", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text(
                                    text = "$etaMinutes MIN ETA",
                                    color = Color(0xFF00E676),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Open in Google Maps (Optional external turn-by-turn)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .clickable {
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    launchFullNavApp(context, activeDestination!!.lat, activeDestination!!.lon)
                                }
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("MAPS", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                            }
                        }

                        // Clear Route Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFF1744).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFFF1744).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable {
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    navManager.clearDestination()
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text("CLEAR", color = Color(0xFFFF5252), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Idle Status Rail (GPS Status & Heading)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F141F).copy(alpha = 0.90f))
                        .border(1.dp, Color(0xFF243048), RoundedCornerShape(12.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (hasGps) Color(0xFF00E676) else Color(0xFFFF9100))
                        )
                        Text(
                            text = if (hasGps) "LIVE OSM MAP" else "ACQUIRING GPS",
                            color = if (hasGps) Color(0xFF00E676) else Color(0xFFFF9100),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("•", color = Color(0xFF64748B), fontSize = 9.sp)
                        Text(
                            text = getHeadingDescription(bearing),
                            color = accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Google Maps Quick Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F141F).copy(alpha = 0.90f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            launchFullNavApp(context, currentLat, currentLon)
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Navigation, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(11.dp))
                        Text("GOOGLE MAPS ↗", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── 4. Bottom Quick 1-Tap In-Widget POI Route Selector ──────────────
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0F1A).copy(alpha = 0.90f))
                    .border(1.dp, Color(0xFF243048), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple("PETROL", "⛽ PETROL", Icons.Default.LocalGasStation),
                    Triple("MOSQUE", "🕌 MOSQUE", Icons.Default.Mosque),
                    Triple("HOSPITAL", "🏥 HOSPITAL", Icons.Default.LocalHospital)
                ).forEach { (cat, label, _) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF162032))
                            .clickable {
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                val target = navManager.selectPoiDestination(cat, currentLat, currentLon)
                                navManager.setDestination(target, currentLat, currentLon)
                                isFollowing = false
                                mapViewInstance?.let { map ->
                                    val bbox = BoundingBox(
                                        maxOf(currentLat, target.lat) + 0.005,
                                        maxOf(currentLon, target.lon) + 0.005,
                                        minOf(currentLat, target.lat) - 0.005,
                                        minOf(currentLon, target.lon) - 0.005
                                    )
                                    map.zoomToBoundingBox(bbox, true, 80)
                                }
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(label, color = Color(0xFF00E5FF), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── 5. Floating Tactical Automotive Controls (Right Side) ──────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp),
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
            .size(36.dp)
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
            modifier = Modifier.size(18.dp)
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
        val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Destination)")
        val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lon")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}
