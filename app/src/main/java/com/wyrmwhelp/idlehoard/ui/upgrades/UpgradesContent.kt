package com.wyrmwhelp.idlehoard.ui.upgrades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.GEM_INCOME_BONUS_PER_GEM
import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.model.GemUpgrades
import com.wyrmwhelp.idlehoard.domain.model.GpUpgrades
import com.wyrmwhelp.idlehoard.domain.model.OwnedLair
import com.wyrmwhelp.idlehoard.domain.model.UpgradeCategory
import com.wyrmwhelp.idlehoard.ui.common.ComingSoonPlaceholder
import com.wyrmwhelp.idlehoard.ui.common.FantasyPalette
import com.wyrmwhelp.idlehoard.ui.common.WoodenButton
import com.wyrmwhelp.idlehoard.ui.format.GoldFormat
import com.wyrmwhelp.idlehoard.ui.game.rarityColor

/** The three currencies the Upgrades section is organized around — Platinum is a placeholder, see [UpgradesContent]'s class doc. */
private enum class UpgradeTab(val label: String) {
    GOLD("Gold"),
    GEMS("Gems"),
    PLATINUM("Platinum"),
}

/**
 * The "Upgrades" section's real content: the long-planned manual upgrade
 * shop (distinct from the automatic ownership milestones in
 * `domain/model/Milestone.kt`) — three tabs, one per currency that can buy
 * upgrades in this game.
 *
 * **Gold tab** — 475 purchasable tiers total across 30 lines
 * (`domain/model/GpUpgrades.kt`): 14 lairs × (Profit + Speed) = 28
 * per-lair lines, plus 2 "Everything" lines affecting every owned lair at
 * once. Every line resets on a Level Up, same as Gold Pieces themselves
 * (see `GameEngine.performLevelUp`).
 *
 * **Gems tab** — a single 200-tier "Gem Efficiency" line
 * (`domain/model/GemUpgrades.kt`) raising the per-Gem income bonus
 * [gemIncomeMultiplier] grants. Resets on a Level Up alongside Gems
 * themselves, since Gems are temporary (see `domain/model/LevelUp.kt`).
 *
 * **Platinum tab** — deliberately still a [ComingSoonPlaceholder]. This
 * game already has Speed Boost/Profit Boost bought with Platinum
 * (`domain/model/Boosts.kt`); folding those into (or replacing them with)
 * a tiered Platinum upgrade shop here was explicitly deferred rather than
 * built alongside Gold and Gems.
 *
 * Pure display plus callbacks — reads [lairs]/[state] passed in by the
 * caller (`MainActivity`'s `WyrmWhelpApp`, which already holds the
 * `GameViewModel` reference) and forwards purchases through
 * [onBuyGpLairUpgrade]/[onBuyGpEverythingUpgrade]/[onBuyGemEfficiencyUpgrade]
 * rather than calling the ViewModel itself, same pattern as
 * `ShopContent`/`StewardsContent`.
 */
@Composable
fun UpgradesContent(
    lairs: List<CreatureLair>,
    state: GameState,
    onBuyGpLairUpgrade: (String, UpgradeCategory) -> Unit,
    onBuyGpEverythingUpgrade: (UpgradeCategory) -> Unit,
    onBuyGemEfficiencyUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    palette: FantasyPalette = FantasyPalette.Default,
) {
    var selectedTab by remember { mutableStateOf(UpgradeTab.GOLD) }

    Column(modifier = modifier.fillMaxSize()) {
        UpgradeTabRow(selected = selectedTab, onSelect = { selectedTab = it }, palette = palette)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                UpgradeTab.GOLD -> GoldUpgradesTab(
                    lairs = lairs,
                    state = state,
                    onBuyLairUpgrade = onBuyGpLairUpgrade,
                    onBuyEverythingUpgrade = onBuyGpEverythingUpgrade,
                    palette = palette,
                )
                UpgradeTab.GEMS -> GemsUpgradesTab(
                    gems = state.gems,
                    gemEfficiencyLevel = state.gemEfficiencyLevel,
                    onBuyGemEfficiency = onBuyGemEfficiencyUpgrade,
                    palette = palette,
                )
                UpgradeTab.PLATINUM -> ComingSoonPlaceholder()
            }
        }
    }
}

@Composable
private fun UpgradeTabRow(selected: UpgradeTab, onSelect: (UpgradeTab) -> Unit, palette: FantasyPalette, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        UpgradeTab.entries.forEach { tab ->
            UpgradeTabButton(
                text = tab.label,
                selected = tab == selected,
                onClick = { onSelect(tab) },
                palette = palette,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * A tab button, not a [WoodenButton][com.wyrmwhelp.idlehoard.ui.common.WoodenButton] —
 * unlike that component's `enabled` (which also gates clickability),
 * every tab here must stay clickable regardless of selection state, only
 * its *color* should change, so this is its own small composable rather
 * than (ab)using `enabled` for a purely visual highlight.
 */
@Composable
private fun UpgradeTabButton(text: String, selected: Boolean, onClick: () -> Unit, palette: FantasyPalette, modifier: Modifier = Modifier) {
    val shape = CutCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (selected) listOf(palette.goldBright, palette.goldDeep) else listOf(palette.woodLight, palette.woodDark),
                ),
            )
            .border(1.dp, palette.woodDark, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) palette.ink else palette.parchment,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** A translucent parchment card matching `LairCard`/`ShopContent`/`StewardsContent`'s base treatment. */
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
private fun SectionLabel(text: String, palette: FantasyPalette, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 4.dp),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Serif, color = palette.ink),
    )
}

/**
 * One upgrade line's current level, effect, and buy control — shared shape
 * for every line in both tabs (a lair's own Profit/Speed, the two
 * Everything lines, and Gem Efficiency). Never wraps itself in a
 * [ParchmentCard] — callers that want one line per card (Everything, Gem
 * Efficiency) wrap it themselves; [LairUpgradeCard] stacks two of these
 * inside one shared card instead.
 */
@Composable
private fun UpgradeLineRow(
    label: String,
    level: Int,
    maxLevel: Int,
    effectDescription: String,
    costLabel: String,
    canAfford: Boolean,
    onBuy: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    val maxed = level >= maxLevel
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$label — Lv $level/$maxLevel",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
            )
            Text(
                text = effectDescription,
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink.copy(alpha = 0.7f),
            )
        }
        WoodenButton(
            text = if (maxed) "Maxed" else "Buy — $costLabel",
            onClick = onBuy,
            enabled = !maxed && canAfford,
            colors = palette,
        )
    }
}

@Composable
private fun GoldUpgradesTab(
    lairs: List<CreatureLair>,
    state: GameState,
    onBuyLairUpgrade: (String, UpgradeCategory) -> Unit,
    onBuyEverythingUpgrade: (UpgradeCategory) -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ParchmentCard(palette = palette) {
                Text(
                    text = "Gold Upgrades",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = "Spend Gold Pieces on permanent-for-this-run boosts — a lair's own Profit " +
                        "and Speed, or \"Everything\" lines that improve every owned lair at once. " +
                        "Resets on your next Level Up, same as Gold itself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.8f),
                )
            }
        }
        item { SectionLabel(text = "Everything", palette = palette) }
        item {
            EverythingUpgradeCard(
                category = UpgradeCategory.PROFIT,
                level = state.everythingProfitUpgradeLevel,
                goldPieces = state.goldPieces,
                onBuy = { onBuyEverythingUpgrade(UpgradeCategory.PROFIT) },
                palette = palette,
            )
        }
        item {
            EverythingUpgradeCard(
                category = UpgradeCategory.SPEED,
                level = state.everythingSpeedUpgradeLevel,
                goldPieces = state.goldPieces,
                onBuy = { onBuyEverythingUpgrade(UpgradeCategory.SPEED) },
                palette = palette,
            )
        }
        item { SectionLabel(text = "Per-Lair", palette = palette) }
        items(lairs, key = { it.id }) { lair ->
            LairUpgradeCard(
                lair = lair,
                owned = state.ownedLair(lair.id),
                goldPieces = state.goldPieces,
                onBuyProfit = { onBuyLairUpgrade(lair.id, UpgradeCategory.PROFIT) },
                onBuySpeed = { onBuyLairUpgrade(lair.id, UpgradeCategory.SPEED) },
                palette = palette,
            )
        }
    }
}

@Composable
private fun EverythingUpgradeCard(
    category: UpgradeCategory,
    level: Int,
    goldPieces: Double,
    onBuy: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    val maxLevel = if (category == UpgradeCategory.PROFIT) {
        GpUpgrades.EVERYTHING_PROFIT_PHASES.totalTiers
    } else {
        GpUpgrades.EVERYTHING_SPEED_PHASES.totalTiers
    }
    val nextTier = level + 1
    val cost = if (nextTier <= maxLevel) GpUpgrades.costForEverythingTier(category, nextTier) else 0.0
    val currentBonusPercent = if (category == UpgradeCategory.PROFIT) {
        (GpUpgrades.everythingProfitMultiplier(level) - 1.0) * 100.0
    } else {
        (GpUpgrades.everythingSpeedMultiplier(level) - 1.0) * 100.0
    }
    val categoryLabel = if (category == UpgradeCategory.PROFIT) "Profit" else "Speed"
    ParchmentCard(palette = palette, modifier = modifier, borderColor = palette.goldDeep.copy(alpha = 0.8f)) {
        UpgradeLineRow(
            label = "Everything — $categoryLabel",
            level = level,
            maxLevel = maxLevel,
            effectDescription = "+${GoldFormat.format(currentBonusPercent)}% $categoryLabel, every owned lair",
            costLabel = "${GoldFormat.format(cost)} gp",
            canAfford = goldPieces >= cost,
            onBuy = onBuy,
            palette = palette,
        )
    }
}

@Composable
private fun LairUpgradeCard(
    lair: CreatureLair,
    owned: OwnedLair,
    goldPieces: Double,
    onBuyProfit: () -> Unit,
    onBuySpeed: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    val rarity = rarityColor(lair.tier)
    val maxLevel = GpUpgrades.LAIR_LINE_PHASES.totalTiers
    val profitNextTier = owned.profitUpgradeLevel + 1
    val profitCost = if (profitNextTier <= maxLevel) GpUpgrades.costForLairTier(lair.id, UpgradeCategory.PROFIT, profitNextTier) else 0.0
    val speedNextTier = owned.speedUpgradeLevel + 1
    val speedCost = if (speedNextTier <= maxLevel) GpUpgrades.costForLairTier(lair.id, UpgradeCategory.SPEED, speedNextTier) else 0.0

    ParchmentCard(palette = palette, modifier = modifier, borderColor = rarity.copy(alpha = 0.7f)) {
        Text(
            text = lair.name,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
        )
        Spacer(Modifier.height(6.dp))
        UpgradeLineRow(
            label = "Profit",
            level = owned.profitUpgradeLevel,
            maxLevel = maxLevel,
            effectDescription = "+${GoldFormat.format((GpUpgrades.lairProfitMultiplier(owned.profitUpgradeLevel) - 1.0) * 100.0)}% income",
            costLabel = "${GoldFormat.format(profitCost)} gp",
            canAfford = goldPieces >= profitCost,
            onBuy = onBuyProfit,
            palette = palette,
        )
        Spacer(Modifier.height(6.dp))
        UpgradeLineRow(
            label = "Speed",
            level = owned.speedUpgradeLevel,
            maxLevel = maxLevel,
            effectDescription = "+${GoldFormat.format((GpUpgrades.lairSpeedMultiplier(owned.speedUpgradeLevel) - 1.0) * 100.0)}% speed",
            costLabel = "${GoldFormat.format(speedCost)} gp",
            canAfford = goldPieces >= speedCost,
            onBuy = onBuySpeed,
            palette = palette,
        )
    }
}

@Composable
private fun GemsUpgradesTab(
    gems: Long,
    gemEfficiencyLevel: Int,
    onBuyGemEfficiency: () -> Unit,
    palette: FantasyPalette,
    modifier: Modifier = Modifier,
) {
    val maxLevel = GemUpgrades.PHASES.totalTiers
    val nextTier = gemEfficiencyLevel + 1
    val cost = if (nextTier <= maxLevel) GemUpgrades.costForTierGems(nextTier) else 0L
    val currentPerGemPercent = (GEM_INCOME_BONUS_PER_GEM + GemUpgrades.bonusPerGem(gemEfficiencyLevel)) * 100.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ParchmentCard(palette = palette) {
                Text(
                    text = "Gem Upgrades",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, color = palette.ink),
                )
                Text(
                    text = "Spend Gems to raise how much income bonus each Gem is worth. Gems you " +
                        "spend here stop counting toward that same bonus — and, like your Gems " +
                        "themselves, this resets on your next Level Up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.8f),
                )
            }
        }
        item {
            ParchmentCard(palette = palette, borderColor = palette.gemDeep.copy(alpha = 0.8f)) {
                UpgradeLineRow(
                    label = "Gem Efficiency",
                    level = gemEfficiencyLevel,
                    maxLevel = maxLevel,
                    effectDescription = "each Gem now worth +${GoldFormat.format(currentPerGemPercent)}% income",
                    costLabel = "${GoldFormat.format(cost.toDouble())} gems",
                    canAfford = gems >= cost,
                    onBuy = onBuyGemEfficiency,
                    palette = palette,
                )
            }
        }
    }
}
