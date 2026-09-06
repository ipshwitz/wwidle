package com.wyrmwhelp.idlehoard.domain.model

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Level Up is this game's prestige mechanic: reset the current run — Gold
 * Pieces and every owned lair, back to the same single starting Kobold
 * Warren a brand-new save begins with (see `GameEngine.performLevelUp`) —
 * in exchange for Gems, a permanent currency whose only effect so far is
 * [gemIncomeMultiplier]'s account-wide income bonus. Everything *not* tied
 * to the gold economy (Platinum Pieces, the boosts bought with it,
 * [GameState.offlineCapHours], the ad cooldown) survives a Level Up — see
 * `GameEngine.performLevelUp` for exactly what carries over.
 *
 * Tuning numbers here are first-pass placeholders, same as everywhere else
 * in the economy (see `Boosts.kt`) — not yet playtested.
 */
private const val GEMS_NET_WORTH_DIVISOR = 1_000_000.0
private const val GEM_INCOME_BONUS_PER_GEM = 0.02

/**
 * Gems earned by Leveling Up right now, from [GameState.estimatedNetWorth] —
 * `floor(sqrt(netWorth / GEMS_NET_WORTH_DIVISOR))`, the classic square-root
 * prestige curve: early Level Ups are cheap, later ones need dramatically
 * more net worth for the same Gem reward. A save too early to earn even one
 * Gem returns 0 — `GameEngine.performLevelUp`/the Level Up screen both treat
 * that as "can't Level Up yet" rather than performing a reset for nothing.
 */
fun GameState.gemsEarnedFromLevelUp(): Long {
    val netWorth = estimatedNetWorth()
    if (netWorth <= 0.0) return 0L
    return floor(sqrt(netWorth / GEMS_NET_WORTH_DIVISOR)).toLong().coerceAtLeast(0L)
}

/**
 * The permanent income bonus from [gems] earned across every past Level
 * Up — each Gem is worth a flat +2%, additive rather than compounding
 * (unlike the Platinum-bought Profit Boost in `Boosts.kt`) — feeds into
 * `CreatureLair.incomePerCycle` alongside the milestone and Profit Boost
 * multipliers.
 */
fun gemIncomeMultiplier(gems: Long): Double = 1.0 + gems * GEM_INCOME_BONUS_PER_GEM
