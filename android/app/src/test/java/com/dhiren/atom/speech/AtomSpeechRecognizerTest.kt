package com.dhiren.atom.speech

import android.speech.SpeechRecognizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AtomSpeechRecognizerTest {
    @Test
    fun `network errors explain both online and offline recovery`() {
        val message = AtomSpeechRecognizer.errorMessage(SpeechRecognizer.ERROR_NETWORK)

        assertTrue(message.contains("internet connection"))
        assertTrue(message.contains("language pack"))
    }

    @Test
    fun `standard recognizer remains available without an on-device service`() {
        val capability = AtomSpeechRecognizer.capabilityFor(
            standardAvailable = true,
            onDeviceAvailable = false,
        )

        assertTrue(capability.available)
        assertFalse(capability.isGuaranteedOnDevice)
        assertTrue(capability.description.contains("permission"))
    }

    @Test
    fun `voice is unavailable only when neither recognizer exists`() {
        val capability = AtomSpeechRecognizer.capabilityFor(
            standardAvailable = false,
            onDeviceAvailable = false,
        )

        assertFalse(capability.available)
        assertTrue(capability.description.contains("not installed"))
    }

    @Test
    fun `missing on-device language offers a consent-gated online fallback`() {
        assertTrue(
            AtomSpeechRecognizer.shouldOfferOnlineFallback(
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            ),
        )
        assertFalse(
            AtomSpeechRecognizer.shouldOfferOnlineFallback(
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
            ),
        )
    }

    @Test
    fun `permission errors explain microphone requirement`() {
        val message = AtomSpeechRecognizer.errorMessage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)

        assertTrue(message.contains("Microphone permission"))
    }

    @Test
    fun `no match offers retry and typing`() {
        val message = AtomSpeechRecognizer.errorMessage(SpeechRecognizer.ERROR_NO_MATCH)

        assertTrue(message.contains("try again"))
        assertTrue(message.contains("type"))
    }

    @Test
    fun `unknown errors promise transcript safety`() {
        val message = AtomSpeechRecognizer.errorMessage(Int.MIN_VALUE)

        assertTrue(message.contains("text is safe"))
    }
}
