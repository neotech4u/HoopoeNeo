package com.hoopoe.launcher.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import android.speech.tts.TextToSpeech
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import java.util.Locale

@Composable
fun TravelDuaScreen(
    accent: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasRead by remember { mutableStateOf(false) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialize TTS
            }
        }
        ttsInstance = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun toggleAudio() {
        if (isPlayingAudio) {
            ttsInstance?.stop()
            isPlayingAudio = false
        } else {
            isPlayingAudio = true
            val duaArabic = "Allahu Akbar, Allahu Akbar, Allahu Akbar. Subhanallazi sakh-khara lana haza wa ma kunna lahu muqrinin, wa inna ila Rabbina lamun-qaliboon. Allahumma inna nas-aluka fee safarina haza al-birra wat-taqwa."
            ttsInstance?.speak(duaArabic, TextToSpeech.QUEUE_FLUSH, null, "dua_audio")
        }
    }

    // When driver clicks "I Have Read", show "Fi Amanillah • Happy Journey" then dismiss smoothly
    LaunchedEffect(hasRead) {
        if (hasRead) {
            delay(1800)
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090E).copy(alpha = 0.94f))
            .clickable { if (hasRead) onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = hasRead,
            transitionSpec = {
                fadeIn() + scaleIn(initialScale = 0.9f) togetherWith
                fadeOut() + scaleOut(targetScale = 1.05f)
            },
            label = "dua_state_transition"
        ) { isCompleted ->
            if (isCompleted) {
                // ── Fi Amanillah / Happy Journey Card ──────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F141F).copy(alpha = 0.95f))
                        .border(1.5.dp, Color(0xFF00E676), RoundedCornerShape(24.dp))
                        .padding(horizontal = 32.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = 0.15f))
                            .border(1.5.dp, Color(0xFF00E676), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "فِي أَمَانِ اللَّهِ",
                        color = Color(0xFF00E676),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Fi Aman Allah",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Happy Journey • இனிய பயணம் அமையட்டும்",
                        color = Color(0xFF00E5FF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "அல்லாஹ்வின் பாதுகாப்புடன் உங்கள் பயணம் இனிதாகட்டும்",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // ── Travel Dua Display Screen ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.92f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F141F).copy(alpha = 0.92f))
                        .border(1.dp, Color(0xFF243048), RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar: Greeting & Skip Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "அஸ்ஸலாமு அலைக்கும் • Assalamu Alaikum",
                                color = Color(0xFF00E5FF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Please read the travel dua before starting your journey",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Audio recitation toggle
                            IconButton(
                                onClick = { toggleAudio() },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlayingAudio) Color(0xFF00E676).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = if (isPlayingAudio) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Audio Recitation",
                                    tint = if (isPlayingAudio) Color(0xFF00E676) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Quick skip button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Skip",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color(0xFF243048),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Scrollable Dua Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Arabic Takbeer & Opening
                        Text(
                            text = "اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ ، اللَّهُ أَكْبَرُ",
                            color = Color(0xFFFF9100),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنْقَلِبُونَ",
                            color = Color(0xFFF8FAFC),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 26.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(4.dp))

                        // Full User-Specified Tamil Transliteration Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFF243048), RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "அல்லாஹு அக்(b)பர், அல்லாஹு அக்(b)பர், அல்லாஹு அக்(b)பர்.\n\n" +
                                       "ஸு(b)ப்ஹானல்ல(d)தீ ஸ(kh)க்கர லனா ஹா(d)தா வமா குன்னா லஹு மு(q)க்ரினீன். வஇன்னா இலா ர(b)ப்பினா லமுன்(q)கலிபூன்.\n\n" +
                                       "அல்லா ஹும்ம இன்னா நஸ்அலு(க்)க (f)பீ ஸ(f)பரினா ஹா(d)தல்(b)பிர்ர வ(th)த்த(q)க்வா வமினல் அமலி மா(th)தர்ளா.\n\n" +
                                       "அல்லாஹும்ம ஹவ்வின் அலைனா ஸ(f)பரனா ஹா(d)தா வ(th)த்வி அன்னா (b)புஃ(d)தஹு, அல்லாஹும்ம அன்(த்)தஸ் ஸாஹி(b)பு (f)பிஸ்ஸ(f)பரி வல் (kh)கலீ(f)ப(th)து (f)பில் அஹ்லி.\n\n" +
                                       "அல்லா ஹும்ம இன்னீ அவூ(d)து பி(க்)க மின் வஃஸாயிஸ் ஸ(f)பரி வகா(b)ப(th)தில் மன்(d)ளரி வஸுயில் முன்(q)கல(b)பி (f)பில் மாலி வல் அஹ்லி.",
                                color = Color(0xFFF1F5F9),
                                fontSize = 13.5.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Tactile In-Car "I Have Read" Action Button
                    Button(
                        onClick = { hasRead = true },
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF07090E)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "வாசித்தேன் • I READ (ஆமீன்)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
