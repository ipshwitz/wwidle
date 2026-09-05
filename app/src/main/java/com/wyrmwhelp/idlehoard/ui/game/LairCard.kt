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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.domain.engine.GameEngine
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat

private val buttonContentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)

@Composable
fun LairCard(
    lair: CreatureLair,
    owned: OwnedLair,
    goldPieces: Double,
    buyQuantity: BuyQuantity,
    globalMultiplier: Double,
    onClaim: () -> Unit,
    onHireSteward: () -> Unit,
    onPlunder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // coerceAtLeast(1): MAX resolves to 0 when even one more unit isn't
    // affordable — falling back to a 1-unit preview keeps the button showing
    // a real cost (and staying correctly disabled) instead of a "x0" label.
    val claimQuantity = buyQuantity.resolve(lair, owned.count, goldPieces).coerceAtLeast(1)
    val claimCost = lair.costForUnits(owned.count, claimQuantity)
    val canClaim = goldPieces >= claimCost
    val canHireSteward = owned.count > 0 && !owned.hasSteward && goldPieces >= lair.stewardCostGp
    val progress = if (lair.baseProductionSeconds <= 0.0) {
        0f
    } else {
        (owned.cycleProgressSeconds / lair.baseProductionSeconds).toFloat().coerceIn(0f, 1f)
    }
    val fillFraction = if (owned.isReadyToCollect) 1f else progress
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
    // Bumped on every manual plunder tap (not on a Steward's automatic
    // collection, which never runs through this click handler) to fire a
    // fresh CoinBurstOverlay — see that file for why it's a counter, not a
    // boolean.
    var coinBurstTrigger by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(rarity.copy(alpha = if (owned.count > 0) 0.35f else 0.20f))
            .border(1.dp, rarity.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable(
                enabled = owned.isReadyToCollect,
                onClick = {
                    coinBurstTrigger++
                    onPlunder()
                },
            ),
    ) {
        if (owned.count > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedFillFraction)
                    .background(rarity.copy(alpha = 0.55f)),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = lair.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(text = "Owned: ${owned.count}", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${lair.monster} • CR ${lair.challengeRating}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (owned.count > 0) {
                    Text(
                        text = "${GoldFormat.format(lair.incomePerCycle(owned.count, globalMultiplier))} gp/cycle",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = onClaim,
                    enabled = canClaim,
                    contentPadding = buttonContentPadding,
                ) {
                    Text(
                        text = if (owned.count == 0) {
                            val prefix = if (claimQuantity == 1) "Claim" else "Claim x$claimQuantity"
                            "$prefix — ${GoldFormat.format(claimCost)} gp"
                        } else {
                            "+$claimQuantity — ${GoldFormat.format(claimCost)} gp"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (owned.count > 0 && !owned.hasSteward) {
                    OutlinedButton(
                        onClick = onHireSteward,
                        enabled = canHireSteward,
                        contentPadding = buttonContentPadding,
                    ) {
                        Text(
                            text = "Steward — ${GoldFormat.format(lair.stewardCostGp)} gp",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
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
private fun rarityColor(tier: Int): Color = when {
    tier <= 2 -> Color(0xFF4CAF50)
    tier <= 5 -> Color(0xFF2196F3)
    tier <= 8 -> Color(0xFF9C27B0)
    tier <= 11 -> Color(0xFFFF9800)
    else -> Color(0xFFFFC107)
}
