package com.wyrmwhelp.idlehoard.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A small carved-wood plaque button — cut corners (matching the angled
 * corners on `FloatingMenu`'s wooden signs), a wood-tone gradient with a
 * thin gold bevel highlight along the top edge, and engraved-looking label
 * text. Shared by `GameHeader`'s buy-quantity selector and `LairCard`'s
 * Claim button so the two read as the same material instead of two
 * different button styles.
 *
 * Not a Material component, so `enabled = false` fades the whole thing
 * itself (wood, border, bevel line, text alpha) rather than relying on a
 * disabled treatment Material would otherwise provide for free.
 *
 * `contentPadding` and `modifier` are separate on purpose, mirroring
 * Material's own `Button` API: a caller that needs a *fixed* size (the
 * header's selector, so cycling between "x1" and "x100" doesn't reflow the
 * header) sets `modifier = Modifier.size(...)` and `contentPadding =
 * PaddingValues(0.dp)`; a caller that wants the button to size to its text
 * (`LairCard`'s Claim button) just uses the default padding and leaves
 * sizing to content.
 */
@Composable
fun WoodenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: FantasyPalette = FantasyPalette.Default,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
) {
    val shape = CutCornerShape(6.dp)
    val alpha = if (enabled) 1f else 0.45f
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(colors.woodLight, colors.woodMid, colors.woodDark).map { it.copy(alpha = alpha) },
                ),
            )
            .drawBehind {
                drawLine(
                    color = colors.goldBright.copy(alpha = 0.3f * alpha),
                    start = Offset(4f, 2f),
                    end = Offset(size.width - 4f, 2f),
                    strokeWidth = 1.5f,
                )
            }
            .border(1.dp, colors.woodDark.copy(alpha = alpha), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = colors.parchment.copy(alpha = alpha),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge.copy(
                shadow = Shadow(colors.woodDark.copy(alpha = alpha), Offset(1f, 1f), blurRadius = 0.5f),
            ),
        )
    }
}
