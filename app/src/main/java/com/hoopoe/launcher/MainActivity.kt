package com.hoopoe.launcher

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hoopoe.launcher.data.DayNightMode
import com.hoopoe.launcher.data.SidebarPosition
import com.hoopoe.launcher.data.GradientDirection
import com.hoopoe.launcher.model.NavDestination
import com.hoopoe.launcher.ui.components.Sidebar
import com.hoopoe.launcher.ui.screen.*
import com.hoopoe.launcher.ui.theme.OpenLauncherTheme
import com.hoopoe.launcher.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    companion object {
        var hasPlayedLogonSession = false
        var hasShownTravelDuaSession = false
    }

    private val vm: LauncherViewModel by viewModels()

    private val locationPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            vm.startLocationUpdates()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        setContent {
            val settingsLoaded by vm.settingsLoaded.collectAsStateWithLifecycle()
            val settings       by vm.settings.collectAsStateWithLifecycle()
            val nav            by vm.nav.collectAsStateWithLifecycle()
            val apps        by vm.apps.collectAsStateWithLifecycle()
            val appsLoading by vm.appsLoading.collectAsStateWithLifecycle()
            val nowPlaying  by vm.nowPlaying.collectAsStateWithLifecycle()
            val weather     by vm.weather.collectAsStateWithLifecycle()
            val isWifi      by vm.isWifi.collectAsStateWithLifecycle()
            val isData      by vm.isData.collectAsStateWithLifecycle()
            val isDayModeVM by vm.isDayMode.collectAsStateWithLifecycle()
            val hardwareRadio by vm.hardwareRadio.collectAsStateWithLifecycle()
            val systemIsDark = isSystemInDarkTheme()
            val isDayMode = if (settings.dayNightMode == DayNightMode.SYSTEM) !systemIsDark else isDayModeVM
            val pickerSlot      by vm.shortcutPickerSlot.collectAsStateWithLifecycle()
            val appPickerTarget by vm.appPickerTarget.collectAsStateWithLifecycle()
            val isPreviewingLogon by vm.previewingLogon.collectAsStateWithLifecycle()
            val isPreviewingTravelDua by vm.previewingTravelDua.collectAsStateWithLifecycle()
            var showLogon by remember {
                mutableStateOf(!hasPlayedLogonSession && settings.playLogonAnimation)
            }
            var showTravelDua by remember {
                mutableStateOf(!hasShownTravelDuaSession && settings.showTravelDua)
            }
            var showRefuelDialog by remember { mutableStateOf(false) }

            val accent         = Color(settings.accentColor)
            val bg             = if (settings.useCustomBackgroundColor) {
                Color(settings.backgroundColor)
            } else {
                if (isDayMode) Color(0xFFEEEEEE) else Color.Black
            }
            val textColor      = if (isDayMode) Color(0xFF111111) else Color(settings.fontColor)
            val bgGradientEnd  = Color(settings.gradientEndColor)
            val bgBrush        = if (settings.useCustomBackgroundColor && settings.useGradient) {
                val colors = listOf(bg, bgGradientEnd)
                when (settings.gradientDirection) {
                    GradientDirection.TOP_TO_BOTTOM -> androidx.compose.ui.graphics.Brush.verticalGradient(colors)
                    GradientDirection.LEFT_TO_RIGHT -> androidx.compose.ui.graphics.Brush.horizontalGradient(colors)
                    GradientDirection.DIAGONAL -> androidx.compose.ui.graphics.Brush.linearGradient(colors)
                    GradientDirection.RADIAL -> androidx.compose.ui.graphics.Brush.radialGradient(colors)
                }
            } else null

            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density   = baseDensity.density * settings.uiScale,
                    fontScale = baseDensity.fontScale
                )
            ) {
                if (showLogon || isPreviewingLogon) {
                    LogonScreen(
                        onFinished = {
                            hasPlayedLogonSession = true
                            showLogon = false
                            vm.dismissLogonPreview()
                        }
                    )
                } else if (showTravelDua || isPreviewingTravelDua) {
                    TravelDuaScreen(
                        accent = accent,
                        onDismiss = {
                            hasShownTravelDuaSession = true
                            showTravelDua = false
                            vm.dismissTravelDua()
                        }
                    )
                } else if (!settingsLoaded) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                } else OpenLauncherTheme(
                    accent     = accent,
                    background = bg,
                    textColor  = textColor,
                    fontBold   = settings.fontBold,
                    textScale  = settings.textScale,
                    appFont    = settings.appFont,
                    isDayMode  = isDayMode,
                    useCustomBg = settings.useCustomBackgroundColor
                ) {
                if (!settings.onboardingCompleted) {
                    OnboardingScreen(
                        accent = accent,
                        onComplete = {
                            vm.updateSettings { copy(onboardingCompleted = true) }
                            // Start location updates immediately upon completion
                            vm.startLocationUpdates()
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().let { m ->
                        if (bgBrush != null) m.background(bgBrush) else m.background(bg)
                    }) {
                        // Optional wallpaper layer
                        if (settings.wallpaperUri.isNotEmpty()) {
                            AsyncImage(
                                model              = android.net.Uri.parse(settings.wallpaperUri),
                                contentDescription = null,
                                contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                            Box(modifier = Modifier.fillMaxSize()
                                .background(Color.Black.copy(alpha = settings.wallpaperDim)))
                        }

                        val isBottomBar    = settings.sidebarPosition == SidebarPosition.BOTTOM
                        val layoutDivColor = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1A1A1A)

                        val sidebarContent: @Composable () -> Unit = {
                            val sidebarDensity = Density(
                                density = baseDensity.density * (1.0f + (settings.uiScale - 1.0f) * 0.35f),
                                fontScale = baseDensity.fontScale
                            )
                            CompositionLocalProvider(LocalDensity provides sidebarDensity) {
                                Sidebar(
                                    currentDest   = nav,
                                    settings      = settings,
                                    isHorizontal  = isBottomBar,
                                    installedIconFor = { pkg ->
                                        apps.find { it.packageName == pkg }?.icon
                                    },
                                    onNavigate    = { dest ->
                                        vm.cancelShortcutPicker()
                                        vm.cancelCarPlayPicker()
                                        vm.exitRearrangeMode()
                                        vm.navigate(dest)
                                    },
                                    onShortcutClick = { slot ->
                                        vm.launchShortcut(slot)
                                    },
                                    onShortcutLongPress  = { slot -> vm.startShortcutPicker(slot) },
                                    onShortcutRemove     = { slot -> vm.removeShortcut(slot) },
                                    onShortcutSetIcon    = { slot, icon -> vm.setShortcutIcon(slot, icon) },
                                    onReorder            = { from, to -> vm.reorderShortcut(from, to) },
                                    onOpenRefuel         = { showRefuelDialog = true }
                                )
                            }
                        }

                        val mainPane: @Composable (Modifier) -> Unit = { paneModifier ->
                            // ── Main content pane ─────────────────────────────
                            AnimatedContent(
                                targetState   = nav,
                                transitionSpec = {
                                    fadeIn() + slideInHorizontally { it / 10 } togetherWith
                                    fadeOut() + slideOutHorizontally { -it / 10 }
                                },
                                modifier = paneModifier,
                                label    = "pane_transition"
                            ) { destination ->
                                when (destination) {
                                    NavDestination.HOME -> HomeScreen(
                                        settings            = settings,
                                        weather             = weather,
                                        nowPlaying          = nowPlaying,
                                        locationFlow        = vm.location,
                                        bearingFlow         = vm.compassBearing,
                                        isWifi              = isWifi,
                                        isData              = isData,
                                        isDayMode           = isDayMode,
                                        onPlayPause         = vm::playPause,
                                        onNext              = vm::skipNext,
                                        onPrev              = vm::skipPrev,
                                        onLaunchCarPlay     = { vm.launchApp(settings.carPlayPackage) },
                                        onLaunchAndroidAuto = { vm.launchApp(settings.androidAutoPackage) },
                                        onAssignCarPlay     = { vm.startCarPlayPicker() },
                                        onAssignAndroidAuto = { vm.startAndroidAutoPicker() },
                                        onClearCarPlay      = { vm.clearCarPlayApp() },
                                        onClearAndroidAuto  = { vm.clearAndroidAutoApp() },
                                        onAssignPip         = { vm.startPipPicker() },
                                        onClearPip          = { vm.clearPipApp() },
                                        onLaunchPip         = { vm.launchApp(settings.pipAppPackage) },
                                        onTapNowPlaying     = {
                                            val pkg = nowPlaying?.controller?.packageName
                                            if (!pkg.isNullOrEmpty()) vm.launchApp(pkg) else vm.launchMusicApp()
                                        },
                                        onUpdateWidget      = { id, sx, sy -> vm.updateWidgetConfig(id, sx, sy) },
                                        onMoveWidget        = { id, gx, gy -> vm.moveWidgetConfig(id, gx, gy) },
                                        onAddWidget         = { id -> vm.addWidget(id) },
                                        onRemoveWidget      = { id -> vm.removeWidget(id) },
                                        onSetClockStyle     = { style -> vm.updateSettings { copy(clockStyle = style) } },
                                        onSetVitalsAsBars   = { asBars -> vm.updateSettings { copy(vitalsAsBars = asBars) } },
                                        onSetSpeedometerDigitalOnly = { digital -> vm.updateSettings { copy(speedometerDigitalOnly = digital) } },
                                        onUpdateSoundPad    = { idx, pad -> vm.updateSoundboardPad(idx, pad) },
                                        hardwareRadio         = hardwareRadio,
                                        onLaunchHardwareRadio = { vm.launchHardwareRadioApp() },
                                        onStopHardwareRadio   = { vm.stopHardwareRadioApp() },
                                        onRadioSeekUp         = { vm.radioSeekUp() },
                                        onRadioSeekDown       = { vm.radioSeekDown() },
                                        onRadioCycleFm        = { vm.radioCycleFm() },
                                        onRadioSwitchAm       = { vm.radioSwitchAm() },
                                        onRadioTune           = { band, freq -> vm.radioTune(band, freq) },
                                        onAssignRadio         = { vm.startRadioPicker() },
                                        onOpenIslamic         = { vm.navigate(NavDestination.ISLAMIC_COMPANION) },
                                        onOpenVoiceAssistant  = { vm.navigate(NavDestination.VOICE_ASSISTANT) },
                                        onOpenQuickControls   = { vm.navigate(NavDestination.QUICK_CONTROLS) },
                                        onOpenDiagnostics     = { vm.navigate(NavDestination.OBD_DIAGNOSTICS) },
                                        onOpenNavigationCentre = { vm.navigate(NavDestination.NAVIGATION_CENTRE) },
                                        onOpenMediaCentre     = { vm.navigate(NavDestination.MEDIA_CENTRE) }
                                    )

                                    NavDestination.NAVIGATION_CENTRE -> NavigationScreen(
                                        settings         = settings,
                                        locationFlow     = vm.location,
                                        bearingFlow      = vm.compassBearing,
                                        installedApps    = apps,
                                        accent           = accent,
                                        isDayMode        = isDayMode,
                                        onBackToCockpit  = { vm.navigate(NavDestination.HOME) }
                                    )

                                    NavDestination.MEDIA_CENTRE -> MediaScreen(
                                        nowPlaying          = nowPlaying,
                                        hardwareRadio       = hardwareRadio,
                                        installedApps       = apps,
                                        accent              = accent,
                                        isDayMode           = isDayMode,
                                        onPlayPause         = vm::playPause,
                                        onNext              = vm::skipNext,
                                        onPrev              = vm::skipPrev,
                                        onRadioSeekUp       = { vm.radioSeekUp() },
                                        onRadioSeekDown     = { vm.radioSeekDown() },
                                        onRadioCycleFm      = { vm.radioCycleFm() },
                                        onRadioSwitchAm     = { vm.radioSwitchAm() },
                                        onLaunchMediaApp    = { vm.launchApp(it) },
                                        onBackToCockpit     = { vm.navigate(NavDestination.HOME) }
                                    )

                                    NavDestination.VEHICLE_INFO -> VehicleInfoScreen(
                                        location          = vm.location.value,
                                        accent            = accent,
                                        isDayMode         = isDayMode,
                                        onBackToCockpit   = { vm.navigate(NavDestination.HOME) },
                                        onOpenDiagnostics = { vm.navigate(NavDestination.OBD_DIAGNOSTICS) }
                                    )

                                    NavDestination.OBD_DIAGNOSTICS -> ObdScreen(
                                        accent          = accent,
                                        isDayMode       = isDayMode,
                                        onBackToCockpit = { vm.navigate(NavDestination.HOME) }
                                    )

                                    NavDestination.TPMS_DASHBOARD -> TpmsScreen(
                                        accent          = accent,
                                        isDayMode       = isDayMode,
                                        onBackToCockpit = { vm.navigate(NavDestination.HOME) }
                                    )

                                    NavDestination.ISLAMIC_COMPANION -> IslamicScreen(
                                        location        = vm.location.value,
                                        bearing         = vm.compassBearing.value,
                                        method          = settings.prayerCalculationMethod,
                                        asrMethod       = settings.asrMethod,
                                        accent          = accent,
                                        isDayMode       = isDayMode,
                                        onBackToCockpit = { vm.navigate(NavDestination.HOME) },
                                        onOpenDuaModal  = { vm.previewTravelDua() }
                                    )

                                    NavDestination.VOICE_ASSISTANT -> VoiceAssistantScreen(
                                        currentWeatherSummary = weather?.temperatureDisplay(settings.unitSystem.name == "METRIC") ?: "",
                                        nextPrayerSummary     = "",
                                        currentSpeedKmh       = ((vm.location.value?.speedMps ?: 0f) * 3.6f).toInt(),
                                        accent                = accent,
                                        isDayMode             = isDayMode,
                                        onNavigateDestination = { vm.navigate(it) },
                                        onMediaPlay           = { vm.playPause() },
                                        onMediaPause          = { vm.playPause() },
                                        onMediaNext           = { vm.skipNext() },
                                        onMediaPrev           = { vm.skipPrev() },
                                        onLaunchRadio         = { vm.launchHardwareRadioApp() },
                                        onBackToCockpit       = { vm.navigate(NavDestination.HOME) }
                                    )

                                    NavDestination.QUICK_CONTROLS -> QuickControlsScreen(
                                        accent                = accent,
                                        isDayMode             = isDayMode,
                                        onNavigateDestination = { vm.navigate(it) },
                                        onBackToCockpit       = { vm.navigate(NavDestination.HOME) }
                                    )

                                    NavDestination.AUTOMATION_SETTINGS -> AutomationScreen(
                                        settings         = settings,
                                        accent           = accent,
                                        isDayMode        = isDayMode,
                                        onUpdateSettings = { block -> vm.updateSettings(block) },
                                        onBackToCockpit  = { vm.navigate(NavDestination.HOME) }
                                    )

                                    NavDestination.THEME_CUSTOMIZATION -> ThemeCustomizationScreen(
                                        settings         = settings,
                                        accent           = accent,
                                        isDayMode        = isDayMode,
                                        onUpdateSettings = { block -> vm.updateSettings(block) },
                                        onBackToCockpit  = { vm.navigate(NavDestination.HOME) }
                                    )

                                    NavDestination.PERMISSIONS_COMPATIBILITY -> PermissionsScreen(
                                        accent          = accent,
                                        isDayMode       = isDayMode,
                                        onBackToCockpit = { vm.navigate(NavDestination.HOME) }
                                    )

                                    NavDestination.APP_LIBRARY -> AppLibraryScreen(
                                        apps                = apps,
                                        isLoading           = appsLoading,
                                        isPickerMode        = pickerSlot != null,
                                        pickerSlot          = pickerSlot,
                                        isCarPlayPickerMode = appPickerTarget != null,
                                        carPlayPickerLabel  = when (appPickerTarget) {
                                            com.hoopoe.launcher.viewmodel.LauncherViewModel.AppPickerTarget.ANDROID_AUTO -> "CHOOSE ANDROID AUTO APP"
                                            com.hoopoe.launcher.viewmodel.LauncherViewModel.AppPickerTarget.PIP          -> "CHOOSE PIP APP"
                                            com.hoopoe.launcher.viewmodel.LauncherViewModel.AppPickerTarget.RADIO        -> "CHOOSE RADIO APP"
                                            else -> "CHOOSE CARPLAY APP"
                                        },
                                        accent              = accent,
                                        onAppClick          = { app -> vm.launchApp(app.packageName) },
                                        onPickerSelect      = { slot, app -> vm.assignShortcut(slot, app) },
                                        onCarPlaySelect     = { app -> vm.assignPickerApp(app) }
                                    )

                                    NavDestination.SETTINGS -> SettingsScreen(
                                        settings = settings,
                                        accent   = accent,
                                        onUpdate = { block -> vm.updateSettings(block) },
                                        onReset  = { vm.resetSettings() },
                                        onPreviewLogon = { vm.previewLogonAnimation() },
                                        onPreviewTravelDua = { vm.previewTravelDua() },
                                        onNavigate = { vm.navigate(it) }
                                    )
                                }
                            }
                        }

                        if (isBottomBar) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                mainPane(Modifier.weight(1f).fillMaxWidth())
                                androidx.compose.material3.HorizontalDivider(color = layoutDivColor)
                                sidebarContent()
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                val vDivider: @Composable () -> Unit = {
                                    androidx.compose.material3.VerticalDivider(
                                        modifier = Modifier.fillMaxHeight(),
                                        color    = layoutDivColor
                                    )
                                }
                                if (settings.sidebarPosition == SidebarPosition.LEFT) {
                                    sidebarContent()
                                    vDivider()
                                }
                                mainPane(Modifier.weight(1f).fillMaxHeight())
                                if (settings.sidebarPosition == SidebarPosition.RIGHT) {
                                    vDivider()
                                    sidebarContent()
                                }
                            }
                        }

                        if (showRefuelDialog) {
                            com.hoopoe.launcher.ui.components.RefuelDialog(
                                accent = accent,
                                onDismiss = { showRefuelDialog = false }
                            )
                        }
                    }
                }
            }
            } // CompositionLocalProvider
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshConnectivity()
        vm.refreshMedia()
    }

    override fun onStop() {
        super.onStop()
        vm.stopLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        vm.startLocationUpdates()
    }
}
