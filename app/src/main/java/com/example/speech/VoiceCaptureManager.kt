package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.domain.model.SupportedLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceCaptureState(
    val isListening: Boolean = false,
    val partialTranscript: String = "",
    val finalTranscript: String = "",
    val rmsDb: Float = 0f,
    val errorMessage: String? = null,
    val isSpeechAvailable: Boolean = true
)

/**
 * Wraps Android's on-device [SpeechRecognizer] for English and Spanish job dictation.
 * Gracefully degrades when speech services are unavailable (e.g., emulators without voice input)
 * so contractors can always type or load a sample transcript.
 */
class VoiceCaptureManager(private val context: Context) {

    private val _state = MutableStateFlow(
        VoiceCaptureState(isSpeechAvailable = SpeechRecognizer.isRecognitionAvailable(context))
    )
    val state: StateFlow<VoiceCaptureState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var accumulatedPrefix: String = ""

    fun startListening(language: SupportedLanguage, existingTranscript: String = "") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = _state.value.copy(
                isListening = false,
                isSpeechAvailable = false,
                errorMessage = if (language == SupportedLanguage.ES) {
                    "Reconocimiento de voz no disponible en este dispositivo. Escribe o usa un ejemplo."
                } else {
                    "On-device speech recognition is unavailable here. Type your job or try a sample."
                }
            )
            return
        }

        accumulatedPrefix = existingTranscript.trim()
        destroyRecognizer()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = _state.value.copy(isListening = true, errorMessage = null)
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _state.value = _state.value.copy(rmsDb = normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                _state.value = _state.value.copy(isListening = false, rmsDb = 0f)
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech heard — tap the mic and try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out — tap to continue."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Voice network hiccup — switch to offline language pack or type directly."
                    else -> "Voice capture stopped (code $error)."
                }
                _state.value = _state.value.copy(
                    isListening = false,
                    rmsDb = 0f,
                    errorMessage = message
                )
            }

            override fun onResults(results: Bundle?) {
                val spoken = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                val combined = listOf(accumulatedPrefix, spoken)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                _state.value = _state.value.copy(
                    isListening = false,
                    rmsDb = 0f,
                    partialTranscript = "",
                    finalTranscript = combined,
                    errorMessage = null
                )
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                val combined = listOf(accumulatedPrefix, partial)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                _state.value = _state.value.copy(partialTranscript = combined)
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        }

        _state.value = _state.value.copy(isListening = true, errorMessage = null)
        recognizer.startListening(intent)
    }

    fun stopListening() {
        runCatching { speechRecognizer?.stopListening() }
        _state.value = _state.value.copy(isListening = false, rmsDb = 0f)
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun destroyRecognizer() {
        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }
}
