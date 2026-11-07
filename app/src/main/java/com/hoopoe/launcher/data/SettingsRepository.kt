package com.hoopoe.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

class SettingsRepository(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val VEHICLE_NAME       = stringPreferencesKey("vehicle_name")
        val ACCENT_COLOR       = intPreferencesKey("accent_color")
        val BG_COLOR           = intPreferencesKey("bg_color")
        val FONT_COLOR         = intPreferencesKey("font_color")
        val WALLPAPER_URI      = stringPreferencesKey("wallpaper_uri")
        val FONT_BOLD          = booleanPreferencesKey("font_bold")
        val TEXT_SCALE         = floatPreferencesKey("text_scale")
        val UI_SCALE           = floatPreferencesKey("ui_scale")
        val CLOCK_STYLE        = stringPreferencesKey("clock_style")
        val UNIT_SYSTEM        = stringPreferencesKey("unit_system")
        val APP_FONT           = stringPreferencesKey("app_font")
        val SHOW_WEATHER       = booleanPreferencesKey("show_weather")
        val SHOW_CLOCK         = booleanPreferencesKey("show_clock")
        val SHOW_TELEMETRY     = booleanPreferencesKey("show_telemetry")
        val SHOW_NOW_PLAYING   = booleanPreferencesKey("show_now_playing")
        val SHOW_ALTIMETER     = booleanPreferencesKey("show_altimeter")
        val SHOW_SPEEDOMETER   = booleanPreferencesKey("show_speedometer")
        val SHORTCUTS_JSON     = stringPreferencesKey("shortcuts_json")
        val WIDGET_LAYOUT_JSON = stringPreferencesKey("widget_layout_json")
        val CAR_PLAY_PACKAGE      = stringPreferencesKey("car_play_package")
        val ANDROID_AUTO_PACKAGE  = stringPreferencesKey("android_auto_package")
        val USE_GRADIENT          = booleanPreferencesKey("use_gradient")
        val GRADIENT_END_COLOR    = intPreferencesKey("gradient_end_color")
        val WALLPAPER_DIM         = floatPreferencesKey("wallpaper_dim")
        val RIGHT_HAND_DRIVE      = booleanPreferencesKey("right_hand_drive") // kept for migration
        val SIDEBAR_POSITION           = stringPreferencesKey("sidebar_position")
        val BOTTOM_BAR_SHORTCUTS_RIGHT = booleanPreferencesKey("bottom_bar_shortcuts_right")
        val DAY_NIGHT_MODE        = stringPreferencesKey("day_night_mode")
        val SHOW_PIP              = booleanPreferencesKey("show_pip")
        val PIP_APP_PACKAGE       = stringPreferencesKey("pip_app_package")
        val RADIO_PACKAGE         = stringPreferencesKey("radio_package")
        val ONBOARDING_COMPLETED  = booleanPreferencesKey("onboarding_completed")
        val SHOW_VITALS           = booleanPreferencesKey("show_vitals")
        val SHOW_TRIP_TRACKER     = booleanPreferencesKey("show_trip_tracker")
        val COMPASS_OFFSET        = floatPreferencesKey("compass_offset")
        val SHOW_SOUNDBOARD       = booleanPreferencesKey("show_soundboard")
        val SOUNDBOARD_PADS_JSON  = stringPreferencesKey("soundboard_pads_json")
        val VITALS_AS_BARS        = booleanPreferencesKey("vitals_as_bars")
        val SPEEDOMETER_DIGITAL_ONLY = booleanPreferencesKey("speedometer_digital_only")
        val GRADIENT_DIRECTION    = stringPreferencesKey("gradient_direction")
        val USE_CUSTOM_BG_COLOR   = booleanPreferencesKey("use_custom_bg_color")
        val PLAY_LOGON_ANIMATION  = booleanPreferencesKey("play_logon_animation")
        val SHOW_TRAVEL_DUA       = booleanPreferencesKey("show_travel_dua")
        val SHOW_FUEL_HUB         = booleanPreferencesKey("show_fuel_hub")
        val SHOW_NAV_MAP          = booleanPreferencesKey("show_nav_map")
        val SHOW_RADIO            = booleanPreferencesKey("show_radio")

        // Companion Extensions
        val AUTO_LAUNCH_BOOT      = booleanPreferencesKey("auto_launch_boot")
        val DASHBOARD_LAYOUT      = stringPreferencesKey("dashboard_layout")
        val ACTIVE_THEME          = stringPreferencesKey("active_theme")
        val PRAYER_CALC_METHOD    = stringPreferencesKey("prayer_calc_method")
        val ASR_CALC_METHOD       = stringPreferencesKey("asr_calc_method")
        val PRAYER_ALERTS_ENABLED = booleanPreferencesKey("prayer_alerts_enabled")
        val SHOW_PRAYER_WIDGET    = booleanPreferencesKey("show_prayer_widget")
        val OVERSPEED_ENABLED     = booleanPreferencesKey("overspeed_enabled")
        val OVERSPEED_THRESHOLD   = floatPreferencesKey("overspeed_threshold")
        val AUTO_BT_MEDIA         = booleanPreferencesKey("auto_bt_media")
        val HOME_ADDRESS          = stringPreferencesKey("home_address")
        val WORK_ADDRESS          = stringPreferencesKey("work_address")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> readSettings(prefs) }

    private fun readSettings(prefs: Preferences): AppSettings {
        val defaults = AppSettings()
        val shortcutsJson = prefs[Keys.SHORTCUTS_JSON]
        val shortcuts = if (shortcutsJson != null) {
            runCatching {
                gson.fromJson<List<ShortcutConfig>>(
                    shortcutsJson,
                    object : TypeToken<List<ShortcutConfig>>() {}.type
                )
            }.getOrNull() ?: defaults.shortcuts
        } else defaults.shortcuts

        val widgetJson = prefs[Keys.WIDGET_LAYOUT_JSON]
        val widgets = if (widgetJson != null) {
            val loaded = runCatching {
                gson.fromJson<List<WidgetConfig>>(
                    widgetJson,
                    object : TypeToken<List<WidgetConfig>>() {}.type
                )
            }.getOrNull() ?: defaults.widgetLayout
            if (loaded.none { it.id == "NAV_MAP" } || loaded.none { it.id == "FUEL_HUB" } || loaded.none { it.gridX >= 2 }) {
                defaults.widgetLayout
            } else loaded
        } else defaults.widgetLayout

        return AppSettings(
            vehicleName    = prefs[Keys.VEHICLE_NAME]     ?: defaults.vehicleName,
            accentColor    = prefs[Keys.ACCENT_COLOR]     ?: defaults.accentColor,
            backgroundColor = prefs[Keys.BG_COLOR]        ?: defaults.backgroundColor,
            fontColor      = prefs[Keys.FONT_COLOR]       ?: defaults.fontColor,
            wallpaperUri   = prefs[Keys.WALLPAPER_URI]    ?: defaults.wallpaperUri,
            fontBold       = prefs[Keys.FONT_BOLD]        ?: defaults.fontBold,
            textScale      = prefs[Keys.TEXT_SCALE]       ?: defaults.textScale,
            uiScale        = prefs[Keys.UI_SCALE]         ?: defaults.uiScale,
            clockStyle     = prefs[Keys.CLOCK_STYLE]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() } ?: defaults.clockStyle,
            unitSystem     = prefs[Keys.UNIT_SYSTEM]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: defaults.unitSystem,
            appFont        = prefs[Keys.APP_FONT]?.let { runCatching { AppFont.valueOf(it) }.getOrNull() } ?: defaults.appFont,
            showWeather    = prefs[Keys.SHOW_WEATHER]     ?: defaults.showWeather,
            showClock      = prefs[Keys.SHOW_CLOCK]       ?: defaults.showClock,
            showTelemetry  = prefs[Keys.SHOW_TELEMETRY]   ?: defaults.showTelemetry,
            showNowPlaying = prefs[Keys.SHOW_NOW_PLAYING] ?: defaults.showNowPlaying,
            showAltimeter   = prefs[Keys.SHOW_ALTIMETER]   ?: defaults.showAltimeter,
            showSpeedometer = prefs[Keys.SHOW_SPEEDOMETER] ?: defaults.showSpeedometer,
            shortcuts      = shortcuts,
            widgetLayout   = widgets,
            carPlayPackage      = prefs[Keys.CAR_PLAY_PACKAGE]      ?: defaults.carPlayPackage,
            androidAutoPackage  = prefs[Keys.ANDROID_AUTO_PACKAGE]  ?: defaults.androidAutoPackage,
            useGradient      = prefs[Keys.USE_GRADIENT]        ?: defaults.useGradient,
            gradientEndColor = prefs[Keys.GRADIENT_END_COLOR]  ?: defaults.gradientEndColor,
            wallpaperDim     = prefs[Keys.WALLPAPER_DIM]       ?: defaults.wallpaperDim,
            sidebarPosition  = prefs[Keys.SIDEBAR_POSITION]?.let { runCatching { SidebarPosition.valueOf(it) }.getOrNull() }
                               ?: if (prefs[Keys.RIGHT_HAND_DRIVE] == true) SidebarPosition.RIGHT else defaults.sidebarPosition,
            bottomBarShortcutsRight = prefs[Keys.BOTTOM_BAR_SHORTCUTS_RIGHT] ?: defaults.bottomBarShortcutsRight,
            dayNightMode     = prefs[Keys.DAY_NIGHT_MODE]?.let { runCatching { DayNightMode.valueOf(it) }.getOrNull() } ?: defaults.dayNightMode,
            showPip          = prefs[Keys.SHOW_PIP]         ?: defaults.showPip,
            pipAppPackage    = prefs[Keys.PIP_APP_PACKAGE]  ?: defaults.pipAppPackage,
            radioPackage     = prefs[Keys.RADIO_PACKAGE]    ?: defaults.radioPackage,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
            showVitals       = prefs[Keys.SHOW_VITALS]      ?: defaults.showVitals,
            showTripTracker  = prefs[Keys.SHOW_TRIP_TRACKER] ?: defaults.showTripTracker,
            compassOffset    = prefs[Keys.COMPASS_OFFSET]    ?: defaults.compassOffset,
            showSoundboard   = prefs[Keys.SHOW_SOUNDBOARD]   ?: defaults.showSoundboard,
            soundboardPads   = prefs[Keys.SOUNDBOARD_PADS_JSON]?.let {
                runCatching {
                    gson.fromJson<List<SoundPadConfig>>(it, object : com.google.gson.reflect.TypeToken<List<SoundPadConfig>>() {}.type)
                }.getOrNull()
            } ?: defaults.soundboardPads,
            vitalsAsBars     = prefs[Keys.VITALS_AS_BARS] ?: defaults.vitalsAsBars,
            speedometerDigitalOnly = prefs[Keys.SPEEDOMETER_DIGITAL_ONLY] ?: defaults.speedometerDigitalOnly,
            gradientDirection = prefs[Keys.GRADIENT_DIRECTION]?.let { runCatching { GradientDirection.valueOf(it) }.getOrNull() } ?: defaults.gradientDirection,
            useCustomBackgroundColor = prefs[Keys.USE_CUSTOM_BG_COLOR] ?: defaults.useCustomBackgroundColor,
            playLogonAnimation       = prefs[Keys.PLAY_LOGON_ANIMATION] ?: defaults.playLogonAnimation,
            showTravelDua            = prefs[Keys.SHOW_TRAVEL_DUA] ?: defaults.showTravelDua,
            showFuelHub              = prefs[Keys.SHOW_FUEL_HUB] ?: defaults.showFuelHub,
            showNavMap               = prefs[Keys.SHOW_NAV_MAP] ?: defaults.showNavMap,
            showRadio                = prefs[Keys.SHOW_RADIO] ?: defaults.showRadio,

            // Companion Extensions
            autoLaunchOnBoot = prefs[Keys.AUTO_LAUNCH_BOOT] ?: defaults.autoLaunchOnBoot,
            dashboardLayout  = prefs[Keys.DASHBOARD_LAYOUT]?.let { runCatching { DashboardLayoutPreset.valueOf(it) }.getOrNull() } ?: defaults.dashboardLayout,
            activeTheme      = prefs[Keys.ACTIVE_THEME]?.let { runCatching { ThemePreset.valueOf(it) }.getOrNull() } ?: defaults.activeTheme,
            prayerCalculationMethod = prefs[Keys.PRAYER_CALC_METHOD]?.let { runCatching { PrayerCalcMethod.valueOf(it) }.getOrNull() } ?: defaults.prayerCalculationMethod,
            asrMethod        = prefs[Keys.ASR_CALC_METHOD]?.let { runCatching { AsrCalcMethod.valueOf(it) }.getOrNull() } ?: defaults.asrMethod,
            prayerAlertsEnabled = prefs[Keys.PRAYER_ALERTS_ENABLED] ?: defaults.prayerAlertsEnabled,
            showPrayerWidget    = prefs[Keys.SHOW_PRAYER_WIDGET]    ?: defaults.showPrayerWidget,
            overspeedWarningEnabled = prefs[Keys.OVERSPEED_ENABLED] ?: defaults.overspeedWarningEnabled,
            overspeedThresholdKmh   = prefs[Keys.OVERSPEED_THRESHOLD] ?: defaults.overspeedThresholdKmh,
            autoResumeBluetoothMedia = prefs[Keys.AUTO_BT_MEDIA] ?: defaults.autoResumeBluetoothMedia,
            homeAddress      = prefs[Keys.HOME_ADDRESS] ?: defaults.homeAddress,
            workAddress      = prefs[Keys.WORK_ADDRESS] ?: defaults.workAddress
        )
    }

    suspend fun saveSettings(s: AppSettings) {
        context.dataStore.edit { prefs -> writeSettings(prefs, s) }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs -> writeSettings(prefs, transform(readSettings(prefs))) }
    }

    private fun writeSettings(prefs: MutablePreferences, s: AppSettings) {
        prefs[Keys.VEHICLE_NAME]       = s.vehicleName
        prefs[Keys.ACCENT_COLOR]       = s.accentColor
        prefs[Keys.BG_COLOR]           = s.backgroundColor
        prefs[Keys.FONT_COLOR]         = s.fontColor
        prefs[Keys.WALLPAPER_URI]      = s.wallpaperUri
        prefs[Keys.FONT_BOLD]          = s.fontBold
        prefs[Keys.TEXT_SCALE]         = s.textScale
        prefs[Keys.UI_SCALE]           = s.uiScale
        prefs[Keys.CLOCK_STYLE]        = s.clockStyle.name
        prefs[Keys.UNIT_SYSTEM]        = s.unitSystem.name
        prefs[Keys.APP_FONT]           = s.appFont.name
        prefs[Keys.SHOW_WEATHER]       = s.showWeather
        prefs[Keys.SHOW_CLOCK]         = s.showClock
        prefs[Keys.SHOW_TELEMETRY]     = s.showTelemetry
        prefs[Keys.SHOW_NOW_PLAYING]   = s.showNowPlaying
        prefs[Keys.SHOW_ALTIMETER]     = s.showAltimeter
        prefs[Keys.SHOW_SPEEDOMETER]   = s.showSpeedometer
        prefs[Keys.SHORTCUTS_JSON]     = gson.toJson(s.shortcuts)
        prefs[Keys.WIDGET_LAYOUT_JSON] = gson.toJson(s.widgetLayout)
        prefs[Keys.CAR_PLAY_PACKAGE]      = s.carPlayPackage
        prefs[Keys.ANDROID_AUTO_PACKAGE]  = s.androidAutoPackage
        prefs[Keys.USE_GRADIENT]       = s.useGradient
        prefs[Keys.GRADIENT_END_COLOR] = s.gradientEndColor
        prefs[Keys.WALLPAPER_DIM]      = s.wallpaperDim
        prefs[Keys.SIDEBAR_POSITION]           = s.sidebarPosition.name
        prefs[Keys.BOTTOM_BAR_SHORTCUTS_RIGHT] = s.bottomBarShortcutsRight
        prefs[Keys.DAY_NIGHT_MODE]     = s.dayNightMode.name
        prefs[Keys.SHOW_PIP]           = s.showPip
        prefs[Keys.PIP_APP_PACKAGE]    = s.pipAppPackage
        prefs[Keys.RADIO_PACKAGE]      = s.radioPackage
        prefs[Keys.ONBOARDING_COMPLETED] = s.onboardingCompleted
        prefs[Keys.SHOW_VITALS]        = s.showVitals
        prefs[Keys.SHOW_TRIP_TRACKER]  = s.showTripTracker
        prefs[Keys.COMPASS_OFFSET]     = s.compassOffset
        prefs[Keys.SHOW_SOUNDBOARD]    = s.showSoundboard
        prefs[Keys.SOUNDBOARD_PADS_JSON] = gson.toJson(s.soundboardPads)
        prefs[Keys.VITALS_AS_BARS]     = s.vitalsAsBars
        prefs[Keys.SPEEDOMETER_DIGITAL_ONLY] = s.speedometerDigitalOnly
        prefs[Keys.GRADIENT_DIRECTION] = s.gradientDirection.name
        prefs[Keys.USE_CUSTOM_BG_COLOR] = s.useCustomBackgroundColor
        prefs[Keys.PLAY_LOGON_ANIMATION]  = s.playLogonAnimation
        prefs[Keys.SHOW_TRAVEL_DUA]       = s.showTravelDua
        prefs[Keys.SHOW_FUEL_HUB]         = s.showFuelHub
        prefs[Keys.SHOW_NAV_MAP]          = s.showNavMap
        prefs[Keys.SHOW_RADIO]            = s.showRadio

        // Companion Extensions
        prefs[Keys.AUTO_LAUNCH_BOOT]      = s.autoLaunchOnBoot
        prefs[Keys.DASHBOARD_LAYOUT]      = s.dashboardLayout.name
        prefs[Keys.ACTIVE_THEME]          = s.activeTheme.name
        prefs[Keys.PRAYER_CALC_METHOD]    = s.prayerCalculationMethod.name
        prefs[Keys.ASR_CALC_METHOD]       = s.asrMethod.name
        prefs[Keys.PRAYER_ALERTS_ENABLED] = s.prayerAlertsEnabled
        prefs[Keys.SHOW_PRAYER_WIDGET]    = s.showPrayerWidget
        prefs[Keys.OVERSPEED_ENABLED]     = s.overspeedWarningEnabled
        prefs[Keys.OVERSPEED_THRESHOLD]   = s.overspeedThresholdKmh
        prefs[Keys.AUTO_BT_MEDIA]         = s.autoResumeBluetoothMedia
        prefs[Keys.HOME_ADDRESS]          = s.homeAddress
        prefs[Keys.WORK_ADDRESS]          = s.workAddress
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }
}
