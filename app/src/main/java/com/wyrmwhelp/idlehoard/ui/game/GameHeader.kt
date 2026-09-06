package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.GlowingGoldText
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * Plain data bag for what [GameHeader] displays — keeps the composable's
 * parameter list to one bundle instead of four loose primitives, and keeps
 * [GameHeader] itself free of any `GameViewModel` reference (the caller
 * collects the ViewModel's flows and assembles this).
 */
data class GameHeaderState(
    val goldPieces: Double,
    val goldPerSecond: Double,
    val platinumPieces: Double,
    val gems: Long,
    val buyQuantity: BuyQuantity,
)

/**
 * The game screen's top bar, styled to match the rest of the app's cozy-
 * fantasy chrome (wooden signs, parchment, carved edges) instead of a plain
 * Material title bar. Three sections, left to right:
 * - [MedallionEmblem]: a carved gold-ringed medallion standing in for the
 *   not-yet-built avatar system ("a handful of pre-created avatar images
 *   they can choose from").
 * - Total Gold Pieces (glowing/embossed, [GlowingGoldText]) over a thin
 *   [ParchmentStrip] showing gold-per-second, Platinum Pieces (labeled
 *   "pp" — "Premium Coins" in the original ask, but kept the existing
 *   5E-flavored `platinumPieces` name rather than add a second label for the
 *   same currency), and Gems (the Level Up prestige currency — see
 *   `domain/model/LevelUp.kt`).
 * - `WoodenButton` (`ui/common/`): the bulk-purchase quantity selector,
 *   shared with `LairCard`'s Claim button.
 *
 * The whole row sits on a [woodenBanner] background that spans full width —
 * including behind the status bar, since it's drawn before
 * `statusBarsPadding()` is applied — so the plank reads as one continuous
 * strip across the top of the screen rather than stopping at the status bar.
 *
 * Pure presentation: no `GameViewModel` reference, no side effects. The
 * caller collects state (`collectAsStateWithLifecycle`) and passes a plain
 * [GameHeaderState] plus a callback.
 */
@Composable
fun GameHeader(
    state: GameHeaderState,
    onCycleBuyQuantity: () -> Unit,
    modifier: Modifier = Modifier,
    colors: FantasyPalette = FantasyPalette.Default,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .woodenBanner(colors)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MedallionEmblem(colors = colors)

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Image(
                    painter = painterResource(R.drawable.coin),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                GlowingGoldText(text = "${GoldFormat.format(state.goldPieces)} gp", colors = colors)
            }
            ParchmentStrip(colors = colors, modifier = Modifier.padding(top = 3.dp)) {
                Text(
                    text = "${GoldFormat.format(state.goldPerSecond)} gp/sec",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink,
                )
                Text(
                    text = "${GoldFormat.format(state.platinumPieces)} pp",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink,
                )
                Text(
                    text = "${GoldFormat.format(state.gems.toDouble())} gems",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.gemDeep,
                )
            }
        }

        WoodenButton(
            text = state.buyQuantity.label,
            onClick = onCycleBuyQuantity,
            modifier = Modifier.size(width = 56.dp, height = 40.dp),
            colors = colors,
            contentPadding = PaddingValues(0.dp),
        )
    }
}

/**
 * A carved plank background: a vertical wood-tone gradient plus faint
 * horizontal grain streaks and a two-tone (shadow + gold highlight) carved
 * line along the bottom edge. Own `Modifier` extension rather than a
 * composable since it's pure drawing with no layout/state of its own.
 */
private fun Modifier.woodenBanner(colors: FantasyPalette): Modifier = drawBehind {
    drawRect(
        brush = Brush.verticalGradient(listOf(colors.woodLight, colors.woodMid, colors.woodDark)),
    )
    val grainLines = 16
    for (i in 0 until grainLines) {
        val y = size.height * (i + 0.5f) / grainLines
        val wobble = if (i % 2 == 0) 2f else -2f
        drawLine(
            color = colors.woodGrain.copy(alpha = 0.12f),
            start = Offset(0f, y),
            end = Offset(size.width, y + wobble),
            strokeWidth = 1.5f,
        )
    }
    drawLine(
        color = colors.woodDark,
        start = Offset(0f, size.height - 2f),
        end = Offset(size.width, size.height - 2f),
        strokeWidth = 2f,
    )
    drawLine(
        color = colors.goldDeep.copy(alpha = 0.35f),
        start = Offset(0f, size.height - 4f),
        end = Offset(size.width, size.height - 4f),
        strokeWidth = 1f,
    )
}

/**
 * The avatar stand-in: a small carved medallion — gold ring, embossed wood
 * disc, engraved shield silhouette — rather than a plain circle. `Canvas` is
 * used here (not a built-in shape) because the gold ring's metallic sheen
 * (sweep gradient) and the shield silhouette (custom [Path]) both need
 * direct drawing.
 */
@Composable
private fun MedallionEmblem(colors: FantasyPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(52.dp)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            brush = Brush.sweepGradient(
                listOf(colors.goldDeep, colors.goldBright, colors.goldDeep, colors.goldBright, colors.goldDeep),
                center = center,
            ),
            radius = radius - radius * 0.11f,
            center = center,
            style = Stroke(width = radius * 0.22f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.woodLight, colors.woodDark),
                center = center,
                radius = radius * 0.8f,
            ),
            radius = radius * 0.76f,
            center = center,
        )

        val shield = shieldPath(center, radius * 0.42f)
        drawPath(shield, color = colors.parchment.copy(alpha = 0.88f))
        drawPath(shield, color = colors.ink.copy(alpha = 0.65f), style = Stroke(width = 1.5f))
    }
}

/** A simple flat-shaded shield outline, centered at [center] with "radius" [r]. */
private fun shieldPath(center: Offset, r: Float): Path = Path().apply {
    moveTo(center.x, center.y - r)
    lineTo(center.x + r * 0.85f, center.y - r * 0.55f)
    lineTo(center.x + r * 0.85f, center.y + r * 0.15f)
    cubicTo(
        center.x + r * 0.85f, center.y + r * 0.75f,
        center.x + r * 0.4f, center.y + r * 1.05f,
        center.x, center.y + r * 1.15f,
    )
    cubicTo(
        center.x - r * 0.4f, center.y + r * 1.05f,
        center.x - r * 0.85f, center.y + r * 0.75f,
        center.x - r * 0.85f, center.y + r * 0.15f,
    )
    lineTo(center.x - r * 0.85f, center.y - r * 0.55f)
    close()
}

/** A thin strip of parchment the rate readouts sit on, distinct from the wood behind it. */
@Composable
private fun ParchmentStrip(
    colors: FantasyPalette,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.horizontalGradient(listOf(colors.parchmentShade, colors.parchment, colors.parchmentShade)))
            .border(1.dp, colors.woodDark.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

