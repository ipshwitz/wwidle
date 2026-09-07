package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette

/**
 * One row in the lair list: a circular [CreatureAvatar] on the left and the
 * [LairCard] on the right, as two separate containers sharing a `Row`
 * (rather than the avatar living inside the card) — tapping either one
 * starts the lair's production cycle. `Modifier.height(IntrinsicSize.Min)`
 * on the row plus `fillMaxHeight().aspectRatio(1f)` on the avatar makes the
 * avatar a perfect circle that matches the card's own (content-driven)
 * height automatically, no matching magic numbers between the two
 * composables.
 *
 * Owns the `coinBurstTrigger` counter (hoisted here, not local to
 * `LairCard`) so both the avatar and the card can fire the same
 * [CoinBurstOverlay] — but unlike the old tap-to-collect flow, tapping
 * doesn't bump it directly anymore. Gold collection (and the burst) now
 * happens when `GameEngine` actually finishes the cycle the tap started,
 * not at the moment of the tap itself — see `OwnedLair.completedLoads`,
 * which increments on the domain side each time that happens. This
 * composable just watches that counter via [LaunchedEffect] and bumps
 * [coinBurstTrigger] whenever it changes, so the burst always fires at
 * completion regardless of how long the load actually took.
 *
 * [progress] comes from `GameEngine.lairProgress` (via `GameViewModel`/
 * `GameScreen`) and is passed straight through to [LairCard] — see that
 * file's doc for why the fill fraction is computed engine-side now instead
 * of derived here from raw cycle-progress fields. [productionSeconds] is
 * this lair's current actual cycle time (`GameScreen` computes it the same
 * way it computes `goldPerSecond`) — also just passed straight through, for
 * `LairCard`'s "gp / cycle time" line.
 */
@Composable
fun LairRow(
    lair: CreatureLair,
    owned: OwnedLair,
    goldPieces: Double,
    buyQuantity: BuyQuantity,
    globalIncomeMultiplier: Double,
    progress: Float,
    productionSeconds: Double,
    onClaim: () -> Unit,
    onStartLoad: () -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
    profitBoostMultiplier: Double = 1.0,
    gemBonusMultiplier: Double = 1.0,
    upgradeProfitMultiplier: Double = 1.0,
) {
    var coinBurstTrigger by remember { mutableIntStateOf(0) }
    var lastSeenCompletedLoads by remember { mutableIntStateOf(owned.completedLoads) }
    LaunchedEffect(owned.completedLoads) {
        if (owned.completedLoads != lastSeenCompletedLoads) {
            coinBurstTrigger++
            lastSeenCompletedLoads = owned.completedLoads
        }
    }

    // Tappable only when this lair is owned, has no Steward (which runs on
    // its own — tapping it does nothing), and isn't already mid-cycle.
    val canStartLoad = owned.count > 0 && !owned.hasSteward && !owned.isLoading
    // Full brightness once owned, *including* while Steward-managed — a
    // hired Steward means this lair is continuously earning on its own, not
    // idle, so it shouldn't read as dimmed/disabled the way "not tappable
    // right now" implies for the other two dim cases (unowned, mid-load).
    val isBright = owned.count > 0 && (owned.hasSteward || !owned.isLoading)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CreatureAvatar(
            lair = lair,
            enabled = canStartLoad,
            bright = isBright,
            onClick = onStartLoad,
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
            globalIncomeMultiplier = globalIncomeMultiplier,
            progress = progress,
            productionSeconds = productionSeconds,
            coinBurstTrigger = coinBurstTrigger,
            onClaim = onClaim,
            onStartLoad = onStartLoad,
            modifier = Modifier.weight(1f),
            palette = palette,
            profitBoostMultiplier = profitBoostMultiplier,
            gemBonusMultiplier = gemBonusMultiplier,
            upgradeProfitMultiplier = upgradeProfitMultiplier,
        )
    }
}

/**
 * The lair id → real portrait art mapping. Only a few lairs have art so far
 * (dropped into `/assets` as `lair-<monster>.png`, copied into
 * `drawable-nodpi/` as `lair_<lairId>.png` once verified as a genuinely
 * transparent square) — everything else still falls back to
 * [CreatureAvatar]'s placeholder disc until it gets its own art in the same
 * style. Keyed by lair id (not monster name) since a couple of tiers already
 * share a monster initial and could plausibly share art direction too.
 */
private fun lairPortraitRes(lairId: String): Int? = when (lairId) {
    "kobold_warren" -> R.drawable.lair_kobold_warren
    "giant_rat_burrow" -> R.drawable.lair_giant_rat_burrow
    "goblin_camp" -> R.drawable.lair_goblin_camp
    "orc_encampment" -> R.drawable.lair_orc_encampment
    "gnoll_den" -> R.drawable.lair_gnoll_den
    "bugbear_warcamp" -> R.drawable.lair_bugbear_warcamp
    else -> null
}

/**
 * A circular creature portrait — real art via [lairPortraitRes] where it
 * exists, otherwise a stand-in: a rarity-tinted radial gradient disc with a
 * carved border and the monster's first letter in serif type. The
 * placeholder isn't unique per monster (a few tiers share an initial) but
 * the rarity color band and the full name right next to it in `LairCard`
 * already disambiguate. Both variants dim identically whenever [bright] is
 * false — unowned, or owned but mid-cycle without a Steward — while a
 * Steward-managed lair stays fully opaque even though it's never tappable,
 * since it's continuously producing rather than idle (see `LairRow`'s
 * `isBright`). `enabled` is tap-ability only (mirrors `LairRow`'s
 * `canStartLoad`) and no longer drives opacity; `onClick` is the same
 * hoisted `onStartLoad` action.
 */
@Composable
private fun CreatureAvatar(
    lair: CreatureLair,
    enabled: Boolean,
    bright: Boolean,
    onClick: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    val alpha = if (bright) 1f else 0.55f
    val portraitRes = lairPortraitRes(lair.id)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (portraitRes != null) {
            // matchParentSize(), not fillMaxHeight()/fillMaxWidth(): the Row this
            // sits in uses Modifier.height(IntrinsicSize.Min), and an Image (unlike
            // the Canvas below) reports its painter's own intrinsic size during
            // that measurement pass — fillMaxWidth/fillMaxHeight let that leak
            // through and blew the whole row up to the portrait's raw size.
            // matchParentSize() sizes strictly off the already-resolved Box instead.
            Image(
                painter = painterResource(portraitRes),
                contentDescription = lair.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(alpha),
            )
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = palette.woodDark.copy(alpha = alpha),
                    radius = size.minDimension / 2f - 1.5f,
                    style = Stroke(width = 3f),
                )
            }
        } else {
            val rarity = rarityColor(lair.tier)
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
}
