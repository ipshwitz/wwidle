package com.wyrmwhelp.idlehoard.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Platinum-Pieces-funded power, permanent through a Level Up unlike
 * everything Gold/Gem-funded (`GpUpgrades.kt`/`GemUpgrades.kt`) — see
 * `GameEngine.performLevelUp`, which explicitly carries every field on
 * this page forward. Two shapes:
 *
 * - **Permanent boosts** ([PermanentBoostTier]/[PERMANENT_SPEED_TIERS]/
 *   [PERMANENT_PROFIT_TIERS]/[PERMANENT_GEM_TIERS]) — named, discrete
 *   multiplier tiers (2x/5x/10x Speed, 1.5x/2x/5x Profit, 1.5x/2x/5x Gem
 *   percentage) rather than a smooth per-level percentage. Each tier is
 *   independently repurchasable any number of times — buying "5x Speed"
 *   three times contributes `5^3 = 125x` from that tier alone, stacking
 *   multiplicatively both with itself and with every other tier bought in
 *   that category (see [GameState.permanentSpeedMultiplier]) — an
 *   explicit design choice ("so they could purchase and apply 3 5x
 *   speeds and have them stack"), with the growing cost per repeat
 *   purchase ([costForPermanentBoostPurchase]) the intended brake on
 *   stacking a single tier indefinitely.
 * - **Temporary boosts** ([TemporaryBoostOption]/[TEMPORARY_BOOST_OPTIONS]) —
 *   a fixed-price, instant-activation consumable: buying one immediately
 *   starts a [ActiveTemporaryBoost] running for its stated duration.
 *   Buying a second one of the same category before the first expires
 *   stacks multiplicatively for as long as both are running (confirmed
 *   design — "stack multiplicatively while both run"), which is why
 *   [GameState.activeTemporaryBoosts] is a list of independently-expiring
 *   instances rather than a single "level + one expiry" pair.
 *
 * These replace the original Speed Boost/Profit Boost design (a single
 * compounding %-per-level line, same shape as the Gold/Gem upgrade
 * lines) — that design is gone entirely, not layered underneath this one.
 *
 * Tuning numbers here are first-pass placeholders, same as everywhere
 * else in the economy — not yet playtested.
 */
enum class PermanentBoostCategory { SPEED, PROFIT, GEM_PERCENT }

/**
 * One named, repurchasable permanent-boost tier. [multiplier] is this
 * tier's own per-purchase factor (e.g. 5.0 for "5x Speed") — buying it
 * [n] times contributes `multiplier^n` to its category's combined
 * multiplier (see [GameState.permanentSpeedMultiplier]/
 * [GameState.permanentProfitMultiplier]/[GameState.permanentGemPercentMultiplier]).
 * [basePp]/[costGrowthRate] feed [costForPermanentBoostPurchase] with the
 * same closed-form per-purchase cost curve the original Speed/Profit
 * Boost design used, just against a much steeper growth rate — since a
 * single tier can be bought unboundedly many times, the escalating cost
 * (not a level cap) is what keeps stacking one tier forever from being
 * the obviously-correct play.
 */
data class PermanentBoostTier(
    val category: PermanentBoostCategory,
    val multiplier: Double,
    val basePp: Double,
    val costGrowthRate: Double,
)

/** Permanent, account-wide production-speed multipliers — see [GameState.permanentSpeedMultiplier]. */
val PERMANENT_SPEED_TIERS: List<PermanentBoostTier> = listOf(
    PermanentBoostTier(PermanentBoostCategory.SPEED, multiplier = 2.0, basePp = 5.0, costGrowthRate = 1.6),
    PermanentBoostTier(PermanentBoostCategory.SPEED, multiplier = 5.0, basePp = 20.0, costGrowthRate = 1.7),
    PermanentBoostTier(PermanentBoostCategory.SPEED, multiplier = 10.0, basePp = 60.0, costGrowthRate = 1.8),
)

/** Permanent, account-wide income multipliers — see [GameState.permanentProfitMultiplier]. */
val PERMANENT_PROFIT_TIERS: List<PermanentBoostTier> = listOf(
    PermanentBoostTier(PermanentBoostCategory.PROFIT, multiplier = 1.5, basePp = 5.0, costGrowthRate = 1.5),
    PermanentBoostTier(PermanentBoostCategory.PROFIT, multiplier = 2.0, basePp = 12.0, costGrowthRate = 1.6),
    PermanentBoostTier(PermanentBoostCategory.PROFIT, multiplier = 5.0, basePp = 40.0, costGrowthRate = 1.7),
)

/**
 * Permanent multipliers on the per-Gem income *percentage* itself (see
 * [gemIncomeMultiplier]'s `platinumGemPercentMultiplier` param) — distinct
 * from, and stacking on top of, the Gem-funded Gem Efficiency upgrade
 * (`GemUpgrades.kt`), which resets on Level Up while this doesn't.
 */
val PERMANENT_GEM_TIERS: List<PermanentBoostTier> = listOf(
    PermanentBoostTier(PermanentBoostCategory.GEM_PERCENT, multiplier = 1.5, basePp = 8.0, costGrowthRate = 1.6),
    PermanentBoostTier(PermanentBoostCategory.GEM_PERCENT, multiplier = 2.0, basePp = 20.0, costGrowthRate = 1.7),
    PermanentBoostTier(PermanentBoostCategory.GEM_PERCENT, multiplier = 5.0, basePp = 60.0, costGrowthRate = 1.8),
)

/** Every permanent boost tier the Shop sells, all categories together — for UI iteration. */
val ALL_PERMANENT_BOOST_TIERS: List<PermanentBoostTier> = PERMANENT_SPEED_TIERS + PERMANENT_PROFIT_TIERS + PERMANENT_GEM_TIERS

/** Platinum Pieces to buy one more copy of [tier], given [currentLevel] copies already bought. */
fun costForPermanentBoostPurchase(tier: PermanentBoostTier, currentLevel: Int): Double =
    tier.basePp * Math.pow(tier.costGrowthRate, currentLevel.toDouble())

/**
 * How many copies of [tier] have been bought so far. [tier] must be one of
 * [PERMANENT_SPEED_TIERS]/[PERMANENT_PROFIT_TIERS]/[PERMANENT_GEM_TIERS] —
 * matched by category and exact [PermanentBoostTier.multiplier], which is
 * safe since those are fixed literal constants, never computed values.
 */
fun GameState.permanentBoostLevel(tier: PermanentBoostTier): Int = when (tier.category) {
    PermanentBoostCategory.SPEED -> when (tier.multiplier) {
        2.0 -> permanentSpeedBoost2xLevel
        5.0 -> permanentSpeedBoost5xLevel
        10.0 -> permanentSpeedBoost10xLevel
        else -> error("Unknown permanent Speed tier ${tier.multiplier}x")
    }
    PermanentBoostCategory.PROFIT -> when (tier.multiplier) {
        1.5 -> permanentProfitBoost15xLevel
        2.0 -> permanentProfitBoost2xLevel
        5.0 -> permanentProfitBoost5xLevel
        else -> error("Unknown permanent Profit tier ${tier.multiplier}x")
    }
    PermanentBoostCategory.GEM_PERCENT -> when (tier.multiplier) {
        1.5 -> permanentGemBoost15xLevel
        2.0 -> permanentGemBoost2xLevel
        5.0 -> permanentGemBoost5xLevel
        else -> error("Unknown permanent Gem % tier ${tier.multiplier}x")
    }
}

/** Returns a copy of this state with [tier]'s purchase count set to [newLevel]. See [permanentBoostLevel]. */
fun GameState.withPermanentBoostLevel(tier: PermanentBoostTier, newLevel: Int): GameState = when (tier.category) {
    PermanentBoostCategory.SPEED -> when (tier.multiplier) {
        2.0 -> copy(permanentSpeedBoost2xLevel = newLevel)
        5.0 -> copy(permanentSpeedBoost5xLevel = newLevel)
        10.0 -> copy(permanentSpeedBoost10xLevel = newLevel)
        else -> error("Unknown permanent Speed tier ${tier.multiplier}x")
    }
    PermanentBoostCategory.PROFIT -> when (tier.multiplier) {
        1.5 -> copy(permanentProfitBoost15xLevel = newLevel)
        2.0 -> copy(permanentProfitBoost2xLevel = newLevel)
        5.0 -> copy(permanentProfitBoost5xLevel = newLevel)
        else -> error("Unknown permanent Profit tier ${tier.multiplier}x")
    }
    PermanentBoostCategory.GEM_PERCENT -> when (tier.multiplier) {
        1.5 -> copy(permanentGemBoost15xLevel = newLevel)
        2.0 -> copy(permanentGemBoost2xLevel = newLevel)
        5.0 -> copy(permanentGemBoost5xLevel = newLevel)
        else -> error("Unknown permanent Gem % tier ${tier.multiplier}x")
    }
}

/** Combined permanent production-speed multiplier from every 2x/5x/10x Speed tier bought, each compounding on its own purchase count. */
fun GameState.permanentSpeedMultiplier(): Double =
    PERMANENT_SPEED_TIERS.fold(1.0) { acc, tier -> acc * Math.pow(tier.multiplier, permanentBoostLevel(tier).toDouble()) }

/** Combined permanent income multiplier from every 1.5x/2x/5x Profit tier bought. */
fun GameState.permanentProfitMultiplier(): Double =
    PERMANENT_PROFIT_TIERS.fold(1.0) { acc, tier -> acc * Math.pow(tier.multiplier, permanentBoostLevel(tier).toDouble()) }

/** Combined permanent multiplier on the per-Gem income percentage from every 1.5x/2x/5x Gem % tier bought — see [gemIncomeMultiplier]. */
fun GameState.permanentGemPercentMultiplier(): Double =
    PERMANENT_GEM_TIERS.fold(1.0) { acc, tier -> acc * Math.pow(tier.multiplier, permanentBoostLevel(tier).toDouble()) }

/** Which production stat a temporary or active boost affects. */
enum class TemporaryBoostCategory { SPEED, PROFIT }

/**
 * One purchasable temporary-boost tier — a fixed-price, instant-activation
 * consumable (unlike [PermanentBoostTier], its cost never grows with repeat
 * purchases): buying it immediately starts a [durationSeconds]-long
 * [ActiveTemporaryBoost] at [multiplier].
 */
data class TemporaryBoostOption(
    val category: TemporaryBoostCategory,
    val multiplier: Double,
    val durationSeconds: Long,
    val costPp: Double,
)

/** Every temporary-boost tier the Shop sells. */
val TEMPORARY_BOOST_OPTIONS: List<TemporaryBoostOption> = listOf(
    TemporaryBoostOption(TemporaryBoostCategory.SPEED, multiplier = 50.0, durationSeconds = 300, costPp = 10.0),
    TemporaryBoostOption(TemporaryBoostCategory.SPEED, multiplier = 100.0, durationSeconds = 300, costPp = 25.0),
    TemporaryBoostOption(TemporaryBoostCategory.PROFIT, multiplier = 15.0, durationSeconds = 600, costPp = 10.0),
    TemporaryBoostOption(TemporaryBoostCategory.PROFIT, multiplier = 25.0, durationSeconds = 300, costPp = 20.0),
)

/**
 * One currently-running temporary boost, created by
 * `GameEngine.purchaseTemporaryBoost` and pruned once [expiresAt] passes
 * (see `GameEngine.advance`). Distinct instances rather than a single
 * "level" per category, specifically so two overlapping purchases (e.g.
 * buying the 100x Speed boost while the 50x one is still running) stack
 * multiplicatively for their overlap instead of one replacing the other.
 */
data class ActiveTemporaryBoost(
    val category: TemporaryBoostCategory,
    val multiplier: Double,
    val expiresAt: Instant,
)

/** The combined multiplier from every not-yet-expired boost in this list matching [category]. */
fun List<ActiveTemporaryBoost>.multiplierFor(category: TemporaryBoostCategory, now: Instant = Instant.now()): Double =
    asSequence()
        .filter { it.category == category && it.expiresAt.isAfter(now) }
        .fold(1.0) { acc, boost -> acc * boost.multiplier }

/** Every still-running temporary boost paired with its remaining [Duration], soonest-expiring first — for the Shop's "active" display. */
fun GameState.activeTemporaryBoostsRemaining(now: Instant = Instant.now()): List<Pair<ActiveTemporaryBoost, Duration>> =
    activeTemporaryBoosts
        .mapNotNull { boost ->
            val remaining = Duration.between(now, boost.expiresAt)
            if (remaining.isZero || remaining.isNegative) null else boost to remaining
        }
        .sortedBy { (_, remaining) -> remaining }

/**
 * Combined production-speed multiplier from every permanent Speed tier
 * plus every currently-running temporary Speed boost — feeds
 * [CreatureLair.effectiveProductionSeconds]'s `speedBoostMultiplier` param
 * exactly where the old per-level Speed Boost multiplier used to.
 */
fun GameState.platinumSpeedMultiplier(now: Instant = Instant.now()): Double =
    permanentSpeedMultiplier() * activeTemporaryBoosts.multiplierFor(TemporaryBoostCategory.SPEED, now)

/**
 * Combined income multiplier from every permanent Profit tier plus every
 * currently-running temporary Profit boost — feeds
 * [CreatureLair.incomePerCycle]'s `profitBoostMultiplier` param exactly
 * where the old per-level Profit Boost multiplier used to.
 */
fun GameState.platinumProfitMultiplier(now: Instant = Instant.now()): Double =
    permanentProfitMultiplier() * activeTemporaryBoosts.multiplierFor(TemporaryBoostCategory.PROFIT, now)

/**
 * One purchasable Time Skip tier — instantly grants [seconds] of production
 * (see `GameEngine.purchaseTimeSkip`) for [costPp] Platinum Pieces, using
 * the same [GameEngine] `advance()` logic offline earnings use.
 */
data class TimeSkipOption(val costPp: Double, val seconds: Double)

/** Every Time Skip tier the Shop currently sells, cheapest first. */
val TIME_SKIP_OPTIONS: List<TimeSkipOption> = listOf(
    TimeSkipOption(costPp = 2.0, seconds = 300.0), // 5 minutes
    TimeSkipOption(costPp = 8.0, seconds = 1_800.0), // 30 minutes
    TimeSkipOption(costPp = 15.0, seconds = 3_600.0), // 1 hour
    TimeSkipOption(costPp = 100.0, seconds = 43_200.0), // 12 hours
    TimeSkipOption(costPp = 180.0, seconds = 86_400.0), // 24 hours
    TimeSkipOption(costPp = 1_000.0, seconds = 604_800.0), // 7 days
)
