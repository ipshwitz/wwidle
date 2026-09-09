package com.wyrmwhelp.idlehoard.domain.model

/**
 * A special account-wide Steward, earned once by watching
 * [UNIVERSAL_STEWARD_AD_THRESHOLD] rewarded ads in total — across *every*
 * rewarded placement (Welcome Back's "Watch Ad to Double", the Shop's
 * Platinum ad, and both Speed/Income ad-boosts — see
 * `GameEngine.recordAdWatched`, called from all four of
 * `GameViewModel`'s `onRewardEarned` callbacks), not any one of them
 * specifically. [GameState.totalAdsWatched] only ever grows and is
 * carried across a Level Up explicitly (`GameEngine.performLevelUp`) —
 * it's a one-time account milestone, not run progress, the same
 * treatment Platinum Pieces and the permanent boost tiers get.
 *
 * It doesn't have its own multiplier or upgrade tiers ("it doesn't
 * level") — earning it just means every *owned* lair auto-collects
 * continuously from then on, exactly as if it had a real Steward hired,
 * without ever paying that lair's own `stewardCostGp` again. It only
 * takes effect on a lair once at least one unit is actually owned — it
 * doesn't retroactively claim anything, and does nothing for a lair tier
 * the player hasn't bought into at all yet. "Stacks with other
 * Stewards" in the sense that a lair with both a real, already-hired
 * Steward *and* this one is simply managed either way ([isLairManaged]
 * is an `||`, not a numeric stack) — hiring one first and earning this
 * later, or vice versa, never conflicts or wastes anything.
 */
const val UNIVERSAL_STEWARD_AD_THRESHOLD = 100

/** Whether the account-wide Universal Steward has been earned — see this file's class doc. */
fun GameState.hasUniversalSteward(): Boolean = totalAdsWatched >= UNIVERSAL_STEWARD_AD_THRESHOLD

/**
 * Whether [owned] is auto-collected right now — a real per-lair Steward,
 * or the account-wide Universal Steward once at least one unit of it is
 * owned. The one check every "is this lair managed" call site
 * (`GameEngine`'s tick loop, `startLairLoad`, the UI's tap-target/dim
 * logic) should use instead of reading [OwnedLair.hasSteward] alone.
 */
fun GameState.isLairManaged(owned: OwnedLair): Boolean = owned.hasSteward || (owned.count > 0 && hasUniversalSteward())

/**
 * Ads watched toward the Universal Steward, clamped to
 * [UNIVERSAL_STEWARD_AD_THRESHOLD] — what the progress UI shows ("X /
 * 100 ads watched"), so it reads "100 / 100" forever afterward rather
 * than an ever-climbing raw count.
 */
fun GameState.adsWatchedTowardUniversalSteward(): Int = totalAdsWatched.coerceAtMost(UNIVERSAL_STEWARD_AD_THRESHOLD)
