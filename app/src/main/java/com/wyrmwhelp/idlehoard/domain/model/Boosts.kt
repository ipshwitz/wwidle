package com.wyrmwhelp.idlehoard.domain.model

/**
 * Permanent, account-wide bonuses purchased with Platinum Pieces (see
 * `GameState.platinumPieces`, `ui/shop/ShopContent.kt`) — unlike the
 * ownership milestones (`Milestone.kt`), these aren't tied to any one lair.
 * Each level compounds with the last and costs more Platinum than the one
 * before it, the same closed-form cost-growth shape lairs themselves use
 * (`CreatureLair.costForNextUnit`) — just against Platinum instead of Gold,
 * and one level at a time rather than in bulk.
 *
 * Tuning numbers here are first-pass placeholders, same as everywhere else
 * in the economy — not yet playtested.
 */
private const val SPEED_BOOST_BASE_COST_PP = 10.0
private const val SPEED_BOOST_COST_GROWTH = 1.5
private const val SPEED_BOOST_PER_LEVEL = 1.05

private const val PROFIT_BOOST_BASE_COST_PP = 10.0
private const val PROFIT_BOOST_COST_GROWTH = 1.5
private const val PROFIT_BOOST_PER_LEVEL = 1.10

/** Platinum Pieces to buy the next speed-boost level, given [currentLevel] already bought. */
fun speedBoostCost(currentLevel: Int): Double =
    SPEED_BOOST_BASE_COST_PP * Math.pow(SPEED_BOOST_COST_GROWTH, currentLevel.toDouble())

/**
 * The compounding production-speed multiplier from [level] speed boosts
 * (each one 5% faster) — divide a lair's `baseProductionSeconds` by this to
 * get its effective (shorter) cycle time; see
 * [CreatureLair.effectiveProductionSeconds].
 */
fun speedBoostMultiplier(level: Int): Double = Math.pow(SPEED_BOOST_PER_LEVEL, level.toDouble())

/** Platinum Pieces to buy the next profit-boost level, given [currentLevel] already bought. */
fun profitBoostCost(currentLevel: Int): Double =
    PROFIT_BOOST_BASE_COST_PP * Math.pow(PROFIT_BOOST_COST_GROWTH, currentLevel.toDouble())

/**
 * The compounding income multiplier from [level] profit boosts (each one
 * 10% more) — feeds into [CreatureLair.incomePerCycle] alongside the
 * milestone multipliers.
 */
fun profitBoostMultiplier(level: Int): Double = Math.pow(PROFIT_BOOST_PER_LEVEL, level.toDouble())

/** Platinum Pieces to buy one time skip (see `GameEngine.purchaseTimeSkip`). */
const val TIME_SKIP_COST_PP = 5.0

/** Real-world seconds one time skip instantly grants. */
const val TIME_SKIP_SECONDS = 3_600.0
