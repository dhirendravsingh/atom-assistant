package com.dhiren.atom.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

fun friendlyDate(date: LocalDate = LocalDate.now()): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))

enum class LogoVariant(val label: String) {
    Original("Original"),
    Orbit("Orbit"),
    Pulse("Pulse"),
    Spark("Spark"),
    Nucleus("Nucleus"),
    Halo("Halo"),
    Bond("Bond"),
    Mono("Mono A"),
    Prism("Prism"),
    Twin("Twin"),
    Eclipse("Eclipse"),
    Ripple("Ripple"),
    Node("Node"),
    Arc("Arc"),
}
