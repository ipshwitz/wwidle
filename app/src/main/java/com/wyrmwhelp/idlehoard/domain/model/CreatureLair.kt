package com.wyrmwhelp.idlehoard.domain.model

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

    /** Total Gold Pieces produced per completed cycle by [unitsOwned] units. */
    fun incomePerCycle(unitsOwned: Int): Double =
        baseIncomeGp * unitsOwned
}
