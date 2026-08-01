package com.dhiren.atom.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class AtomPalette(
    val canvas: Color,
    val paper: Color,
    val surface: Color,
    val elevated: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val mint: Color,
    val mintDark: Color,
    val mintPale: Color,
    val coral: Color,
    val coralPale: Color,
    val lime: Color,
    val sky: Color,
    val quickCard: Color,
    val quickCardText: Color,
)

private val LightAtomPalette = AtomPalette(
    canvas = Color(0xFFEDF0EB),
    paper = Color(0xFFF8FAF6),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFFFFFFF),
    ink = Color(0xFF101311),
    muted = Color(0xFF68706A),
    line = Color(0x1A101311),
    mint = Color(0xFF5EB996),
    mintDark = Color(0xFF2D8568),
    mintPale = Color(0xFFDFF2E9),
    coral = Color(0xFFEB6B4F),
    coralPale = Color(0xFFFBE6DF),
    lime = Color(0xFFD8ED75),
    sky = Color(0xFFCBE4EF),
    quickCard = Color(0xFF111713),
    quickCardText = Color(0xFFF4F8F5),
)

private val DarkAtomPalette = AtomPalette(
    canvas = Color(0xFF080B09),
    paper = Color(0xFF101511),
    surface = Color(0xFF171D18),
    elevated = Color(0xFF1D241F),
    ink = Color(0xFFF2F6F3),
    muted = Color(0xFF9AA49D),
    line = Color(0x24F2F6F3),
    mint = Color(0xFF65C29E),
    mintDark = Color(0xFF82D2B3),
    mintPale = Color(0xFF18382C),
    coral = Color(0xFFFF8266),
    coralPale = Color(0xFF41231E),
    lime = Color(0xFFCFEA71),
    sky = Color(0xFF2A4A56),
    quickCard = Color(0xFF030504),
    quickCardText = Color(0xFFF5F8F6),
)

val LocalAtomPalette = staticCompositionLocalOf { LightAtomPalette }

@Composable
fun AtomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val atomColors = if (darkTheme) DarkAtomPalette else LightAtomPalette
    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = atomColors.mint,
            onPrimary = Color(0xFF082017),
            primaryContainer = atomColors.mintPale,
            onPrimaryContainer = atomColors.ink,
            secondary = atomColors.coral,
            background = atomColors.canvas,
            onBackground = atomColors.ink,
            surface = atomColors.surface,
            onSurface = atomColors.ink,
            onSurfaceVariant = atomColors.muted,
            outline = atomColors.line,
        )
    } else {
        lightColorScheme(
            primary = atomColors.mintDark,
            onPrimary = Color.White,
            primaryContainer = atomColors.mintPale,
            onPrimaryContainer = atomColors.ink,
            secondary = atomColors.coral,
            background = atomColors.canvas,
            onBackground = atomColors.ink,
            surface = atomColors.surface,
            onSurface = atomColors.ink,
            onSurfaceVariant = atomColors.muted,
            outline = atomColors.line,
        )
    }

    val typeface = FontFamily.SansSerif
    val typography = MaterialTheme.typography.copy(
        displaySmall = TextStyle(
            fontFamily = typeface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            letterSpacing = (-1.2).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = typeface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            lineHeight = 31.sp,
            letterSpacing = (-0.7).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = typeface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 25.sp,
            letterSpacing = (-0.3).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = typeface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 21.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = typeface,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 23.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = typeface,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = typeface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = typeface,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp,
        ),
    )

    androidx.compose.runtime.CompositionLocalProvider(LocalAtomPalette provides atomColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = typography,
            content = content,
        )
    }
}
