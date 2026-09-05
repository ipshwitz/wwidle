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
     * This lair's own compounding milestone bonus at [unitsOwned] owned —
     * e.g. owning 100 Kobold Warrens (crossing the 25/50/100 rungs) is 8x,
     * independent of every other lair. See [nextMilestoneThreshold] for how
     * far away the next rung is.
     */
    fun individualMilestoneMultiplier(unitsOwned: Int): Double = milestoneMultiplierFor(unitsOwned)

    /**
     * Total Gold Pieces produced per completed cycle by [unitsOwned] units,
     * including this lair's own milestone bonus and, via [globalMultiplier],
     * the "Everything" bonus from [GameState.globalMilestoneMultiplier] —
     * callers that don't pass one (existing tests, mainly) get the
     * no-bonus default of 1.0.
     */
    fun incomePerCycle(unitsOwned: Int, globalMultiplier: Double = 1.0): Double =
        baseIncomeGp * unitsOwned * individualMilestoneMultiplier(unitsOwned) * globalMultiplier
}
