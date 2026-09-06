package com.wyrmwhelp.idlehoard.domain.model

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog

/** Which stat a Gold Pieces upgrade line improves — mirrors [MilestoneType]'s own split. */
enum class UpgradeCategory { PROFIT, SPEED }

/**
 * The manual "spend Gold Pieces on a chosen boost" upgrade shop referenced
 * as an open question since early in the project (see CLAUDE.md) —
 * distinct from the automatic ownership milestones in `Milestone.kt`, which
 * this doesn't replace or interact with.
 *
 * 30 lines total, tiered to sum to exactly 475 purchasable upgrades: every
 * [CreatureLairCatalog] lair gets its own Profit line and Speed line (14
 * lairs × 2 = 28 lines, [LAIR_LINE_PHASES] each, 15 tiers), plus two
 * "Everything" lines affecting every owned lair at once
 * ([EVERYTHING_PROFIT_PHASES]/[EVERYTHING_SPEED_PHASES], 28 and 27 tiers) —
 * 28×15 + 28 + 27 = 475. Levels live on [OwnedLair.profitUpgradeLevel]/
 * [OwnedLair.speedUpgradeLevel] for the per-lair lines and
 * [GameState.everythingProfitUpgradeLevel]/[GameState.everythingSpeedUpgradeLevel]
 * for the Everything ones — both reset on a Level Up (the per-lair ones
 * implicitly, since [GameState.lairs] itself resets to the starting map;
 * the Everything ones explicitly, since gold-sourced power is tied to the
 * current run, same as Gold Pieces themselves — see `GameEngine.performLevelUp`).
 *
 * A per-lair line's base cost scales with that lair's own [CreatureLair.baseCostGp]
 * (`LAIR_LINE_BASE_COST_MULTIPLIER` — an early lair's upgrades are cheap in
 * absolute terms, a late lair's appropriately expensive) so the upgrade
 * shop's own pacing tracks the lair catalog's existing tier-0–13 curve
 * rather than needing a second, independently-tuned cost scale per lair.
 * The two Everything lines use one flat, mid-catalog-anchored base cost
 * instead, since they aren't tied to any single lair's economy.
 *
 * Tuning numbers (cost growth, phase jump, per-tier percentages) are
 * first-pass placeholders, not playtested, same as everywhere else in the
 * economy.
 */
object GpUpgrades {
    val LAIR_LINE_PHASES = UpgradePhases(beginningTiers = 5, midTiers = 5, endTiers = 5)
    val EVERYTHING_PROFIT_PHASES = UpgradePhases(beginningTiers = 9, midTiers = 9, endTiers = 10)
    val EVERYTHING_SPEED_PHASES = UpgradePhases(beginningTiers = 9, midTiers = 9, endTiers = 9)

    private const val LAIR_LINE_BASE_COST_MULTIPLIER = 100.0
    private const val EVERYTHING_BASE_COST_GP = 300_000_000.0
    private const val COST_GROWTH_RATE = 1.25
    private const val PHASE_JUMP_MULTIPLIER = 8.0

    private const val PROFIT_PERCENT_PHASE_1 = 2.0
    private const val PROFIT_PERCENT_PHASE_2 = 4.0
    private const val PROFIT_PERCENT_PHASE_3 = 8.0

    private const val SPEED_PERCENT_PHASE_1 = 2.0
    private const val SPEED_PERCENT_PHASE_2 = 4.0
    private const val SPEED_PERCENT_PHASE_3 = 8.0

    /** This lair's own Profit/Speed line's tier-1 cost — scales with how far into the catalog [lair] sits. */
    fun lairLineBaseCost(lair: CreatureLair): Double = lair.baseCostGp * LAIR_LINE_BASE_COST_MULTIPLIER

    /** Gold Pieces to buy tier [tier] (1-indexed) of [lairId]'s own [category] line. */
    fun costForLairTier(lairId: String, category: UpgradeCategory, tier: Int): Double =
        upgradeTierCost(tier, LAIR_LINE_PHASES, lairLineBaseCost(CreatureLairCatalog.get(lairId)), COST_GROWTH_RATE, PHASE_JUMP_MULTIPLIER)

    /** Gold Pieces to buy tier [tier] (1-indexed) of the Everything [category] line. */
    fun costForEverythingTier(category: UpgradeCategory, tier: Int): Double =
        upgradeTierCost(tier, everythingPhases(category), EVERYTHING_BASE_COST_GP, COST_GROWTH_RATE, PHASE_JUMP_MULTIPLIER)

    /**
     * This lair's own Profit-line income multiplier at [level] tiers bought
     * — feeds into `CreatureLair.incomePerCycle`'s `upgradeProfitMultiplier`
     * alongside [everythingProfitMultiplier].
     */
    fun lairProfitMultiplier(level: Int): Double =
        1.0 + upgradeTotalPercent(level, LAIR_LINE_PHASES, PROFIT_PERCENT_PHASE_1, PROFIT_PERCENT_PHASE_2, PROFIT_PERCENT_PHASE_3) / 100.0

    /**
     * This lair's own Speed-line cycle-time multiplier at [level] tiers
     * bought — feeds into `CreatureLair.effectiveProductionSeconds`'s
     * `upgradeSpeedMultiplier` alongside [everythingSpeedMultiplier].
     */
    fun lairSpeedMultiplier(level: Int): Double =
        1.0 + upgradeTotalPercent(level, LAIR_LINE_PHASES, SPEED_PERCENT_PHASE_1, SPEED_PERCENT_PHASE_2, SPEED_PERCENT_PHASE_3) / 100.0

    /** The "Everything" Profit line's income multiplier at [level] tiers bought — applies to every owned lair at once. */
    fun everythingProfitMultiplier(level: Int): Double =
        1.0 + upgradeTotalPercent(level, EVERYTHING_PROFIT_PHASES, PROFIT_PERCENT_PHASE_1, PROFIT_PERCENT_PHASE_2, PROFIT_PERCENT_PHASE_3) / 100.0

    /** The "Everything" Speed line's cycle-time multiplier at [level] tiers bought — applies to every owned lair at once. */
    fun everythingSpeedMultiplier(level: Int): Double =
        1.0 + upgradeTotalPercent(level, EVERYTHING_SPEED_PHASES, SPEED_PERCENT_PHASE_1, SPEED_PERCENT_PHASE_2, SPEED_PERCENT_PHASE_3) / 100.0

    private fun everythingPhases(category: UpgradeCategory): UpgradePhases =
        if (category == UpgradeCategory.PROFIT) EVERYTHING_PROFIT_PHASES else EVERYTHING_SPEED_PHASES
}
