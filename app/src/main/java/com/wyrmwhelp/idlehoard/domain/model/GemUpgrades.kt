package com.wyrmwhelp.idlehoard.domain.model

/**
 * The Gem-spent upgrade shop — a single line, "Gem Efficiency," raising
 * the per-Gem income bonus [gemIncomeMultiplier] itself grants above its
 * base [GEM_INCOME_BONUS_PER_GEM] rate (2%/Gem). Unlike the Gold Pieces
 * shop (`GpUpgrades.kt`), there's no separate "Everything Speed"-style
 * line here — per explicit user design intent, every Gem upgrade is about
 * the value of a Gem itself, nothing else. 200 tiers total ([PHASES]),
 * split into the same three-phase beginning/mid/end-game shape every
 * upgrade line in this game uses (see `UpgradeTiers.kt`).
 *
 * Levels live on [GameState.gemEfficiencyLevel] and reset on a Level Up —
 * Gems themselves are temporary (see `LevelUp.kt`'s class doc), so a
 * Gem-bought upgrade to their value can't outlive them either, or the
 * player could keep a permanently-growing bonus by spending Gems just
 * before every reset.
 *
 * Buying a tier spends Gems, which — since [gemIncomeMultiplier] reads the
 * live [GameState.gems] balance directly — automatically stops those spent
 * Gems from contributing to the passive income bonus, exactly matching the
 * explicit request that "spent gems on upgrades shouldn't count towards
 * the lair profit increase." No special-casing needed for that; it falls
 * out of gems already being a plain spendable balance.
 *
 * Tuning numbers are first-pass placeholders, not playtested, same as
 * everywhere else in the economy.
 */
object GemUpgrades {
    val PHASES = UpgradePhases(beginningTiers = 67, midTiers = 67, endTiers = 66)

    private const val BASE_COST_GEMS = 5.0
    private const val COST_GROWTH_RATE = 1.15
    private const val PHASE_JUMP_MULTIPLIER = 5.0

    // Additive per-Gem-percentage-point bonus per tier bought, by phase —
    // small since it multiplies every Gem held, not just this upgrade.
    private const val PER_GEM_BONUS_PHASE_1 = 0.0005 // +0.05 percentage points/tier
    private const val PER_GEM_BONUS_PHASE_2 = 0.001 // +0.10 percentage points/tier
    private const val PER_GEM_BONUS_PHASE_3 = 0.002 // +0.20 percentage points/tier

    /** Raw (fractional) Gem cost formula for tier [tier] (1-indexed) — see [costForTierGems] for the whole-Gem price actually charged. */
    fun costForTier(tier: Int): Double =
        upgradeTierCost(tier, PHASES, BASE_COST_GEMS, COST_GROWTH_RATE, PHASE_JUMP_MULTIPLIER)

    /**
     * Gems to buy tier [tier] (1-indexed) of Gem Efficiency — [costForTier]
     * rounded up to a whole Gem, since Gems (unlike Gold Pieces) are a
     * whole-number currency and a fractional formula result shouldn't
     * silently round down to a cheaper price.
     */
    fun costForTierGems(tier: Int): Long = kotlin.math.ceil(costForTier(tier)).toLong()

    /**
     * The extra per-Gem income-bonus rate (as a decimal, e.g. 0.05 = +5
     * percentage points) from [level] tiers of Gem Efficiency bought —
     * added to [GEM_INCOME_BONUS_PER_GEM] inside [gemIncomeMultiplier].
     */
    fun bonusPerGem(level: Int): Double =
        upgradeTotalPercent(level, PHASES, PER_GEM_BONUS_PHASE_1, PER_GEM_BONUS_PHASE_2, PER_GEM_BONUS_PHASE_3)
}
