package com.wyrmwhelp.idlehoard.ui.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.wyrmwhelp.idlehoard.domain.model.ActiveTemporaryBoost
import com.wyrmwhelp.idlehoard.domain.model.PERMANENT_GEM_TIERS
import com.wyrmwhelp.idlehoard.domain.model.PERMANENT_PROFIT_TIERS
import com.wyrmwhelp.idlehoard.domain.model.PERMANENT_SPEED_TIERS
import com.wyrmwhelp.idlehoard.domain.model.PLATINUM_AD_REWARD_PP
import com.wyrmwhelp.idlehoard.domain.model.PLATINUM_PURCHASE_OPTIONS
import com.wyrmwhelp.idlehoard.domain.model.PermanentBoostTier
import com.wyrmwhelp.idlehoard.domain.model.PlatinumPurchaseOption
import com.wyrmwhelp.idlehoard.domain.model.TEMPORARY_BOOST_OPTIONS
import com.wyrmwhelp.idlehoard.domain.model.TIME_SKIP_OPTIONS
import com.wyrmwhelp.idlehoard.domain.model.TemporaryBoostCategory
import com.wyrmwhelp.idlehoard.domain.model.TemporaryBoostOption
import com.wyrmwhelp.idlehoard.domain.model.TimeSkipOption
import com.wyrmwhelp.idlehoard.domain.model.costForPermanentBoostPurchase
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.DurationFormat
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat
import java.time.Duration

/**
 * The "Shop" section's real content: the player's current Platinum Pieces
 * balance, every way Platinum actually gets spent (see
 * `domain/model/Boosts.kt`) — permanent boost tiles (2x/5x/10x Speed,
 * 1.5x/2x/5x Profit, 1.5x/2x/5x Gem %, each independently repurchasable
 * and stacking with itself), temporary boost tiles (50x/100x Speed for 5
 * minutes, 15x/25x Profit for 5-10 minutes, stacking multiplicatively with
 * any other still-running boost in the same category), and one row per
 * [TIME_SKIP_OPTIONS] tier — then the two ways to earn more Platinum.
 * Buying here is the only way to acquire any of this; the Upgrades
 * section's Platinum tab only *displays* what's been bought (see
 * `ui/upgrades/UpgradesContent.kt`), it has no buy buttons of its own.
 * "Watch an Ad" (see [platinumAdCooldownRemaining]/[onWatchAd]) is open to
 * guests too — it has no monetary value, so a guest losing it on reinstall
 * isn't a real loss the way losing an IAP receipt would be. "Buy Platinum
 * Pieces" (real Play Billing IAP as of v0.27.0 — one row per
 * [PLATINUM_PURCHASE_OPTIONS] tier, [platinumPurchasePrices] supplying
 * Play's own live formatted price once `BillingManager` resolves it, the
 * tier's own [PlatinumPurchaseOption.priceUsd] as a fallback before that)
 * stays gated to signed-in players — see [isSignedIn] — since that *is*
 * real money, which should stay tied to a recoverable account. Pure
 * display plus callbacks — takes state passed in by `MainActivity`'s
 * `WyrmWhelpApp` (which already holds the `GameViewModel` reference)
 * rather than taking a ViewModel itself.
 */
@Composable
fun ShopContent(
    platinumPieces: Double,
    permanentBoostLevelFor: (PermanentBoostTier) -> Int,
    activeTemporaryBoosts: List<Pair<ActiveTemporaryBoost, Duration>>,
    isSignedIn: Boolean,
    platinumAdCooldownRemaining: Duration,
    platinumAdMessage: String?,
    platinumPurchasePrices: Map<String, String>,
    platinumPurchaseMessage: String?,
    onBuyPermanentBoost: (PermanentBoostTier) -> Unit,
    onBuyTemporaryBoost: (TemporaryBoostOption) -> Unit,
    onBuyTimeSkip: (TimeSkipOption) -> Unit,
    onWatchAd: () -> Unit,
    onDismissPlatinumAdMessage: () -> Unit,
    onBuyPlatinumPack: (String) -> Unit,
    onDismissPlatinumPurchaseMessage: () -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { BalanceCard(platinumPieces = platinumPieces, palette = palette) }
        item { SectionLabel(text = "Permanent Boosts", palette = palette) }
        item {
            PermanentBoostCategoryCard(
                title = "Speed",
                tiers = PERMANENT_SPEED_TIERS,
                platinumPieces = platinumPieces,
                levelFor = permanentBoostLevelFor,
                onBuy = onBuyPermanentBoost,
                palette = palette,
            )
        }
        item {
            PermanentBoostCategoryCard(
                title = "Profit",
                tiers = PERMANENT_PROFIT_TIERS,
                platinumPieces = platinumPieces,
                levelFor = permanentBoostLevelFor,
                onBuy = onBuyPermanentBoost,
                palette = palette,
            )
        }
        item {
            PermanentBoostCategoryCard(
                title = "Gem %",
                tiers = PERMANENT_GEM_TIERS,
                platinumPieces = platinumPieces,
                levelFor = permanentBoostLevelFor,
                onBuy = onBuyPermanentBoost,
                palette = palette,
            )
        }
        item { SectionLabel(text = "Temporary Boosts", palette = palette) }
        if (activeTemporaryBoosts.isNotEmpty()) {
            item { ActiveTemporaryBoostsCard(activeTemporaryBoosts, palette) }
        }
        TEMPORARY_BOOST_OPTIONS.forEach { option ->
            item {
                BoostRow(
                    title = "${GoldFormat.format(option.multiplier)}x ${option.category.label()} — " +
                        DurationFormat.format(Duration.ofSeconds(option.durationSeconds)),
                    description = "Instantly activates ${GoldFormat.format(option.multiplier)}x " +
                        "${option.category.label().lowercase()} for ${DurationFormat.format(Duration.ofSeconds(option.durationSeconds))}. " +
                        "Stacks with any other active boost in this category.",
                    cost = option.costPp,
                    canAfford = platinumPieces >= option.costPp,
                    onBuy = { onBuyTemporaryBoost(option) },
                    palette = palette,
                )
            }
        }
        item { SectionLabel(text = "Time Skips", palette = palette) }
        TIME_SKIP_OPTIONS.forEach { option ->
            item {
                val duration = Duration.ofSeconds(option.seconds.toLong())
                BoostRow(
                    title = "Time Skip — ${DurationFormat.format(duration)}",
                    description = "Instantly grants ${DurationFormat.format(duration)} of production " +
                        "from every owned lair. One-time use.",
                    cost = option.costPp,
                    canAfford = platinumPieces >= option.costPp,
                    onBuy = { onBuyTimeSkip(option) },
                    palette = palette,
                )
            }
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
            item { SectionLabel(text = "Buy Platinum Pieces", palette = palette) }
            PLATINUM_PURCHASE_OPTIONS.forEach { option ->
                item {
                    PlatinumPackRow(
                        option = option,
                        formattedPrice = platinumPurchasePrices[option.productId],
                        onBuy = { onBuyPlatinumPack(option.productId) },
                        palette = palette,
                    )
                }
            }
            platinumPurchaseMessage?.let { message ->
                item {
                    PlatinumAdMessageCard(
                        message = message,
                        onDismiss = onDismissPlatinumPurchaseMessage,
                        palette = palette,
                    )
                }
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

/** "Speed" / "Profit" for a [TemporaryBoostCategory], matching the labels used throughout the Shop and Upgrades screens. */
private fun TemporaryBoostCategory.label(): String = if (this == TemporaryBoostCategory.PROFIT) "Profit" else "Speed"

/**
 * One category's three permanent-boost tiles stacked in a single card
 * (Speed, Profit, or Gem %) — each [tiers] entry gets its own row, using
 * [levelFor] to read how many copies of that tier are already owned.
 */
@Composable
private fun PermanentBoostCategoryCard(
    title: String,
    tiers: List<PermanentBoostTier>,
    platinumPieces: Double,
    levelFor: (PermanentBoostTier) -> Int,
    onBuy: (PermanentBoostTier) -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.goldDeep.copy(alpha = 0.8f)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        tiers.forEachIndexed { index, tier ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            val level = levelFor(tier)
            val cost = costForPermanentBoostPurchase(tier, level)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${GoldFormat.format(tier.multiplier)}x — owned $level",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.ink.copy(alpha = 0.85f),
                    )
                    if (level > 0) {
                        Text(
                            text = "contributing ${GoldFormat.format(Math.pow(tier.multiplier, level.toDouble()))}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink.copy(alpha = 0.6f),
                        )
                    }
                }
                WoodenButton(
                    text = "Buy — ${GoldFormat.format(cost)} pp",
                    onClick = { onBuy(tier) },
                    enabled = platinumPieces >= cost,
                    colors = palette,
                )
            }
        }
    }
}

/** Read-only summary of every still-running temporary boost — the Shop's own live countdown, not a purchase control. */
@Composable
private fun ActiveTemporaryBoostsCard(
    activeTemporaryBoosts: List<Pair<ActiveTemporaryBoost, Duration>>,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.gemDeep.copy(alpha = 0.8f)) {
        Text(
            text = "Active",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        activeTemporaryBoosts.forEach { (boost, remaining) ->
            Text(
                text = "${GoldFormat.format(boost.multiplier)}x ${boost.category.label()} — ${DurationFormat.format(remaining)} left",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.8f),
            )
        }
    }
}

/**
 * One permanent-boost purchase row — Time Skip or temporary-boost tile
 * (both flat-cost, repeatable, no growing level). [onBuy] is only ever
 * called from an enabled button, so it doesn't need to re-check
 * affordability itself.
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

/**
 * One Platinum Pieces pack — [formattedPrice] is Play Billing's own live,
 * localized price string once `BillingManager` has resolved
 * [PlatinumPurchaseOption.productId]; null before that (or if the product
 * doesn't exist in the Play Console yet) falls back to
 * [PlatinumPurchaseOption.priceUsd] and disables the button, since a
 * purchase can't actually be charged without a real live price.
 */
@Composable
private fun PlatinumPackRow(
    option: PlatinumPurchaseOption,
    formattedPrice: String?,
    onBuy: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.goldDeep.copy(alpha = 0.8f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${GoldFormat.format(option.platinumPieces.toDouble())} Platinum Pieces",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = "One-time purchase, real money.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.7f),
                )
            }
            WoodenButton(
                text = formattedPrice ?: "$${"%.2f".format(option.priceUsd)}",
                onClick = onBuy,
                enabled = formattedPrice != null,
                colors = palette,
            )
        }
    }
}
