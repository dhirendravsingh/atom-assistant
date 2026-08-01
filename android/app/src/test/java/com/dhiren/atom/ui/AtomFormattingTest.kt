package com.dhiren.atom.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AtomFormattingTest {
    @Test
    fun `greeting follows time of day`() {
        assertEquals("Good night", greetingForHour(4))
        assertEquals("Good morning", greetingForHour(5))
        assertEquals("Good afternoon", greetingForHour(12))
        assertEquals("Good evening", greetingForHour(17))
        assertEquals("Good night", greetingForHour(22))
    }

    @Test
    fun `logo gallery keeps every approved option`() {
        assertEquals(14, LogoVariant.entries.size)
    }
}
