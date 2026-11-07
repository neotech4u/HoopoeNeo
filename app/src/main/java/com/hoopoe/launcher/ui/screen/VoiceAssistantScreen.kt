package com.hoopoe.launcher.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoopoe.launcher.feature.voice.MediaCommandType
import com.hoopoe.launcher.feature.voice.VoiceCommandProcessor
import com.hoopoe.launcher.feature.voice.VoiceCommandResult
import com.hoopoe.launcher.model.NavDestination
import java.util.Locale

@Composable
fun VoiceAssistantScreen(
    currentWeatherSummary: String,
    nextPrayerSummary: String,
    currentSpeedKmh: Int,
    accent: Color,
    isDayMode: Boolean = false,
    onNavigateDestination: (NavDestination) -> Unit,
    onMediaPlay: () -> Unit,
    onMediaPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onLaunchRadio: () -> Unit,
    onBackToCockpit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val processor = remember { VoiceCommandProcessor(context) }
    DisposableEffect(Unit) {
        onDispose { processor.release() }
    }

    var recognizedText by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("Tap the microphone and speak an automotive command.") }
    var isListening by remember { mutableStateOf(false) }

    val contentColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBorder = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF243048)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF0F141F).copy(alpha = 0.85f)

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spoken.isNotBlank()) {
                recognizedText = spoken
                val cmdResult = processor.processCommand(
                    input = spoken,
                    currentWeatherSummary = currentWeatherSummary,
                    nextPrayerSummary = nextPrayerSummary,
                    currentSpeedKmh = currentSpeedKmh
                )

                when (cmdResult) {
                    is VoiceCommandResult.NavigateToScreen -> {
                        responseText = cmdResult.spokenResponse
                        processor.speak(cmdResult.spokenResponse)
                        onNavigateDestination(cmdResult.destination)
                    }
                    is VoiceCommandResult.LaunchGeo -> {
                        responseText = cmdResult.spokenResponse
                        processor.speak(cmdResult.spokenResponse)
                        val geoIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(cmdResult.query))).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        runCatching { context.startActivity(geoIntent) }
                    }
                    is VoiceCommandResult.MediaAction -> {
                        responseText = cmdResult.spokenResponse
                        processor.speak(cmdResult.spokenResponse)
                        when (cmdResult.action) {
                            MediaCommandType.PLAY -> onMediaPlay()
                            MediaCommandType.PAUSE -> onMediaPause()
                            MediaCommandType.NEXT -> onMediaNext()
                            MediaCommandType.PREV -> onMediaPrev()
                            MediaCommandType.LAUNCH_RADIO -> onLaunchRadio()
                        }
                    }
                    is VoiceCommandResult.ReadInfo -> {
                        responseText = cmdResult.spokenResponse
                        processor.speak(cmdResult.spokenResponse)
                    }
                    is VoiceCommandResult.Unknown -> {
                        responseText = cmdResult.spokenResponse
                        processor.speak(cmdResult.spokenResponse)
                    }
                }
            }
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a car command...")
        }
        isListening = true
        runCatching { speechLauncher.launch(intent) }.onFailure { isListening = false }
    }

    val sampleCommands = listOf(
        "\"Navigate home\" / \"Navigate to petrol station\"",
        "\"Play music\" / \"Pause music\" / \"Next song\"",
        "\"What's the weather?\"",
        "\"Next prayer time\"",
        "\"Open diagnostics\" / \"Open TPMS\""
    )

    Row(modifier = modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {

        // Left: Mic Trigger & Response Card
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
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
                Spacer(Modifier.width(8.dp))
                Text("VOICE ASSISTANT", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Large Push-To-Talk Button
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(if (isListening) Color(0xFFFF1744) else accent)
                        .clickable { startListening() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color(0xFF07090E),
                        modifier = Modifier.size(46.dp)
                    )
                }

                Text(
                    text = if (isListening) "LISTENING..." else "TAP TO SPEAK",
                    color = if (isListening) Color(0xFFFF1744) else accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Dialogue Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (recognizedText.isNotBlank()) {
                    Text("YOU SAID:", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("\"$recognizedText\"", color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                }
                Text("HOOPOE:", color = Color(0xFF00E676), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(responseText, color = contentColor.copy(alpha = 0.85f), fontSize = 12.sp)
            }
        }

        // Right: Supported Commands Reference
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("SUPPORTED AUTOMOTIVE COMMANDS", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Push-to-talk speech recognition runs on-device via Android Speech API.", color = contentColor.copy(alpha = 0.5f), fontSize = 9.5.sp)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sampleCommands) { cmd ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.RecordVoiceOver, null, tint = accent, modifier = Modifier.size(16.dp))
                            Text(cmd, color = contentColor, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
