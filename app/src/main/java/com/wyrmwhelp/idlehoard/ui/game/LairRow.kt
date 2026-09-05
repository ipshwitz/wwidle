package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette

/**
 * One row in the lair list: a circular [CreatureAvatar] on the left and the
 * [LairCard] on the right, as two separate containers sharing a `Row`
 * (rather than the avatar living inside the card) — tapping either one
 * plunders the lair. `Modifier.height(IntrinsicSize.Min)` on the row plus
 * `fillMaxHeight().aspectRatio(1f)` on the avatar makes the avatar a perfect
 * circle that matches the card's own (content-driven) height automatically,
 * no matching magic numbers between the two composables.
 *
 * Owns the `coinBurstTrigger` counter (hoisted here, not local to `LairCard`)
 * so both the avatar and the card can fire the same [CoinBurstOverlay] via
 * one shared `plunder` action.
 */
@Composable
fun LairRow(
    lair: CreatureLair,
    owned: OwnedLair,
    goldPieces: Double,
    buyQuantity: BuyQuantity,
    globalMultiplier: Double,
    onClaim: () -> Unit,
    onPlunder: () -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
    speedBoostMultiplier: Double = 1.0,
    profitBoostMultiplier: Double = 1.0,
) {
    // Bumped on every manual plunder tap (not on a Steward's automatic
    // collection, which never runs through either click handler below) to
    // fire a fresh CoinBurstOverlay on the card — see that file for why it's
    // a counter, not a boolean.
    var coinBurstTrigger by remember { mutableIntStateOf(0) }
    val plunder: () -> Unit = {
        coinBurstTrigger++
        onPlunder()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CreatureAvatar(
            lair = lair,
            enabled = owned.isReadyToCollect,
            onClick = plunder,
            palette = palette,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
        )
        LairCard(
            lair = lair,
            owned = owned,
            goldPieces = goldPieces,
            buyQuantity = buyQuantity,
            globalMultiplier = globalMultiplier,
            coinBurstTrigger = coinBurstTrigger,
            onClaim = onClaim,
            onPlunder = plunder,
            modifier = Modifier.weight(1f),
            palette = palette,
            speedBoostMultiplier = speedBoostMultiplier,
            profitBoostMultiplier = profitBoostMultiplier,
        )
    }
}

/**
 * A circular stand-in for real creature art (none exists yet — no monster
 * portraits have been dropped into `/assets`): a rarity-tinted radial
 * gradient disc with a carved border, and the monster's first letter in
 * serif type. Not unique per monster (a few tiers share an initial) but the
 * rarity color band and the full name right next to it in `LairCard` already
 * disambiguate — this is a placeholder, not a real icon system. Dims to the
 * same "waiting" alpha as everything else on an unready lair, and shares the
 * exact tap target contract as the card: `enabled` mirrors
 * `owned.isReadyToCollect`, `onClick` is the same hoisted `plunder` action
 * from `LairRow`.
 */
@Composable
private fun CreatureAvatar(
    lair: CreatureLair,
    enabled: Boolean,
    onClick: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    val rarity = rarityColor(lair.tier)
    val alpha = if (enabled) 1f else 0.55f

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(rarity.copy(alpha = alpha), rarity.copy(alpha = alpha * 0.6f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
            drawCircle(
                color = palette.woodDark.copy(alpha = alpha),
                radius = radius - 1.5f,
                center = center,
                style = Stroke(width = 3f),
            )
        }
        Text(
            text = lair.monster.take(1).uppercase(),
            color = palette.parchment.copy(alpha = alpha),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                shadow = Shadow(palette.woodDark.copy(alpha = alpha), Offset(1f, 1f), blurRadius = 1f),
            ),
        )
    }
}
