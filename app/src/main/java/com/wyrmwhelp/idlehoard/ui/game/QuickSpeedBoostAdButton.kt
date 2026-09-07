package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wyrmwhelp.idlehoard.R
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
 * Uses the real `media_play.png` art (v0.32.0, replacing an earlier
 * hand-drawn Canvas medallion+triangle) — per explicit request, sized and
 * bottom-aligned to exactly match `FloatingMenu`'s chest toggle (a 72.dp
 * touch target around a 64.dp image, 24.dp up from the screen bottom) so
 * the two read as a matched pair of round icon buttons rather than one
 * looking randomly bigger/higher than the other; `GameScreen` is what
 * applies that matching bottom padding via this composable's [modifier].
 *
 * [availableSlots] (`GameState.availableSpeedBoostAdSlots()`, computed by
 * the caller the same way `MainActivity` already does for `ShopContent`)
 * drives a small gold [SlotBadge] in the button's corner — hidden once all
 * four slots are on cooldown, at which point the play icon itself just dims
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

        Box(
            modifier = Modifier.size(72.dp).clickable(onClick = onWatchAd),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.TopEnd) {
                Image(
                    painter = painterResource(R.drawable.media_play),
                    contentDescription = "Watch ad for Speed boost",
                    modifier = Modifier
                        .size(64.dp)
                        .alpha(if (availableSlots > 0) 1f else 0.55f),
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
