package com.hoopoe.launcher.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.data.AppSettings
import com.hoopoe.launcher.model.AppInfo
import com.hoopoe.launcher.data.NavigationManager
import com.hoopoe.launcher.ui.widget.NavigationMapWidget
import com.hoopoe.launcher.util.LocationData
import kotlinx.coroutines.flow.StateFlow

private data class NavPoiShortcut(
    val label: String,
    val query: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun NavigationScreen(
    settings: AppSettings,
    locationFlow: StateFlow<LocationData?>,
    bearingFlow: StateFlow<Float>,
    installedApps: List<AppInfo>,
    accent: Color,
    isDayMode: Boolean = false,
    onBackToCockpit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navManager = remember { NavigationManager.getInstance(context) }
    val location by locationFlow.collectAsState()
    val curLat = location?.latitude ?: 28.6139
    val curLon = location?.longitude ?: 77.2090

    var searchQuery by remember { mutableStateOf("") }

    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF0F141F).copy(alpha = 0.85f)

    val poiShortcuts = listOf(
        NavPoiShortcut("Petrol Pump", "PETROL", Icons.Default.LocalGasStation, Color(0xFFFF9100)),
        NavPoiShortcut("Mosque / Masjid", "MOSQUE", Icons.Default.Mosque, Color(0xFF00E676)),
        NavPoiShortcut("Hospital / Clinic", "HOSPITAL", Icons.Default.LocalHospital, Color(0xFFFF1744)),
        NavPoiShortcut("Restaurant / Food", "RESTAURANT", Icons.Default.Restaurant, Color(0xFFFFB300)),
        NavPoiShortcut("Home", settings.homeAddress.ifEmpty { "HOME" }, Icons.Default.Home, accent),
        NavPoiShortcut("Work", settings.workAddress.ifEmpty { "WORK" }, Icons.Default.Work, Color(0xFF90CAF9))
    )

    fun routeToTarget(query: String) {
        val target = navManager.selectPoiDestination(query, curLat, curLon)
        navManager.setDestination(target, curLat, curLon)
    }

    fun launchNavIntent(query: String) {
        val uri = if (query.startsWith("geo:")) {
            Uri.parse(query)
        } else {
            Uri.parse("geo:0,0?q=" + Uri.encode(query))
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun launchAppPackage(pkg: String) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(pkg)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        intent?.let { context.startActivity(it) }
    }

    Row(modifier = modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

        // Left 55%: Interactive Osmdroid Map Canvas
        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
        ) {
            NavigationMapWidget(
                locationFlow = locationFlow,
                bearingFlow = bearingFlow,
                compassOffset = settings.compassOffset,
                accent = accent,
                isDayMode = isDayMode,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Right 45%: Destination Shortcuts & App Selector
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    Text(
                        text = "NAVIGATION CENTRE",
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // 1-Tap Google Maps button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.15f))
                        .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { launchNavIntent("geo:0,0") }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Navigation, null, tint = accent, modifier = Modifier.size(14.dp))
                        Text("FULL MAP", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search city, address or POI...", color = contentColor.copy(alpha = 0.4f), fontSize = 12.sp) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { routeToTarget(searchQuery) }) {
                            Icon(Icons.Default.Directions, "Navigate", tint = accent)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor
                )
            )

            // Category POI Grid (Petrol, Mosque, Hospital, Restaurant, Home, Work)
            Text(
                text = "ONE-TOUCH DESTINATIONS",
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(poiShortcuts) { poi ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable { routeToTarget(poi.query) }
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(poi.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(poi.icon, null, tint = poi.color, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = poi.label,
                                color = contentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Navigation Apps Installed Row
            val navApps = installedApps.filter {
                val p = it.packageName.lowercase()
                p.contains("map") || p.contains("waze") || p.contains("here") || p.contains("sygic") || p.contains("navi")
            }

            if (navApps.isNotEmpty()) {
                Text(
                    text = "INSTALLED NAVIGATION APPS",
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    navApps.take(4).forEach { app ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                                .clickable { launchAppPackage(app.packageName) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = app.appName.take(12),
                                color = accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
