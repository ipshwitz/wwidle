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
