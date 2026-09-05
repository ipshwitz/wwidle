package com.wyrmwhelp.idlehoard.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.domain.engine.GameEngine
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

/**
 * One lair in the list. Styled to match the app's cozy-fantasy chrome
 * ([FantasyPalette] — parchment/wood/gold tones, the same palette
 * `GameHeader` uses) instead of a flat Material-colored block. Still no
 * Material `Card` — a custom `Box` sized via `Modifier.height(IntrinsicSize.Min)`
 * so the whole card keeps doubling as its own progress bar (see the fill
 * layer below), just painted richer now:
 * - A translucent parchment gradient base (`palette.parchmentShade` →
 *   `palette.parchment`, alpha 0.55) instead of a flat rarity wash, so an
 *   *unclaimed* lair still reads as a card rather than a near-invisible
 *   tinted rectangle — but stays sheer enough to keep `GameScreen`'s
 *   background art showing through, same as the card always has.
 * - A faint rarity tint over the whole card, then a stronger rarity-gradient
 *   fill for the claimed fraction (`cycleProgressSeconds / effectiveProductionSeconds`,
 *   100% = the cycle a tap started is about to complete and auto-collect)
 *   with a bright "leading edge" line marking exactly how far the fill has
 *   come — drawn via `drawBehind` on the fill `Box` itself at its own right
 *   edge, so it always tracks the animated fraction without any extra
 *   position math.
 * - `FontFamily.Serif` for the name (matching `GameHeader`'s lettering) with
 *   a subtle emboss shadow.
 * - The Claim action is a shared `WoodenButton` instead of a Material
 *   `Button`.
 *
 * The Steward button that used to sit next to Claim is gone — hiring a
 * Steward now lives solely in the Stewards menu section (not built yet), not
 * on every card. `onHireSteward`/`hasSteward` are no longer read here.
 *
 * [coinBurstTrigger] is hoisted up to `LairRow` (not local `remember` state
 * here) so the creature avatar next to this card — a separate container —
 * can fire the same burst as tapping the card itself; both go through the
 * same counter, and `LairRow` is what actually decides when to bump it now
 * (on `OwnedLair.completedLoads` changing, not on the tap — see that file).
 */
@Composable
fun LairCard(
    lair: CreatureLair,
    owned: OwnedLair,
    goldPieces: Double,
    buyQuantity: BuyQuantity,
    globalMultiplier: Double,
    coinBurstTrigger: Int,
    onClaim: () -> Unit,
    onStartLoad: () -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
    speedBoostMultiplier: Double = 1.0,
    profitBoostMultiplier: Double = 1.0,
) {
    // coerceAtLeast(1): MAX resolves to 0 when even one more unit isn't
    // affordable — falling back to a 1-unit preview keeps the button showing
    // a real cost (and staying correctly disabled) instead of a "x0" label.
    val claimQuantity = buyQuantity.resolve(lair, owned.count, goldPieces).coerceAtLeast(1)
    val claimCost = lair.costForUnits(owned.count, claimQuantity)
    val canClaim = goldPieces >= claimCost
    val productionSeconds = lair.effectiveProductionSeconds(speedBoostMultiplier)
    // Naturally reads 0% while idle (cycleProgressSeconds stays pinned at 0
    // until a tap starts the cycle — see GameEngine.advanceLair) with no
    // special-casing needed: there's no separate "ready, waiting" plateau
    // anymore, since completion auto-collects and resets in the same step.
    val fillFraction = if (productionSeconds <= 0.0) {
        0f
    } else {
        (owned.cycleProgressSeconds / productionSeconds).toFloat().coerceIn(0f, 1f)
    }
    // GameEngine only pushes a new fillFraction every TICK_INTERVAL_MS, which
    // would otherwise render as visible steps — animating linearly across
    // that same window turns it back into continuous motion.
    val animatedFillFraction by animateFloatAsState(
        targetValue = fillFraction,
        animationSpec = tween(
            durationMillis = GameEngine.TICK_INTERVAL_MS.toInt(),
            easing = LinearEasing,
        ),
        label = "lairFill",
    )
    val rarity = rarityColor(lair.tier)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(palette.parchmentShade.copy(alpha = 0.55f), palette.parchment.copy(alpha = 0.55f)),
                ),
            )
            .background(rarity.copy(alpha = if (owned.count > 0) 0.14f else 0.08f))
            .border(1.5.dp, rarity.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .clickable(
                enabled = owned.count > 0 && !owned.hasSteward && !owned.isLoading,
                onClick = onStartLoad,
            ),
    ) {
        if (owned.count > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedFillFraction)
                    .background(Brush.horizontalGradient(listOf(rarity.copy(alpha = 0.22f), rarity.copy(alpha = 0.48f))))
                    .drawBehind {
                        if (animatedFillFraction in 0.001f..0.999f) {
                            drawLine(
                                color = rarity,
                                start = Offset(size.width - 1f, 0f),
                                end = Offset(size.width - 1f, size.height),
                                strokeWidth = 2f,
                            )
                        }
                    },
            )
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = lair.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        color = palette.ink,
                        shadow = Shadow(Color.Black.copy(alpha = 0.2f), Offset(0.5f, 0.5f), blurRadius = 0.5f),
                    ),
                )
                Text(
                    text = "Owned: ${owned.count}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.75f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${lair.monster} • CR ${lair.challengeRating}",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = palette.ink.copy(alpha = 0.65f),
                )
                if (owned.count > 0) {
                    Text(
                        text = "${GoldFormat.format(lair.incomePerCycle(owned.count, globalMultiplier, profitBoostMultiplier))} gp/cycle",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.goldDeep,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(modifier = Modifier.padding(top = 6.dp)) {
                WoodenButton(
                    text = if (owned.count == 0) {
                        val prefix = if (claimQuantity == 1) "Claim" else "Claim x$claimQuantity"
                        "$prefix — ${GoldFormat.format(claimCost)} gp"
                    } else {
                        "+$claimQuantity — ${GoldFormat.format(claimCost)} gp"
                    },
                    onClick = onClaim,
                    enabled = canClaim,
                    colors = palette,
                )
            }
        }

        CoinBurstOverlay(trigger = coinBurstTrigger, modifier = Modifier.matchParentSize())
    }
}

/**
 * A five-band "rarity" color ramp across the catalog's tiers (green → blue →
 * purple → orange → gold), so the lair list reads as a visible power curve at
 * a glance instead of a wall of identical cards.
 */
internal fun rarityColor(tier: Int): Color = when {
    tier <= 2 -> Color(0xFF4CAF50)
    tier <= 5 -> Color(0xFF2196F3)
    tier <= 8 -> Color(0xFF9C27B0)
    tier <= 11 -> Color(0xFFFF9800)
    else -> Color(0xFFFFC107)
}
