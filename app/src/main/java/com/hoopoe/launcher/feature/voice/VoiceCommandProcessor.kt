package com.hoopoe.launcher.feature.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import com.hoopoe.launcher.model.NavDestination
import java.util.Locale

sealed class VoiceCommandResult {
    data class NavigateToScreen(val destination: NavDestination, val spokenResponse: String) : VoiceCommandResult()
    data class LaunchGeo(val query: String, val spokenResponse: String) : VoiceCommandResult()
    data class MediaAction(val action: MediaCommandType, val spokenResponse: String) : VoiceCommandResult()
    data class ReadInfo(val spokenResponse: String) : VoiceCommandResult()
    data class Unknown(val spokenResponse: String) : VoiceCommandResult()
}

enum class MediaCommandType { PLAY, PAUSE, NEXT, PREV, LAUNCH_RADIO }

class VoiceCommandProcessor(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isTtsReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hoopoe_voice_out")
        }
    }

    fun processCommand(
        input: String,
        currentWeatherSummary: String = "",
        nextPrayerSummary: String = "",
        currentSpeedKmh: Int = 0
    ): VoiceCommandResult {
        val q = input.lowercase().trim()

        return when {
            // Navigation
            q.contains("navigate home") || q == "go home" -> {
                VoiceCommandResult.LaunchGeo("home", "Navigating to Home")
            }
            q.contains("navigate to work") || q == "go to work" -> {
                VoiceCommandResult.LaunchGeo("work", "Navigating to Work")
            }
            q.contains("petrol") || q.contains("gas station") || q.contains("fuel") -> {
                VoiceCommandResult.LaunchGeo("petrol station", "Searching for nearby petrol stations")
            }
            q.contains("mosque") || q.contains("masjid") -> {
                VoiceCommandResult.LaunchGeo("mosque", "Searching for nearby mosques")
            }
            q.contains("hospital") || q.contains("emergency") -> {
                VoiceCommandResult.LaunchGeo("hospital", "Finding nearest hospitals")
            }
            q.startsWith("navigate to ") -> {
                val dest = q.removePrefix("navigate to ")
                VoiceCommandResult.LaunchGeo(dest, "Navigating to $dest")
            }
            q.contains("navigation") || q.contains("map") -> {
                VoiceCommandResult.NavigateToScreen(NavDestination.NAVIGATION_CENTRE, "Opening Navigation Centre")
            }

            // Media
            q == "play" || q == "play music" || q == "resume" -> {
                VoiceCommandResult.MediaAction(MediaCommandType.PLAY, "Resuming playback")
            }
            q == "pause" || q == "stop music" -> {
                VoiceCommandResult.MediaAction(MediaCommandType.PAUSE, "Playback paused")
            }
            q == "next" || q == "next song" || q == "skip" -> {
                VoiceCommandResult.MediaAction(MediaCommandType.NEXT, "Skipping to next track")
            }
            q == "previous" || q == "prev song" || q == "back" -> {
                VoiceCommandResult.MediaAction(MediaCommandType.PREV, "Playing previous track")
            }
            q.contains("radio") -> {
                VoiceCommandResult.MediaAction(MediaCommandType.LAUNCH_RADIO, "Switching to FM Radio")
            }
            q.contains("media") || q.contains("music") -> {
                VoiceCommandResult.NavigateToScreen(NavDestination.MEDIA_CENTRE, "Opening Media Centre")
            }

            // Information & Status
            q.contains("weather") || q.contains("temperature") -> {
                val reply = if (currentWeatherSummary.isNotBlank()) "Currently $currentWeatherSummary" else "Weather telemetry is offline"
                VoiceCommandResult.ReadInfo(reply)
            }
            q.contains("prayer") || q.contains("namaz") || q.contains("adhan") -> {
                val reply = if (nextPrayerSummary.isNotBlank()) "Next prayer is $nextPrayerSummary" else "Prayer timings calculated offline"
                VoiceCommandResult.ReadInfo(reply)
            }
            q.contains("speed") || q.contains("how fast") -> {
                VoiceCommandResult.ReadInfo("Current vehicle speed is $currentSpeedKmh kilometers per hour")
            }

            // Screens
            q.contains("diagnostic") || q.contains("obd") || q.contains("engine") -> {
                VoiceCommandResult.NavigateToScreen(NavDestination.OBD_DIAGNOSTICS, "Opening OBD-II Diagnostics")
            }
            q.contains("tyre") || q.contains("tire") || q.contains("tpms") || q.contains("pressure") -> {
                VoiceCommandResult.NavigateToScreen(NavDestination.TPMS_DASHBOARD, "Opening TPMS Dashboard")
            }
            q.contains("islamic") || q.contains("qibla") || q.contains("dua") -> {
                VoiceCommandResult.NavigateToScreen(NavDestination.ISLAMIC_COMPANION, "Opening Islamic Companion")
            }
            q.contains("setting") -> {
                VoiceCommandResult.NavigateToScreen(NavDestination.SETTINGS, "Opening Settings")
            }
            q.contains("dashboard") || q.contains("home") -> {
                VoiceCommandResult.NavigateToScreen(NavDestination.HOME, "Returning to Cockpit Dashboard")
            }

            else -> {
                VoiceCommandResult.Unknown("Command '$input' recognized. Say 'help' for automotive commands.")
            }
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
