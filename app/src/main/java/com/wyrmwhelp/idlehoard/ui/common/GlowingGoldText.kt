package com.wyrmwhelp.idlehoard.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A glowing/embossed gold amount, layered as two `Text`s in a `Box`: a dark,
 * slightly offset copy underneath (the "engraved" emboss) and the bright
 * gold copy on top with a wide soft-colored shadow standing in for a glow
 * (Compose's `TextStyle.shadow` only takes one shadow, so a glow *and* an
 * emboss needs two draws). `FontFamily.Serif` approximates "fantasy-style"
 * lettering without a bundled display font — swap in a real one here if/when
 * the game gets a custom font asset.
 *
 * Started as `GameHeader`'s own private composable for the header's gold
 * total; promoted here once `WelcomeBackDialog` needed the same look for its
 * offline-earnings amount. [glowBright]/[glowDeep] default to
 * [colors]' gold tones but can be overridden — `LevelUpContent` reuses this
 * same emboss-plus-glow look for Gems with [FantasyPalette.gemBright]/
 * [FantasyPalette.gemDeep] instead, rather than inventing a second "glowing
 * currency amount" composable for a different color.
 */
@Composable
fun GlowingGoldText(
    text: String,
    modifier: Modifier = Modifier,
    colors: FantasyPalette = FantasyPalette.Default,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    glowBright: Color = colors.goldBright,
    glowDeep: Color = colors.goldDeep,
) {
    val baseStyle = style.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.ExtraBold)
    Box(modifier = modifier) {
        Text(
            text = text,
            style = baseStyle.copy(
                color = colors.ink.copy(alpha = 0.6f),
                shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(1.5f, 2f), blurRadius = 1f),
            ),
            modifier = Modifier.offset(1.dp, 1.dp),
        )
        Text(
            text = text,
            style = baseStyle.copy(
                color = glowBright,
                shadow = Shadow(glowDeep.copy(alpha = 0.9f), Offset.Zero, blurRadius = 18f),
            ),
        )
    }
}
