package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette

/**
 * A persistent quick-access "watch an ad for a Speed boost" control fixed in
 * the main game screen's corner (`GameScreen`, bottom-end) — the exact same
 * ad reward as the Shop's Temporary tab "Earn a Free Boost" row
 * (`ui/shop/ShopContent.kt`, `RewardedPlacement.SHOP_SPEED_BOOST`), just a
 * second door into it. Added per explicit request: players shouldn't have
 * to open the Shop menu to find the ad-watch reward — it should be visible
 * without "hunting" for it. Both entry points call the exact same
 * `GameViewModel.watchAdForSpeedBoost`/`speedBoostAdMessage`/
 * `dismissSpeedBoostAdMessage` — there's no separate state for this button,
 * and watching from here counts against the same four daily slots as
 * watching from the Shop.
 *
 * [availableSlots] (`GameState.availableSpeedBoostAdSlots()`, computed by
 * the caller the same way `MainActivity` already does for `ShopContent`)
 * drives a small gold [SlotBadge] in the button's corner — hidden once all
 * four slots are on cooldown, at which point [AdMedallion] itself just dims
 * rather than disappearing entirely; the button stays tappable either way
 * so a tap while on cooldown still surfaces the "come back in Xh Ym"
 * [message] instead of silently doing nothing.
 */
@Composable
fun QuickSpeedBoostAdButton(
    availableSlots: Int,
    message: String?,
    onWatchAd: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    colors: FantasyPalette = FantasyPalette.Default,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        message?.let {
            MessageBubble(
                text = it,
                onDismiss = onDismissMessage,
                colors = colors,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Box(contentAlignment = Alignment.TopEnd) {
            AdMedallion(
                available = availableSlots > 0,
                colors = colors,
                modifier = Modifier
                    .size(56.dp)
                    .clickable(onClick = onWatchAd),
            )
            if (availableSlots > 0) {
                SlotBadge(
                    count = availableSlots,
                    colors = colors,
                    modifier = Modifier.offset(x = 4.dp, y = (-4).dp),
                )
            }
        }
    }
}

/**
 * A carved gold-ringed medallion — same Canvas-drawn sweep-gradient ring and
 * embossed wood disc as `GameHeader`'s `MedallionEmblem` — with a hand-drawn
 * play-triangle glyph standing in for a "watch video" icon, since there's no
 * ad-specific art asset and the project's style is Canvas drawing rather
 * than a Material icon glyph or a new sprite for this.
 */
@Composable
private fun AdMedallion(available: Boolean, colors: FantasyPalette, modifier: Modifier = Modifier) {
    val alpha = if (available) 1f else 0.55f
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            brush = Brush.sweepGradient(
                listOf(colors.goldDeep, colors.goldBright, colors.goldDeep, colors.goldBright, colors.goldDeep),
                center = center,
            ),
            radius = radius - radius * 0.1f,
            center = center,
            alpha = alpha,
            style = Stroke(width = radius * 0.2f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.woodLight, colors.woodDark),
                center = center,
                radius = radius * 0.82f,
            ),
            radius = radius * 0.78f,
            center = center,
            alpha = alpha,
        )

        val triangle = playTrianglePath(center, radius * 0.36f)
        drawPath(triangle, color = colors.parchment.copy(alpha = 0.92f * alpha))
    }
}

/** A simple right-pointing play triangle, centered at [center] with "radius" [r]. */
private fun playTrianglePath(center: Offset, r: Float): Path = Path().apply {
    moveTo(center.x - r * 0.6f, center.y - r)
    lineTo(center.x + r, center.y)
    lineTo(center.x - r * 0.6f, center.y + r)
    close()
}

/** The small "N slots left" badge overlapping the medallion's rim. */
@Composable
private fun SlotBadge(count: Int, colors: FantasyPalette, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(colors.goldBright)
            .border(1.dp, colors.woodDark, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = count.toString(), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

/**
 * A small dismissible parchment toast for [GameViewModel.speedBoostAdMessage]
 * — same "✕" dismiss affordance as `ShopContent`'s `PlatinumAdMessageCard`,
 * duplicated here rather than shared since it's a small, differently-shaped
 * helper (a floating bubble, not a list row), matching this project's
 * established per-file-duplication convention for small UI helpers.
 */
@Composable
private fun MessageBubble(text: String, onDismiss: () -> Unit, colors: FantasyPalette, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .widthIn(max = 220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(listOf(colors.parchmentShade, colors.parchment)))
            .border(1.dp, colors.woodDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.ink,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = "✕",
            color = colors.ink.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onDismiss),
        )
    }
}
