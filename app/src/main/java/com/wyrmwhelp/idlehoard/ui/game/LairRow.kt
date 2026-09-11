package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.FEATURED_LAIR_TAPS_REQUIRED
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
 *
 * **Featured Lair mini-event (v0.49.0)** — [isFeatured] (true only while
 * `GameState.featuredLairId` names this exact lair — see
 * `domain/model/FeaturedLairEvent.kt`) overrides the tap target entirely:
 * both the avatar and the card become tappable regardless of Steward/load
 * state, and every tap calls [onTapFeatured] (`GameViewModel.tapFeaturedLair`)
 * instead of [onStartLoad] — a manual-only bonus layered on top of
 * whatever's already happening with this lair, never a substitute for the
 * normal tap-to-start flow. [onTapFeatured] returns whether *this* tap
 * cleared the goal, reusing the same `coinBurstTrigger` the completed-load
 * effect already fires (a bigger, dedicated "coins explode all over the
 * screen" effect wasn't built — this app's stated art style is "reuse
 * Canvas effects, no sprite pack," and the existing burst already reads as
 * a celebration) — plus a haptic buzz on every tap, this app's first use
 * of device haptics (via `LocalHapticFeedback`, which needs no `VIBRATE`
 * manifest permission, unlike a raw `Vibrator`/`VibrationEffect` call).
 * [featuredTapCount] drives [FeaturedLairProgressBar], shown below the row
 * (not inside `LairCard`, which has an established fixed-height/
 * `IntrinsicSize` layout too fragile to add a persistent new element to —
 * see that file's own gotcha notes) only while [isFeatured] is true.
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
    achievementBonusMultiplier: Double = 1.0,
    hasUniversalSteward: Boolean = false,
    isFeatured: Boolean = false,
    featuredTapCount: Int = 0,
    onTapFeatured: () -> Boolean = { false },
) {
    var coinBurstTrigger by remember { mutableIntStateOf(0) }
    var lastSeenCompletedLoads by remember { mutableIntStateOf(owned.completedLoads) }
    LaunchedEffect(owned.completedLoads) {
        if (owned.completedLoads != lastSeenCompletedLoads) {
            coinBurstTrigger++
            lastSeenCompletedLoads = owned.completedLoads
        }
    }

    val haptics = LocalHapticFeedback.current
    val handleTap: () -> Unit = {
        if (isFeatured) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (onTapFeatured()) coinBurstTrigger++
        } else {
            onStartLoad()
        }
    }

    // Managed by either a real per-lair Steward or the account-wide
    // Universal Steward (`domain/model/UniversalSteward.kt`) once at least
    // one unit is owned — either way it runs continuously on its own.
    val isManaged = owned.count > 0 && (owned.hasSteward || hasUniversalSteward)
    // Tappable when this lair is owned and isn't already mid-cycle, unless
    // it's Featured right now — a Featured lair is always tappable
    // (regardless of Steward/load state), since the tap challenge is a
    // separate action from the normal gold-collection tap.
    val canStartLoad = owned.count > 0 && !isManaged && !owned.isLoading
    val canTap = isFeatured || canStartLoad
    // Full brightness once owned, *including* while managed — an
    // auto-collecting lair is continuously earning on its own, not idle, so
    // it shouldn't read as dimmed/disabled the way "not tappable right now"
    // implies for the other two dim cases (unowned, mid-load). A Featured
    // lair is always bright too, so the flashing border reads clearly.
    val isBright = owned.count > 0 && (isManaged || !owned.isLoading || isFeatured)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CreatureAvatar(
                lair = lair,
                enabled = canTap,
                bright = isBright,
                onClick = handleTap,
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
                onStartLoad = handleTap,
                modifier = Modifier.weight(1f),
                palette = palette,
                profitBoostMultiplier = profitBoostMultiplier,
                gemBonusMultiplier = gemBonusMultiplier,
                upgradeProfitMultiplier = upgradeProfitMultiplier,
                achievementBonusMultiplier = achievementBonusMultiplier,
                isManaged = isManaged,
                isFeatured = isFeatured,
            )
        }
        if (isFeatured) {
            FeaturedLairProgressBar(
                tapCount = featuredTapCount,
                palette = palette,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

/**
 * The Featured Lair tap challenge's own progress bar — "how many of the
 * [FEATURED_LAIR_TAPS_REQUIRED] taps have landed," shown below the whole
 * [LairRow] (not inside `LairCard`'s own production progress bar, which is
 * a completely different number — see `domain/model/FeaturedLairEvent.kt`).
 */
@Composable
private fun FeaturedLairProgressBar(tapCount: Int, palette: FantasyPalette, modifier: Modifier = Modifier) {
    val fraction = (tapCount.toFloat() / FEATURED_LAIR_TAPS_REQUIRED).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "featuredTapProgress")
    Column(modifier = modifier) {
        Text(
            text = "Tap! $tapCount / $FEATURED_LAIR_TAPS_REQUIRED",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(palette.woodDark.copy(alpha = 0.35f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedFraction)
                    .background(Brush.horizontalGradient(listOf(palette.goldBright, palette.goldDeep))),
            )
        }
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
    "hobgoblin_barracks" -> R.drawable.lair_hobgoblin_barracks
    "ogres_cave" -> R.drawable.lair_ogres_cave
    "owlbear_roost" -> R.drawable.lair_owlbear_roost
    "troll_warren" -> R.drawable.lair_troll_warren
    "wyvern_aerie" -> R.drawable.lair_wyvern_aerie
    "young_dragons_lair" -> R.drawable.lair_young_dragons_lair
    "adult_dragons_lair" -> R.drawable.lair_adult_dragons_lair
    "ancient_dragons_hoard" -> R.drawable.lair_ancient_dragons_hoard
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
