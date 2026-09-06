package com.wyrmwhelp.idlehoard.domain.model

/**
 * Shared "beginning/mid/end-game" tier-and-phase shape used by every
 * purchasable upgrade line in this game — the 30 Gold Pieces lines
 * (`GpUpgrades.kt`) and the single Gem Efficiency line (`GemUpgrades.kt`).
 * A line isn't one smooth curve end to end: it's split into three
 * consecutive phases of [beginningTiers]/[midTiers]/[endTiers] tiers each,
 * and both cost and per-tier effect deliberately *jump* — not just
 * continue compounding — the moment a purchase crosses from one phase
 * into the next, so leveling a line up reads as a real progression
 * through distinct stages rather than one long grind.
 */
data class UpgradePhases(
    val beginningTiers: Int,
    val midTiers: Int,
    val endTiers: Int,
) {
    val totalTiers: Int get() = beginningTiers + midTiers + endTiers

    /** Which phase (1, 2, or 3) [tier] (1-indexed) falls into. */
    fun phaseOfTier(tier: Int): Int = when {
        tier <= beginningTiers -> 1
        tier <= beginningTiers + midTiers -> 2
        else -> 3
    }

    /** [tier]'s 0-indexed position within its own phase — tier 1 of phase 2 returns 0, not `beginningTiers`. */
    fun positionWithinPhase(tier: Int): Int = when (phaseOfTier(tier)) {
        1 -> tier - 1
        2 -> tier - beginningTiers - 1
        else -> tier - beginningTiers - midTiers - 1
    }
}

/**
 * Gold/Gem cost to buy tier [tier] (1-indexed; buying tier 1 takes a line
 * from level 0 to level 1) of a line described by [phases]/[baseCost]/
 * [costGrowthRate]/[phaseJumpMultiplier] — [costGrowthRate] compounds
 * smoothly *across the whole line* (same shape as every other geometric
 * cost curve in this game — lair costs, Boosts; the exponent is [tier]'s
 * absolute position, not [UpgradePhases.positionWithinPhase], precisely so
 * this compounding never resets), while [phaseJumpMultiplier] is an
 * *additional* multiplicative jump layered on top once per phase crossed.
 * Using the position *within* a phase for the exponent instead — this
 * function's first-pass implementation did exactly that — silently
 * defeats the "obvious jump" this exists for once a phase runs long enough
 * (Gem Efficiency's 67-tier phases, at 1.15 growth per tier, compound past
 * 10,000x *before* the phase boundary; resetting that back down to a flat
 * `phaseJumpMultiplier` of 5x made phase 2's first tier *cheaper* than
 * phase 1's last one — the opposite of a jump). Keeping the exponent
 * absolute avoids that regardless of how long any given phase is.
 */
fun upgradeTierCost(
    tier: Int,
    phases: UpgradePhases,
    baseCost: Double,
    costGrowthRate: Double,
    phaseJumpMultiplier: Double,
): Double {
    val phase = phases.phaseOfTier(tier)
    val phaseMultiplier = Math.pow(phaseJumpMultiplier, (phase - 1).toDouble())
    return baseCost * phaseMultiplier * Math.pow(costGrowthRate, (tier - 1).toDouble())
}

/**
 * Total percentage-point bonus accumulated after buying [level] tiers of a
 * line whose three phases each grant a flat, *additive* per-tier bonus
 * ([percentPerTierPhase1]/2/3) — additive within and across phases (no
 * compounding), matching [gemIncomeMultiplier]'s own additive-per-Gem
 * style. The per-tier rate itself jumps at each phase boundary (phase 2's
 * tiers are worth more each than phase 1's, phase 3's more than phase 2's),
 * which is where a maxed line's "obvious jump" in power actually comes
 * from — the cost curve's [upgradeTierCost] phase jump only makes each
 * phase more expensive, not more rewarding, on its own.
 */
fun upgradeTotalPercent(
    level: Int,
    phases: UpgradePhases,
    percentPerTierPhase1: Double,
    percentPerTierPhase2: Double,
    percentPerTierPhase3: Double,
): Double {
    val tiersInPhase1 = level.coerceIn(0, phases.beginningTiers)
    val tiersInPhase2 = (level - phases.beginningTiers).coerceIn(0, phases.midTiers)
    val tiersInPhase3 = (level - phases.beginningTiers - phases.midTiers).coerceIn(0, phases.endTiers)
    return tiersInPhase1 * percentPerTierPhase1 +
        tiersInPhase2 * percentPerTierPhase2 +
        tiersInPhase3 * percentPerTierPhase3
}
