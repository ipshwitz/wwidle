package com.wyrmwhelp.idlehoard.domain.model

/**
 * Per-lair "Steward Efficiency" upgrade — spending Gold Pieces to make a
 * *specific* lair's own Steward better at their job, which manifests as a
 * discount on that lair's own future unit costs
 * ([CreatureLair.costForNextUnit]'s `costMultiplier`). Confirmed design,
 * deliberately unlike every other upgrade in the game (`GpUpgrades.kt`'s
 * Profit/Speed lines): those are unbounded compounding multipliers, since
 * there's no ceiling on "more income" or "faster cycles," but a cost
 * *discount* can never be allowed anywhere near 100% — a free lair would
 * break the whole exponential cost curve this game's economy runs on. So
 * this is a small, fixed ladder of [DISCOUNT_PERCENTAGES] tiers instead of
 * an open-ended one: each tier *sets* the discount outright (10%, 20%,
 * ..., 90%, 99%) rather than adding to the tier before it, hard-capped at
 * 99% — a lair can get 100x cheaper to keep buying, never free.
 *
 * **Requires the lair's own real Steward to already be hired
 * (`OwnedLair.hasSteward`) before any tier is purchasable — deliberately
 * *not* satisfied by the account-wide Universal Steward
 * (`domain/model/UniversalSteward.kt`), even though that also makes the
 * lair auto-collect.** This is an explicit, intentional choice (not an
 * oversight to "fix" later): a real per-lair Steward is what's being
 * upgraded here, so one still has to exist to upgrade, even for a player
 * who's already earned the account-wide one and technically has no other
 * reason left to hire a real Steward for that lair.
 *
 * Levels live on [OwnedLair.stewardEfficiencyLevel] — resets on a Level Up
 * implicitly, since [GameState.lairs] itself resets to the starting map,
 * same as [OwnedLair.profitUpgradeLevel]/[OwnedLair.speedUpgradeLevel].
 *
 * Costs are deliberately steep — reaching 99% off is meant to be a
 * significant late-game investment for that lair, not incidental. Tiers
 * 1-5 (10%-50% off) grow at the same steady 3x-per-tier rate the feature
 * launched with (v0.44.0) — still large ("large costs," per explicit
 * instruction) but reachable within one run. Tiers 6-10 (60%-99% off) are
 * split into the same [UpgradePhases]/[upgradeTierCost] "phase jump" shape
 * `GpUpgrades`/`GemUpgrades` already use elsewhere, with a
 * [PHASE_JUMP_MULTIPLIER] steep enough that tier 10 specifically requires
 * more Gold than a single run can plausibly earn — genuinely needing
 * several Level Ups' worth of compounding Gem/Everything-upgrade/Platinum
 * income bonuses to reach, per explicit follow-up feedback that 99% off
 * was too easy to hit in one sitting. Scales
 * off the lair's own [CreatureLair.baseCostGp] the same way
 * `GpUpgrades.lairLineBaseCost` does, but with a much larger starting
 * multiplier and a much steeper per-tier growth rate, reflecting how much
 * more powerful a cost discount is than a Profit/Speed percentage point.
 * First-pass placeholder, not playtested, same as everywhere else in the
 * economy.
 */
object StewardEfficiency {
    /** What each tier (1-indexed) sets the discount to outright — not additive with the tier before it. */
    val DISCOUNT_PERCENTAGES: List<Double> = listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 99.0)

    /** The highest tier purchasable — [DISCOUNT_PERCENTAGES]' size. */
    val MAX_TIER: Int = DISCOUNT_PERCENTAGES.size

    private const val BASE_COST_MULTIPLIER = 10_000.0
    private const val COST_GROWTH_RATE = 3.0

    /**
     * 5 tiers (10%-50% off) at the original steady rate, then 3 (60%-80%)
     * and 2 more (90%/99%) each behind their own [PHASE_JUMP_MULTIPLIER]
     * wall — the last jump (entering tier 9/10) is squared, since it's
     * applied once per phase crossed, making the 99% tier the real,
     * multi-Level-Up-away endgame goal.
     */
    private val PHASES = UpgradePhases(beginningTiers = 5, midTiers = 3, endTiers = 2)
    private const val PHASE_JUMP_MULTIPLIER = 10_000.0

    /** Gold Pieces to buy tier [tier] (1-indexed) of [lair]'s own Steward Efficiency line. */
    fun costForTier(lair: CreatureLair, tier: Int): Double =
        upgradeTierCost(
            tier = tier,
            phases = PHASES,
            baseCost = lair.baseCostGp * BASE_COST_MULTIPLIER,
            costGrowthRate = COST_GROWTH_RATE,
            phaseJumpMultiplier = PHASE_JUMP_MULTIPLIER,
        )

    /**
     * The cost multiplier a lair's own units are bought at, given [level]
     * Steward Efficiency tiers already bought — 1.0 (no discount) at level
     * 0, then exactly `1 - DISCOUNT_PERCENTAGES[level - 1] / 100` for any
     * level from 1 up to [MAX_TIER] (clamped, so a stray level past
     * [MAX_TIER] can't be misread as a discount past 99%).
     */
    fun costMultiplier(level: Int): Double {
        if (level <= 0) return 1.0
        val discountPercent = DISCOUNT_PERCENTAGES[(level - 1).coerceAtMost(MAX_TIER - 1)]
        return 1.0 - discountPercent / 100.0
    }
}
