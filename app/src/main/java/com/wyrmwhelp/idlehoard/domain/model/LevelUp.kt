package com.wyrmwhelp.idlehoard.domain.model

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Level Up is this game's prestige mechanic: reset the current run — Gold
 * Pieces and every owned lair, back to the same single starting Kobold
 * Warren a brand-new save begins with (see `GameEngine.performLevelUp`) —
 * in exchange for a fresh batch of Gems (see [gemIncomeMultiplier]'s
 * account-wide income bonus). Platinum Pieces and the Boosts bought with
 * it are the only *permanent* progress here — see `GameEngine.performLevelUp`
 * for exactly what carries over.
 *
 * **Gems are deliberately temporary, not accumulated.** AdVenture
 * Capitalist's real Angel Investors are the model for the formula
 * ([GameState.lifetimeGoldEarned] standing in for its "Total Earnings"),
 * but not for the persistence: this game's Gems *replace* whatever batch
 * the player already had rather than adding to it — a bigger batch means
 * a bigger head start on *this* run's income (via [gemIncomeMultiplier]),
 * not a stockpile that grows forever. That head start is what will matter
 * once a leaderboard exists to compare how fast players ramp up, not how
 * many Gems anyone has banked.
 *
 * `GameState.gemsEarnedFromLevelUp()` is `floor(150 * sqrt(lifetimeGoldEarned
 * / 10^15))` — since [GameState.lifetimeGoldEarned] never resets and only
 * ever grows, this number only ever grows too: leveling up can never hand
 * back *fewer* Gems than the player already had, and leveling up twice in a
 * row without any new lifetime earnings in between simply regrants the same
 * size batch (replacing the identical one, so nothing is lost or gained
 * either way) rather than compounding into something bigger.
 *
 * A minimum batch size blocks the action outright whenever it wouldn't be
 * worth resetting for: [MIN_GEMS_PER_FIRST_LEVEL_UP] (50) for the very
 * first Level Up ([GameState.totalLevelUps] `== 0` — a bigger bar so that
 * milestone is meaningful, not a reset for 1 or 2 Gems), and the smaller
 * [MIN_GEMS_PER_RECURRING_LEVEL_UP] (25) for every one after — in practice
 * this second bar rarely binds once the first has already been cleared,
 * since the batch size only ever grows from there, but it stays in place
 * as a floor regardless. Clearing whichever minimum applies grants the
 * *entire* batch, never just the minimum itself.
 */
private const val GEM_FORMULA_COEFFICIENT = 150.0
private const val LIFETIME_EARNINGS_DIVISOR = 1_000_000_000_000_000.0 // 10^15
private const val GEM_INCOME_BONUS_PER_GEM = 0.02
private const val MIN_GEMS_PER_FIRST_LEVEL_UP = 50L
private const val MIN_GEMS_PER_RECURRING_LEVEL_UP = 25L

/**
 * The Gem batch a Level Up would grant right now — `floor(150 *
 * sqrt(lifetimeGoldEarned / 10^15))`, gated by [MIN_GEMS_PER_FIRST_LEVEL_UP]
 * if this would be the player's very first Level Up
 * ([GameState.totalLevelUps] `== 0`) or [MIN_GEMS_PER_RECURRING_LEVEL_UP]
 * otherwise — a batch smaller than whichever minimum applies reports 0
 * rather than a token payout. 0 means "can't Level Up yet";
 * `GameEngine.performLevelUp` and the Level Up screen both treat that as
 * blocking the action entirely, not performing a reset for nothing. This
 * *replaces* [GameState.gems] rather than adding to it — see this file's
 * class doc for why Gems are temporary rather than accumulated.
 */
fun GameState.gemsEarnedFromLevelUp(): Long {
    val totalGems = floor(GEM_FORMULA_COEFFICIENT * sqrt(lifetimeGoldEarned / LIFETIME_EARNINGS_DIVISOR)).toLong()
    val minimum = if (totalLevelUps == 0) MIN_GEMS_PER_FIRST_LEVEL_UP else MIN_GEMS_PER_RECURRING_LEVEL_UP
    return if (totalGems < minimum) 0L else totalGems
}

/**
 * The income bonus from [gems] currently held — each Gem is worth a flat
 * +2%, additive rather than compounding (unlike the Platinum-bought Profit
 * Boost in `Boosts.kt`) — feeds into `CreatureLair.incomePerCycle` alongside
 * the milestone and Profit Boost multipliers. Temporary by construction:
 * since [gems] itself resets every Level Up (see this file's class doc),
 * so does this bonus — it's a head start for the current run, not a
 * permanent account-wide upgrade.
 */
fun gemIncomeMultiplier(gems: Long): Double = 1.0 + gems * GEM_INCOME_BONUS_PER_GEM
