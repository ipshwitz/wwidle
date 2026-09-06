package com.wyrmwhelp.idlehoard.domain.model

import kotlin.math.floor
import kotlin.math.ln

/**
 * Static definition of a Creature Lair — a monster den that passively produces
 * Gold Pieces, the idle-clicker equivalent of an Adventure Capitalist "business".
 * Tuning numbers (costs/income/timing) are first-pass placeholders for playtesting,
 * not final balance.
 *
 * @property id Stable catalog key, e.g. "kobold_warren".
 * @property name Player-facing lair name, e.g. "Kobold Warren".
 * @property monster The D&D 5E SRD monster inhabiting the lair, e.g. "Kobold".
 * @property challengeRating The monster's 5E Challenge Rating (as written, e.g. "1/8"),
 *   used as flavor and as the anchor for this tier's relative power/cost.
 * @property flavorText Short original narrative blurb shown in the lair's detail view.
 * @property tier 0-based unlock/progression order; lower tiers unlock first.
 * @property baseCostGp Cost in Gold Pieces to claim the first unit of this lair.
 * @property costGrowthRate Multiplier applied to cost per unit already owned.
 * @property baseIncomeGp Gold Pieces produced by a single unit per completed cycle.
 * @property baseProductionSeconds Seconds for one production cycle to complete.
 * @property stewardCostGp Cost in Gold Pieces to hire a Steward, who automatically
 *   collects completed cycles without the player tapping the lair.
 */
data class CreatureLair(
    val id: String,
    val name: String,
    val monster: String,
    val challengeRating: String,
    val flavorText: String,
    val tier: Int,
    val baseCostGp: Double,
    val costGrowthRate: Double,
    val baseIncomeGp: Double,
    val baseProductionSeconds: Double,
    val stewardCostGp: Double,
) {
    /** Cost in Gold Pieces to claim the next unit, given [unitsOwned] already claimed. */
    fun costForNextUnit(unitsOwned: Int): Double =
        baseCostGp * Math.pow(costGrowthRate, unitsOwned.toDouble())

    /**
     * Total Gold Pieces to claim [quantity] more units at once, given
     * [unitsOwned] already claimed — the closed-form sum of a geometric
     * series (each unit costs `costGrowthRate` times the last), not
     * [costForNextUnit] called in a loop.
     */
    fun costForUnits(unitsOwned: Int, quantity: Int): Double {
        if (quantity <= 0) return 0.0
        val firstCost = costForNextUnit(unitsOwned)
        return if (costGrowthRate == 1.0) {
            firstCost * quantity
        } else {
            firstCost * (Math.pow(costGrowthRate, quantity.toDouble()) - 1.0) / (costGrowthRate - 1.0)
        }
    }

    /**
     * The most additional units affordable for [availableGp], given
     * [unitsOwned] already claimed — the closed-form inverse of
     * [costForUnits] (solving the geometric series sum for its unit count),
     * not a purchase-simulating loop. A loop would need an unbounded number
     * of iterations for a slow-growth lair once the economy reaches the
     * kind of gold totals `GoldFormat`'s letter suffixes exist for.
     *
     * The closed-form estimate is nudged by a few corrective steps
     * afterward since floating-point error can put it off by one right at
     * the affordability boundary.
     */
    fun maxAffordableUnits(unitsOwned: Int, availableGp: Double): Int {
        val nextCost = costForNextUnit(unitsOwned)
        if (availableGp < nextCost) return 0

        val estimate = if (costGrowthRate == 1.0) {
            (availableGp / nextCost).toInt()
        } else {
            val ratio = 1.0 + availableGp * (costGrowthRate - 1.0) / nextCost
            floor(ln(ratio) / ln(costGrowthRate)).toInt()
        }.coerceAtLeast(1)

        var n = estimate
        while (n > 0 && costForUnits(unitsOwned, n) > availableGp) n--
        while (costForUnits(unitsOwned, n + 1) <= availableGp) n++
        return n
    }

    /**
     * This lair's own compounding Speed milestone bonus at [unitsOwned]
     * owned — e.g. owning 400 Kobold Warrens (crossing the 25/50/100/200/300/400
     * rungs, all [MilestoneType.SPEED]) is 64x, independent of every other
     * lair. See [nextMilestoneThreshold] for how far away the next rung
     * (of either type) is.
     */
    fun individualSpeedMilestoneMultiplier(unitsOwned: Int): Double = milestoneMultiplierFor(unitsOwned, MilestoneType.SPEED)

    /**
     * This lair's own compounding Income milestone bonus at [unitsOwned]
     * owned — the [MilestoneType.INCOME] rungs (500 and up), same
     * compounding idea as [individualSpeedMilestoneMultiplier] but for the
     * rungs that boost gold per cycle instead of cycle speed.
     */
    fun individualIncomeMilestoneMultiplier(unitsOwned: Int): Double = milestoneMultiplierFor(unitsOwned, MilestoneType.INCOME)

    /**
     * Total Gold Pieces produced per completed cycle by [unitsOwned] units,
     * including this lair's own Income milestone bonus, the "Everything"
     * Income bonus via [globalIncomeMultiplier] (from
     * `GameState.globalIncomeMilestoneMultiplier`), the permanent
     * account-wide profit boost via [profitBoostMultiplier] (from
     * `profitBoostMultiplier(GameState.profitBoostLevel)` in `Boosts.kt`),
     * the temporary Gem bonus via [gemBonusMultiplier] (from
     * `gemIncomeMultiplier(GameState.gems, GameState.gemEfficiencyLevel)`
     * in `LevelUp.kt`), and the manually-bought Gold Pieces upgrade bonus
     * via [upgradeProfitMultiplier] (this lair's own Profit line combined
     * with the "Everything Profit" line — see `GpUpgrades.kt`) — callers
     * that don't pass one of these (existing tests, mainly) get the
     * no-bonus default of 1.0 for it. Speed-type milestone rungs have no
     * effect here — see [effectiveProductionSeconds] for those.
     */
    fun incomePerCycle(
        unitsOwned: Int,
        globalIncomeMultiplier: Double = 1.0,
        profitBoostMultiplier: Double = 1.0,
        gemBonusMultiplier: Double = 1.0,
        upgradeProfitMultiplier: Double = 1.0,
    ): Double =
        baseIncomeGp * unitsOwned * individualIncomeMilestoneMultiplier(unitsOwned) *
            globalIncomeMultiplier * profitBoostMultiplier * gemBonusMultiplier * upgradeProfitMultiplier

    /**
     * This lair's actual cycle time at [unitsOwned] owned, after the
     * permanent account-wide speed boost (`speedBoostMultiplier(GameState.speedBoostLevel)`
     * in `Boosts.kt`), this lair's own Speed milestone bonus (see
     * [individualSpeedMilestoneMultiplier]), the "Everything" Speed
     * bonus via [globalSpeedMilestoneMultiplier] (from
     * `GameState.globalSpeedMilestoneMultiplier`), and the manually-bought
     * Gold Pieces upgrade bonus via [upgradeSpeedMultiplier] (this lair's
     * own Speed line combined with the "Everything Speed" line — see
     * `GpUpgrades.kt`) — every one of these makes cycles complete *faster*,
     * so they divide [baseProductionSeconds] rather than multiplying it.
     * [unitsOwned] defaults to 0 (no milestone speed bonus) for callers
     * that don't care about it (existing tests, mainly); Income-type
     * milestone rungs have no effect here — see [incomePerCycle] for those.
     */
    fun effectiveProductionSeconds(
        unitsOwned: Int = 0,
        speedBoostMultiplier: Double = 1.0,
        globalSpeedMilestoneMultiplier: Double = 1.0,
        upgradeSpeedMultiplier: Double = 1.0,
    ): Double =
        baseProductionSeconds / (
            speedBoostMultiplier * individualSpeedMilestoneMultiplier(unitsOwned) *
                globalSpeedMilestoneMultiplier * upgradeSpeedMultiplier
            )
}
