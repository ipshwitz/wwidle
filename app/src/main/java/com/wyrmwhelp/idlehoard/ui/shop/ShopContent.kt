package com.wyrmwhelp.idlehoard.ui.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.R
import com.wyrmwhelp.idlehoard.domain.model.PLATINUM_AD_REWARD_PP
import com.wyrmwhelp.idlehoard.domain.model.TIME_SKIP_COST_PP
import com.wyrmwhelp.idlehoard.domain.model.TIME_SKIP_SECONDS
import com.wyrmwhelp.idlehoard.domain.model.profitBoostCost
import com.wyrmwhelp.idlehoard.domain.model.profitBoostMultiplier
import com.wyrmwhelp.idlehoard.domain.model.speedBoostCost
import com.wyrmwhelp.idlehoard.domain.model.speedBoostMultiplier
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.DurationFormat
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat
import java.time.Duration

/**
 * The "Shop" section's real content: the player's current Platinum Pieces
 * balance, the permanent Boosts Platinum actually buys (Speed, Profit, Time
 * Skip — see `domain/model/Boosts.kt`), then the two ways to earn more
 * Platinum. "Watch an Ad" (see [platinumAdCooldownRemaining]/[onWatchAd])
 * is open to guests too — it has no monetary value, so a guest losing it on
 * reinstall isn't a real loss the way losing an IAP receipt would be.
 * "Buy Platinum Pieces" (IAP, still a disabled `WoodenButton` with a
 * "Coming soon" note since billing isn't wired up yet) stays gated to
 * signed-in players — see [isSignedIn] — since that *is* real money, which
 * should stay tied to a recoverable account. Pure display plus callbacks —
 * takes state passed in by `MainActivity`'s `WyrmWhelpApp` (which already
 * holds the `GameViewModel` reference) rather than taking a ViewModel
 * itself.
 */
@Composable
fun ShopContent(
    platinumPieces: Double,
    speedBoostLevel: Int,
    profitBoostLevel: Int,
    isSignedIn: Boolean,
    platinumAdCooldownRemaining: Duration,
    platinumAdMessage: String?,
    onBuySpeedBoost: () -> Unit,
    onBuyProfitBoost: () -> Unit,
    onBuyTimeSkip: () -> Unit,
    onWatchAd: () -> Unit,
    onDismissPlatinumAdMessage: () -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { BalanceCard(platinumPieces = platinumPieces, palette = palette) }
        item { SectionLabel(text = "Boosts", palette = palette) }
        item {
            BoostRow(
                title = "Speed Boost",
                description = "Level ${speedBoostLevel} — every lair's cycle is " +
                    "${GoldFormat.format((speedBoostMultiplier(speedBoostLevel) - 1.0) * 100.0)}% faster.",
                cost = speedBoostCost(speedBoostLevel),
                canAfford = platinumPieces >= speedBoostCost(speedBoostLevel),
                onBuy = onBuySpeedBoost,
                palette = palette,
            )
        }
        item {
            BoostRow(
                title = "Profit Boost",
                description = "Level ${profitBoostLevel} — every lair earns " +
                    "${GoldFormat.format((profitBoostMultiplier(profitBoostLevel) - 1.0) * 100.0)}% more gold.",
                cost = profitBoostCost(profitBoostLevel),
                canAfford = platinumPieces >= profitBoostCost(profitBoostLevel),
                onBuy = onBuyProfitBoost,
                palette = palette,
            )
        }
        item {
            BoostRow(
                title = "Time Skip",
                description = "Instantly grants ${(TIME_SKIP_SECONDS / 3600.0).let { GoldFormat.format(it) }} " +
                    "hour(s) of production from every owned lair.",
                cost = TIME_SKIP_COST_PP,
                canAfford = platinumPieces >= TIME_SKIP_COST_PP,
                onBuy = onBuyTimeSkip,
                palette = palette,
            )
        }
        item { SectionLabel(text = "Earn Platinum", palette = palette) }
        item {
            WatchAdRow(
                cooldownRemaining = platinumAdCooldownRemaining,
                onWatchAd = onWatchAd,
                palette = palette,
            )
        }
        platinumAdMessage?.let { message ->
            item {
                PlatinumAdMessageCard(
                    message = message,
                    onDismiss = onDismissPlatinumAdMessage,
                    palette = palette,
                )
            }
        }
        if (isSignedIn) {
            item {
                EarnMethodRow(
                    title = "Buy Platinum Pieces",
                    description = "Purchase Platinum Pieces with real money.",
                    palette = palette,
                )
            }
        } else {
            item {
                ParchmentCard(palette = palette) {
                    Text(
                        text = "Sign in under Settings to buy Platinum Pieces with real money. This keeps " +
                            "that purchase tied to an account you can recover, not a guest identity " +
                            "that's lost on reinstall.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.ink.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, palette: FantasyPalette, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 4.dp),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Serif, color = palette.ink),
    )
}

/** A translucent parchment card matching `LairCard`/`StewardsContent`/`UnlocksContent`'s base treatment. */
@Composable
private fun ParchmentCard(
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
    borderColor: Color = palette.woodDark.copy(alpha = 0.5f),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(palette.parchmentShade.copy(alpha = 0.8f), palette.parchment.copy(alpha = 0.8f)),
                ),
            )
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(12.dp),
        content = content,
    )
}

@Composable
private fun BalanceCard(platinumPieces: Double, palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.goldDeep.copy(alpha = 0.8f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(
                painter = painterResource(R.drawable.coin),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Column {
                Text(
                    text = "${GoldFormat.format(platinumPieces)} pp",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = "Platinum Pieces",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/**
 * One permanent-boost purchase row — Speed Boost / Profit Boost (leveled,
 * compounding) or Time Skip (flat, repeatable). [onBuy] is only ever called
 * from an enabled button, so it doesn't need to re-check affordability
 * itself.
 */
@Composable
private fun BoostRow(
    title: String,
    description: String,
    cost: Double,
    canAfford: Boolean,
    onBuy: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    ParchmentCard(palette = palette, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
            WoodenButton(
                text = "Buy — ${GoldFormat.format(cost)} pp",
                onClick = onBuy,
                enabled = canAfford,
                colors = palette,
            )
        }
    }
}

/**
 * The real "Watch an Ad" row — earns [PLATINUM_AD_REWARD_PP] Platinum
 * Pieces, gated by a 24-hour cooldown (see `domain/model/AdRewards.kt`).
 * Disables itself and shows "Available in Xh Ym" whenever
 * [cooldownRemaining] is non-zero, computed reactively from the live
 * `GameState` by the caller rather than tracked as separate UI state — so
 * the label updates on its own as `gameState` ticks, no polling needed here.
 */
@Composable
private fun WatchAdRow(
    cooldownRemaining: Duration,
    onWatchAd: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    val available = cooldownRemaining.isZero
    ParchmentCard(palette = palette, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Watch an Ad",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = "Earn ${GoldFormat.format(PLATINUM_AD_REWARD_PP)} Platinum Pieces by watching a short " +
                        "video. Once every 24 hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
            WoodenButton(
                text = if (available) "Watch" else "In ${DurationFormat.format(cooldownRemaining)}",
                onClick = onWatchAd,
                enabled = available,
                colors = palette,
            )
        }
    }
}

@Composable
private fun PlatinumAdMessageCard(
    message: String,
    onDismiss: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.goldDeep.copy(alpha = 0.8f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink,
            )
            Text(
                text = "✕",
                color = palette.ink.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(onClick = onDismiss),
            )
        }
    }
}

/** One way to earn Platinum Pieces — currently always disabled, since neither ads nor billing exist yet. */
@Composable
private fun EarnMethodRow(title: String, description: String, palette: FantasyPalette, modifier: Modifier = Modifier) {
    ParchmentCard(palette = palette, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
            WoodenButton(text = "Soon", onClick = {}, enabled = false, colors = palette)
        }
    }
}
