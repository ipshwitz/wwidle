package com.wyrmwhelp.idlehoard.ui.common

import androidx.compose.ui.graphics.Color

/**
 * Wood/gold/parchment tones for the app's cozy-fantasy chrome, pulled into
 * one object so any UI painted this way (`GameHeader`, `LairCard`, ...) can
 * be retinted without touching drawing code. Not part of the app's Material
 * theme (`ui/theme/Color.kt`/`Theme.kt`) — those are still the untouched
 * default M3 template — this is a self-contained palette, themeable via a
 * `colors` param wherever it's used, that started as `GameHeader`'s own
 * private palette and got promoted here once `LairCard` needed the same
 * look.
 */
data class FantasyPalette(
    val woodLight: Color,
    val woodMid: Color,
    val woodDark: Color,
    val woodGrain: Color,
    val goldBright: Color,
    val goldDeep: Color,
    val parchment: Color,
    val parchmentShade: Color,
    val ink: Color,
) {
    companion object {
        val Default = FantasyPalette(
            woodLight = Color(0xFF8B5A2B),
            woodMid = Color(0xFF6B4226),
            woodDark = Color(0xFF3E2417),
            woodGrain = Color(0xFF2A160C),
            goldBright = Color(0xFFFFE082),
            goldDeep = Color(0xFFB8860B),
            parchment = Color(0xFFE8D9B5),
            parchmentShade = Color(0xFFC9B183),
            ink = Color(0xFF3B2A1A),
        )
    }
}
