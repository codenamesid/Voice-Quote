package com.example.ui.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.ai.SampleTranscriptFixture
import com.example.data.ai.SampleTranscriptFixtures
import com.example.domain.model.Business
import com.example.domain.model.SupportedLanguage
import com.example.speech.VoiceCaptureManager
import com.example.ui.ExtractionUiState
import com.example.ui.components.AiPlaceholderNoticeBanner
import com.example.ui.components.GlovePrimaryButton
import com.example.ui.components.LanguageToggleBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PaidEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SafetyAmberBright
import com.example.ui.theme.SlateInk
import com.example.ui.theme.SlateNavy
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun RecordScreen(
    business: Business?,
    transcript: String,
    spokenLanguage: SupportedLanguage,
    extractionState: ExtractionUiState,
    isAiPlaceholderMode: Boolean,
    onTranscriptChange: (String) -> Unit,
    onSpokenLanguageChange: (SupportedLanguage) -> Unit,
    onSelectSample: (SampleTranscriptFixture) -> Unit,
    onBuildQuote: () -> Unit,
    onDismissError: () -> Unit
) {
    val context = LocalContext.current
    val voiceManager = remember(context) { VoiceCaptureManager(context) }
    val voiceState by voiceManager.state.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            voiceManager.startListening(spokenLanguage, transcript)
        }
    }

    // Cap single recording at 3 minutes (180 seconds)
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(voiceState.isListening) {
        if (voiceState.isListening) {
            elapsedSeconds = 0
            while (voiceState.isListening && elapsedSeconds < 180) {
                delay(1_000L)
                elapsedSeconds += 1
            }
            if (elapsedSeconds >= 180) {
                voiceManager.stopListening()
            }
        }
    }

    LaunchedEffect(voiceState.finalTranscript) {
        if (voiceState.finalTranscript.isNotBlank()) {
            onTranscriptChange(voiceState.finalTranscript)
        }
    }

    DisposableEffect(voiceManager) {
        onDispose { voiceManager.destroyRecognizer() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Offline-ready status pill + Business header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = business?.name ?: "QuoteVoice",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.record_title),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = PaidEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "OFFLINE OK",
                        style = MaterialTheme.typography.labelMedium,
                        color = PaidEmerald
                    )
                }
            }
        }

        if (isAiPlaceholderMode) {
            AiPlaceholderNoticeBanner()
        }

        // Spoken Language Toggle
        LanguageToggleBar(
            selected = spokenLanguage,
            onSelect = onSpokenLanguageChange,
            testTagPrefix = "record_lang"
        )

        // Giant Glove-Friendly Mic Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateNavy)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(SlateInk, SlateNavy))
                    )
                    .padding(horizontal = 20.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.record_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD1D5DB)
                )
                Spacer(Modifier.height(22.dp))

                Box(contentAlignment = Alignment.Center) {
                    if (voiceState.isListening) {
                        val pulseTransition = rememberInfiniteTransition(label = "micPulse")
                        val pulseScale by pulseTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.14f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(650),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(172.dp)
                                .scale(pulseScale + (voiceState.rmsDb * 0.12f))
                                .background(
                                    color = SafetyAmberBright.copy(alpha = 0.24f),
                                    shape = CircleShape
                                )
                        )
                    }
                    Surface(
                        onClick = {
                            onDismissError()
                            if (voiceState.isListening) {
                                voiceManager.stopListening()
                            } else if (hasAudioPermission) {
                                voiceManager.startListening(spokenLanguage, transcript)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(144.dp)
                            .testTag("record_mic_button"),
                        shape = CircleShape,
                        color = if (voiceState.isListening) MaterialTheme.colorScheme.error else SafetyAmber,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (voiceState.isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = stringResource(
                                    if (voiceState.isListening) R.string.record_listening else R.string.record_tap_to_speak
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                val formattedTime = String.format(
                    Locale.US,
                    "%02d:%02d / 03:00",
                    elapsedSeconds / 60,
                    elapsedSeconds % 60
                )
                Text(
                    text = if (voiceState.isListening) formattedTime else stringResource(R.string.record_tap_to_speak),
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (voiceState.isListening) SafetyAmberBright else Color.White
                )

                voiceState.errorMessage?.let { err ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }
        }

        // Quick 5 EN/ES Sample Transcripts (for fast field demos & testing without mic hardware)
        Text(
            text = stringResource(R.string.record_try_sample),
            style = MaterialTheme.typography.titleMedium
        )
        LazyRow(
            modifier = Modifier.testTag("sample_transcripts_row"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(SampleTranscriptFixtures.all, key = { it.id }) { fixture ->
                Surface(
                    onClick = {
                        onDismissError()
                        onSelectSample(fixture)
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("sample_transcript_${fixture.id}"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fixture.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Editable live transcript box
        val displayedTranscript = if (voiceState.isListening && voiceState.partialTranscript.isNotBlank()) {
            voiceState.partialTranscript
        } else {
            transcript
        }

        OutlinedTextField(
            value = displayedTranscript,
            onValueChange = {
                onDismissError()
                onTranscriptChange(it)
            },
            label = { Text(stringResource(R.string.record_transcript_hint)) },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("record_transcript_input")
        )

        if (extractionState is ExtractionUiState.Error) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = extractionState.message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (extractionState is ExtractionUiState.Extracting) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Extracting & pricing line items…",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            GlovePrimaryButton(
                text = stringResource(R.string.record_extract_cta),
                icon = Icons.Filled.AutoAwesome,
                enabled = displayedTranscript.isNotBlank(),
                onClick = onBuildQuote,
                modifier = Modifier.testTag("build_quote_button")
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}
