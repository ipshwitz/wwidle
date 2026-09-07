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
 * back *fewer* Gems than the player already had.
 *
 * A minimum batch size blocks the action outright whenever it wouldn't be
 * worth resetting for: [MIN_GEMS_PER_FIRST_LEVEL_UP] (50) for the very
 * first Level Up ([GameState.totalLevelUps] `== 0` — a bigger bar so that
 * milestone is meaningful, not a reset for 1 or 2 Gems), and the smaller
 * [MIN_GEMS_PER_RECURRING_LEVEL_UP] (25) for every one after. Clearing
 * whichever minimum applies grants the *entire* batch, never just the
 * minimum itself.
 *
 * **[canLevelUp] additionally requires the new batch to beat the one
 * already held (v0.36.0, a correction to the earlier design above).**
 * Since `lifetimeGoldEarned` doesn't move between rapid taps,
 * `gemsEarnedFromLevelUp()` alone would keep returning the same
 * already-cleared-the-minimum batch forever after the first successful
 * Level Up — letting a player Level Up over and over with zero new
 * progress, each tap wiping the current run's Gold and lairs for a Gem
 * batch no bigger than what they already had. Requiring the fresh batch
 * to be strictly greater than [GameState.gems] closes that: a repeat
 * Level Up now only becomes available again once enough *new* lifetime
 * earnings have pushed the formula's result past what's already banked.
 */
private const val GEM_FORMULA_COEFFICIENT = 150.0
private const val LIFETIME_EARNINGS_DIVISOR = 1_000_000_000_000_000.0 // 10^15

/** Base per-Gem income bonus rate before any Gem Efficiency upgrade (see `GemUpgrades.kt`) is applied on top. */
const val GEM_INCOME_BONUS_PER_GEM = 0.02
private const val MIN_GEMS_PER_FIRST_LEVEL_UP = 50L
private const val MIN_GEMS_PER_RECURRING_LEVEL_UP = 25L

/**
 * The raw formula result (`floor(150 * sqrt(lifetimeGoldEarned / 10^15))`)
 * *before* the minimum-batch gate in [gemsEarnedFromLevelUp] is applied —
 * unlike that function (which reports a flat 0 below the minimum), this
 * keeps climbing the whole time, so it's what the Level Up screen's
 * progress bar shows even before a Level Up is actually allowed. A bare
 * "0" gives the player no sense of how close they are; this does.
 */
fun GameState.rawGemsFromLevelUpFormula(): Long =
    floor(GEM_FORMULA_COEFFICIENT * sqrt(lifetimeGoldEarned / LIFETIME_EARNINGS_DIVISOR)).toLong()

/**
 * The minimum Gem batch needed before a Level Up is allowed at all —
 * [MIN_GEMS_PER_FIRST_LEVEL_UP] for the very first
 * ([GameState.totalLevelUps] `== 0`) or [MIN_GEMS_PER_RECURRING_LEVEL_UP]
 * for every one after. Paired with [rawGemsFromLevelUpFormula] by the
 * Level Up screen to show "X / Y Gems" progress toward whichever bar
 * currently applies.
 */
fun GameState.minGemsForLevelUp(): Long =
    if (totalLevelUps == 0) MIN_GEMS_PER_FIRST_LEVEL_UP else MIN_GEMS_PER_RECURRING_LEVEL_UP

/**
 * The Gem batch a Level Up would grant right now — [rawGemsFromLevelUpFormula],
 * gated by [minGemsForLevelUp] — a batch smaller than whichever minimum
 * applies reports 0 rather than a token payout. 0 means "can't Level Up
 * yet"; `GameEngine.performLevelUp` and the Level Up screen both treat
 * that as blocking the action entirely, not performing a reset for
 * nothing. This *replaces* [GameState.gems] rather than adding to it —
 * see this file's class doc for why Gems are temporary rather than
 * accumulated.
 */
fun GameState.gemsEarnedFromLevelUp(): Long {
    val totalGems = rawGemsFromLevelUpFormula()
    val minimum = minGemsForLevelUp()
    return if (totalGems < minimum) 0L else totalGems
}

/**
 * Whether a Level Up is actually allowed right now — not just
 * [gemsEarnedFromLevelUp] clearing its minimum, but the resulting batch
 * being *strictly bigger* than [GameState.gems] already held. Without this,
 * a player who just Leveled Up could immediately Level Up again and again:
 * [GameState.lifetimeGoldEarned] doesn't change between rapid taps, so
 * [gemsEarnedFromLevelUp] keeps returning the exact same batch — always
 * `> 0` once the minimum has been cleared once — which would otherwise let
 * every repeat tap wipe the current run's Gold and lairs for a Gem batch
 * that's no bigger than the one already banked. Requiring genuinely new
 * lifetime earnings since the last Level Up (or, for a fresh save with
 * `gems == 0`, simply clearing the first-time minimum) closes that gap
 * while still never blocking a Level Up that would actually pay out more.
 */
fun GameState.canLevelUp(): Boolean = gemsEarnedFromLevelUp() > gems

/**
 * The income bonus from [gems] currently held — each Gem is worth a flat
 * [GEM_INCOME_BONUS_PER_GEM] (2%) plus whatever `GemUpgrades.bonusPerGem`
 * adds on top for [gemEfficiencyLevel] tiers bought, additive rather than
 * compounding (unlike the Platinum-bought Profit Boost in `Boosts.kt`) —
 * feeds into `CreatureLair.incomePerCycle` alongside the milestone and
 * Profit Boost multipliers. [platinumGemPercentMultiplier]
 * (`GameState.permanentGemPercentMultiplier`, from the permanent Platinum
 * Gem % boost tiers in `Boosts.kt`) multiplies the whole per-Gem rate —
 * unlike [gems]/[gemEfficiencyLevel], it's Platinum-funded and permanent
 * through a Level Up, so it keeps applying to whatever the *next* run's
 * fresh Gem batch is worth too. Temporary by construction otherwise: since
 * [gems] itself resets every Level Up (see this file's class doc) and so
 * does [gemEfficiencyLevel] (`GemUpgrades.kt`), the bulk of this bonus is a
 * head start for the current run, not a permanent account-wide upgrade —
 * only the multiplier on top of it is.
 */
fun gemIncomeMultiplier(gems: Long, gemEfficiencyLevel: Int = 0, platinumGemPercentMultiplier: Double = 1.0): Double =
    1.0 + gems * (GEM_INCOME_BONUS_PER_GEM + GemUpgrades.bonusPerGem(gemEfficiencyLevel)) * platinumGemPercentMultiplier
