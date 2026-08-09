package com.dhiren.atom.speech

import android.speech.SpeechRecognizer
import org.junit.Assert.assertTrue
import org.junit.Test

class AtomSpeechRecognizerTest {
    @Test
    fun `network errors explain offline language support`() {
        val message = AtomSpeechRecognizer.errorMessage(SpeechRecognizer.ERROR_NETWORK)

        assertTrue(message.contains("Offline speech"))
        assertTrue(message.contains("language pack"))
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
