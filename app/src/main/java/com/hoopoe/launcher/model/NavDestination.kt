package com.hoopoe.launcher.model

/**
 * Navigation destinations for the HOOPOE Automotive Companion.
 * Encompasses the 13 dedicated automotive screens plus application library.
 */
enum class NavDestination(val label: String) {
    HOME("Dashboard"),
    NAVIGATION_CENTRE("Navigation"),
    MEDIA_CENTRE("Media"),
    VEHICLE_INFO("Vehicle"),
    OBD_DIAGNOSTICS("OBD-II"),
    TPMS_DASHBOARD("TPMS"),
    ISLAMIC_COMPANION("Islamic"),
    VOICE_ASSISTANT("Voice"),
    QUICK_CONTROLS("Controls"),
    AUTOMATION_SETTINGS("Automation"),
    THEME_CUSTOMIZATION("Themes"),
    SETTINGS("Settings"),
    PERMISSIONS_COMPATIBILITY("Compatibility"),
    APP_LIBRARY("Apps")
}
