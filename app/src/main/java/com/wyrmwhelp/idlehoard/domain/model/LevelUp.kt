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
 * [GameState.offlineCapHours], the ad cooldown, [GameState.lifetimeGoldEarned]
 * itself) survives a Level Up — see `GameEngine.performLevelUp` for exactly
 * what carries over.
 *
 * The Gem formula is AdVenture Capitalist's real Angel Investor formula,
 * carried over 1:1 (Gold Pieces standing in for AdCap's dollars, same as
 * the rest of this game's tier-0–9 balance — see `CreatureLairCatalog`):
 * the *target* total Gems your [GameState.lifetimeGoldEarned] "should" have
 * earned by now is `150 * sqrt(lifetimeGoldEarned / 10^15)`, and a Level Up
 * only ever grants the gap between that target and
 * [GameState.totalGemsEarned] — never a fresh amount computed from
 * scratch. This is a deliberate *stock*, not *flow*, formula: since
 * [GameState.lifetimeGoldEarned] never resets and only ever grows, the
 * target only moves forward when the player actually earns more lifetime
 * income than they had at their last Level Up — leveling up twice in a
 * row without earning anything new in between grants 0 Gems the second
 * time, which is exactly the "reach further before you can Level Up
 * again" gate this is meant to provide, without a separately-tracked
 * threshold to keep in sync with the reward itself.
 *
 * On top of that stock/flow gate, [MIN_GEMS_PER_FIRST_LEVEL_UP] sets a
 * floor on the very *first* Level Up specifically ([GameState.totalLevelUps]
 * `== 0`): it's blocked until the gap would be worth at least 50 Gems, so
 * the player's first prestige is a real milestone rather than a one-off,
 * barely-worth-it reset for 1 or 2 Gems. Every Level Up after that first
 * one goes back to the plain stock/flow rule above with no minimum batch
 * size — once the player has already cleared that first bar, a smaller
 * top-up gap is fine. Whenever the gap does clear whatever bar applies,
 * the *entire* gap is granted, never just the minimum.
 */
private const val GEM_FORMULA_COEFFICIENT = 150.0
private const val LIFETIME_EARNINGS_DIVISOR = 1_000_000_000_000_000.0 // 10^15
private const val GEM_INCOME_BONUS_PER_GEM = 0.02
private const val MIN_GEMS_PER_FIRST_LEVEL_UP = 50L

/**
 * Gems a Level Up would earn right now — the target total Gems implied by
 * [GameState.lifetimeGoldEarned] minus [GameState.totalGemsEarned] already
 * earned (never negative — a target that's fallen behind `totalGemsEarned`
 * is impossible in practice since both only ever grow, but not a case
 * worth crashing over, so it simply grants nothing rather than subtracting
 * Gems), floored to a whole number. If this would be the player's very
 * first Level Up ([GameState.totalLevelUps] `== 0`), the gap is also
 * gated by [MIN_GEMS_PER_FIRST_LEVEL_UP] — smaller than that reports 0
 * rather than a token first prestige; every Level Up after the first has
 * no such minimum. 0 means "can't Level Up yet"; `GameEngine.performLevelUp`
 * and the Level Up screen both treat that as blocking the action entirely,
 * not performing a reset for nothing.
 */
fun GameState.gemsEarnedFromLevelUp(): Long {
    val targetTotalGems = floor(GEM_FORMULA_COEFFICIENT * sqrt(lifetimeGoldEarned / LIFETIME_EARNINGS_DIVISOR)).toLong()
    val gap = (targetTotalGems - totalGemsEarned).coerceAtLeast(0L)
    return if (totalLevelUps == 0 && gap < MIN_GEMS_PER_FIRST_LEVEL_UP) 0L else gap
}

/**
 * The permanent income bonus from [gems] currently held — each Gem is
 * worth a flat +2%, additive rather than compounding (unlike the
 * Platinum-bought Profit Boost in `Boosts.kt`) — feeds into
 * `CreatureLair.incomePerCycle` alongside the milestone and Profit Boost
 * multipliers. Reads [GameState.gems] (the spendable balance), not
 * [GameState.totalGemsEarned] — if a future system lets the player spend
 * Gems, spent Gems stop contributing to this bonus, same as spending gold
 * stops it from sitting in the bank.
 */
fun gemIncomeMultiplier(gems: Long): Double = 1.0 + gems * GEM_INCOME_BONUS_PER_GEM
