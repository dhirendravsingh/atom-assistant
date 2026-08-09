package com.dhiren.atom.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

enum class SpeechInputTarget {
    Command,
    MissingDate,
    MissingTime,
}

sealed interface SpeechCaptureState {
    data object Idle : SpeechCaptureState
    data class Listening(val target: SpeechInputTarget) : SpeechCaptureState
    data class Processing(val target: SpeechInputTarget) : SpeechCaptureState
    data class Error(val message: String) : SpeechCaptureState
    data class Unavailable(val message: String) : SpeechCaptureState
}

data class SpeechCapability(
    val available: Boolean,
    val isGuaranteedOnDevice: Boolean,
    val description: String,
)

class AtomSpeechRecognizer(
    context: Context,
    private val onPartialResult: (SpeechInputTarget, String) -> Unit,
    private val onFinalResult: (SpeechInputTarget, String) -> Unit,
    private val onStateChanged: (SpeechCaptureState) -> Unit,
) : RecognitionListener {
    private val appContext = context.applicationContext
    val capability: SpeechCapability = capability(appContext)
    private var activeTarget: SpeechInputTarget? = null
    private var recognizer: SpeechRecognizer? = createRecognizer(appContext, capability)

    init {
        recognizer?.setRecognitionListener(this)
    }

    fun start(target: SpeechInputTarget) {
        val activeRecognizer = recognizer
        if (activeRecognizer == null) {
            onStateChanged(SpeechCaptureState.Unavailable(capability.description))
            return
        }
        activeRecognizer.cancel()
        activeTarget = target
        onStateChanged(SpeechCaptureState.Listening(target))
        activeRecognizer.startListening(recognitionIntent())
    }

    fun stop() {
        val target = activeTarget ?: return
        onStateChanged(SpeechCaptureState.Processing(target))
        recognizer?.stopListening()
    }

    fun cancel() {
        recognizer?.cancel()
        activeTarget = null
        onStateChanged(SpeechCaptureState.Idle)
    }

    fun destroy() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        activeTarget = null
    }

    override fun onReadyForSpeech(params: Bundle?) {
        activeTarget?.let { onStateChanged(SpeechCaptureState.Listening(it)) }
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        activeTarget?.let { onStateChanged(SpeechCaptureState.Processing(it)) }
    }

    override fun onError(error: Int) {
        activeTarget = null
        onStateChanged(SpeechCaptureState.Error(errorMessage(error)))
    }

    override fun onResults(results: Bundle?) {
        val target = activeTarget ?: return
        val transcript = results.firstTranscript()
        activeTarget = null
        if (transcript.isNullOrBlank()) {
            onStateChanged(SpeechCaptureState.Error("I didn’t catch that. Please try again or type it."))
        } else {
            onFinalResult(target, transcript)
            onStateChanged(SpeechCaptureState.Idle)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val target = activeTarget ?: return
        partialResults.firstTranscript()?.takeIf { it.isNotBlank() }?.let {
            onPartialResult(target, it)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun recognitionIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
    }

    companion object {
        fun capability(context: Context): SpeechCapability {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                return SpeechCapability(
                    available = false,
                    isGuaranteedOnDevice = false,
                    description = "Speech recognition is not installed on this phone. You can keep typing reminders.",
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val onDeviceAvailable = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                return if (onDeviceAvailable) {
                    SpeechCapability(
                        available = true,
                        isGuaranteedOnDevice = true,
                        description = "On-device speech is ready",
                    )
                } else {
                    SpeechCapability(
                        available = false,
                        isGuaranteedOnDevice = false,
                        description = "Offline speech is not available for this language. Install the offline language pack or type instead.",
                    )
                }
            }
            return SpeechCapability(
                available = true,
                isGuaranteedOnDevice = false,
                description = "Offline-preferred speech is ready",
            )
        }

        internal fun errorMessage(error: Int): String = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "The microphone had an audio problem. Please try again."
            SpeechRecognizer.ERROR_CLIENT -> "Speech input was interrupted. Please try again."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice input."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER,
            -> "Offline speech could not complete. Check that your language pack is installed, or type instead."
            SpeechRecognizer.ERROR_NO_MATCH -> "I didn’t catch that. Please try again or type it."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Wait a moment and try again."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn’t hear anything. Tap the microphone and try again."
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            -> "Offline speech is unavailable for this language. Install its language pack or type instead."
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Voice input has been used too frequently. Wait a moment and try again."
            else -> "Voice input could not finish. Your text is safe; please try again."
        }

        private fun createRecognizer(
            context: Context,
            capability: SpeechCapability,
        ): SpeechRecognizer? {
            if (!capability.available) return null
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && capability.isGuaranteedOnDevice) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        }

        private fun Bundle?.firstTranscript(): String? = this
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
    }
}
