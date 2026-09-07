package com.wyrmwhelp.idlehoard.domain.model

import java.time.Duration
import java.time.Instant

/**
 * The Shop's "Watch an Ad" rewarded placement (see `ads/AdManager.kt`'s
 * `RewardedPlacement.SHOP_PLATINUM`) — earns a flat amount of Platinum
 * Pieces, gated by a cooldown tracked on [GameState.lastPlatinumAdWatchedAt]
 * rather than anything ad-network-side, so it persists across sessions the
 * same way the rest of the save does.
 */
const val PLATINUM_AD_REWARD_PP = 2.0

/** Minimum time between Shop ad-for-Platinum watches. */
val PLATINUM_AD_COOLDOWN: Duration = Duration.ofHours(24)

/** Whether the Shop's "Watch an Ad" reward is available again at [now]. */
fun GameState.canWatchPlatinumAd(now: Instant = Instant.now()): Boolean =
    platinumAdCooldownRemaining(now).isZero

/**
 * How much longer until the Shop's "Watch an Ad" reward is available again,
 * or [Duration.ZERO] if it already is (including if it's never been
 * watched at all).
 */
fun GameState.platinumAdCooldownRemaining(now: Instant = Instant.now()): Duration {
    val lastWatched = lastPlatinumAdWatchedAt ?: return Duration.ZERO
    val remaining = Duration.between(now, lastWatched.plus(PLATINUM_AD_COOLDOWN))
    return if (remaining.isNegative) Duration.ZERO else remaining
}

/**
 * The Shop's ad-watch reward for a temporary Speed boost (see
 * `ads/AdManager.kt`'s `RewardedPlacement.SPEED_BOOST`) — grants an
 * [ActiveTemporaryBoost] (`domain/model/Boosts.kt`) exactly like a
 * PP-bought temporary boost, so it stacks multiplicatively with any other
 * running Speed boost the same way. Unlike a single-cooldown reward (see
 * [PLATINUM_AD_COOLDOWN] above), this one has [SPEED_BOOST_AD_MAX_SLOTS]
 * *independent* daily slots — confirmed design ("4 independent daily
 * slots... each on its own 24-hour cooldown") rather than one shared
 * cooldown, specifically so watching all 4 back-to-back stacks four
 * concurrent 2x boosts (16x total) instead of only ever allowing one at a
 * time (which a single 24h cooldown would, since the boost's own 4-hour
 * duration is far shorter than the cooldown). [GameState.speedBoostAdWatchTimestamps]
 * only needs to record *when* each of the last watches happened — a slot
 * is "independent" only in the sense that each watch's own 24-hour timer
 * runs from its own timestamp, not because any watch is tied to a fixed
 * slot number; functionally identical, but avoids needing to track which
 * literal slot (1-4) a watch occupied.
 */
const val SPEED_BOOST_AD_MULTIPLIER = 2.0

/** How long the Shop's ad-watch Speed boost runs once granted. */
val SPEED_BOOST_AD_DURATION: Duration = Duration.ofHours(4)

/** How many of these daily ad-watch slots exist — see [SPEED_BOOST_AD_MULTIPLIER]'s doc. */
const val SPEED_BOOST_AD_MAX_SLOTS = 4

/** Minimum time before any one watch's own slot frees up again. */
val SPEED_BOOST_AD_COOLDOWN: Duration = Duration.ofHours(24)

/** [GameState.speedBoostAdWatchTimestamps] entries still within their own 24-hour cooldown at [now]. */
private fun GameState.activeSpeedBoostAdWatches(now: Instant): List<Instant> =
    speedBoostAdWatchTimestamps.filter { Duration.between(it, now) < SPEED_BOOST_AD_COOLDOWN }

/** How many of the [SPEED_BOOST_AD_MAX_SLOTS] daily ad-watch slots are free right now. */
fun GameState.availableSpeedBoostAdSlots(now: Instant = Instant.now()): Int =
    (SPEED_BOOST_AD_MAX_SLOTS - activeSpeedBoostAdWatches(now).size).coerceAtLeast(0)

/** Whether at least one daily ad-watch slot for the Speed boost is free right now. */
fun GameState.canWatchSpeedBoostAd(now: Instant = Instant.now()): Boolean = availableSpeedBoostAdSlots(now) > 0

/**
 * How much longer until the *next* daily ad-watch slot frees up, or
 * [Duration.ZERO] if one already is — the soonest of the currently-busy
 * slots' own 24-hour cooldowns to expire.
 */
fun GameState.speedBoostAdCooldownRemaining(now: Instant = Instant.now()): Duration {
    val active = activeSpeedBoostAdWatches(now)
    if (active.size < SPEED_BOOST_AD_MAX_SLOTS) return Duration.ZERO
    val remaining = Duration.between(now, active.min().plus(SPEED_BOOST_AD_COOLDOWN))
    return if (remaining.isNegative) Duration.ZERO else remaining
}

/**
 * The second ad-watch reward (v0.34.0) offered alongside the Speed one —
 * a temporary Income (Profit) boost instead, same
 * [SPEED_BOOST_AD_MAX_SLOTS]-independent-daily-slots shape, just its own
 * multiplier/duration and its own [GameState.incomeBoostAdWatchTimestamps]
 * ledger so the two ad types' cooldowns never interfere with each other.
 * Both are offered from the same `ui/game/QuickAdBoostButton.kt` popup on
 * the main game screen (not the Shop — moved out of there in v0.34.0 so
 * both ad types live in exactly one place).
 */
const val INCOME_BOOST_AD_MULTIPLIER = 2.0

/** How long the ad-watch Income boost runs once granted. */
val INCOME_BOOST_AD_DURATION: Duration = Duration.ofHours(2)

/** How many of these daily ad-watch slots exist — same shape as [SPEED_BOOST_AD_MAX_SLOTS]. */
const val INCOME_BOOST_AD_MAX_SLOTS = 4

/** Minimum time before any one Income-boost watch's own slot frees up again. */
val INCOME_BOOST_AD_COOLDOWN: Duration = Duration.ofHours(24)

/** [GameState.incomeBoostAdWatchTimestamps] entries still within their own 24-hour cooldown at [now]. */
private fun GameState.activeIncomeBoostAdWatches(now: Instant): List<Instant> =
    incomeBoostAdWatchTimestamps.filter { Duration.between(it, now) < INCOME_BOOST_AD_COOLDOWN }

/** How many of the [INCOME_BOOST_AD_MAX_SLOTS] daily ad-watch slots are free right now. */
fun GameState.availableIncomeBoostAdSlots(now: Instant = Instant.now()): Int =
    (INCOME_BOOST_AD_MAX_SLOTS - activeIncomeBoostAdWatches(now).size).coerceAtLeast(0)

/** Whether at least one daily ad-watch slot for the Income boost is free right now. */
fun GameState.canWatchIncomeBoostAd(now: Instant = Instant.now()): Boolean = availableIncomeBoostAdSlots(now) > 0

/**
 * How much longer until the *next* daily Income-boost ad-watch slot frees
 * up, or [Duration.ZERO] if one already is.
 */
fun GameState.incomeBoostAdCooldownRemaining(now: Instant = Instant.now()): Duration {
    val active = activeIncomeBoostAdWatches(now)
    if (active.size < INCOME_BOOST_AD_MAX_SLOTS) return Duration.ZERO
    val remaining = Duration.between(now, active.min().plus(INCOME_BOOST_AD_COOLDOWN))
    return if (remaining.isNegative) Duration.ZERO else remaining
}
