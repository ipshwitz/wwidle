package com.wyrmwhelp.idlehoard.domain.model

/**
 * Player-owned state for one [CreatureLair]. Absence of an entry for a lair id
 * (in [GameState.lairs]) is equivalent to an [OwnedLair] with [count] 0 — the
 * player hasn't claimed that lair yet.
 *
 * @property lairId Foreign key into the [CreatureLair] catalog.
 * @property count Number of units of this lair the player has claimed.
 * @property hasSteward Whether a Steward is hired, auto-collecting finished cycles
 *   continuously — [isLoading] is meaningless once this is true (see below).
 * @property cycleProgressSeconds Seconds elapsed in the current production cycle.
 *   For a lair without a Steward, this only advances while [isLoading] is true —
 *   an idle lair stays pinned at 0 rather than silently filling in the background.
 * @property isLoading Only meaningful when [hasSteward] is false: true once the
 *   player has tapped this lair to start its production cycle
 *   (`GameEngine.startLairLoad`), false while it's sitting idle waiting to be
 *   tapped. When a started cycle completes, the gold is credited automatically
 *   and this flips back to false — there's no separate "ready, waiting to be
 *   collected" state to tap through; the tap starts the cycle, not the collection.
 * @property completedLoads Counts every time this lair's manually-started cycle
 *   has completed and auto-collected (see [isLoading]) — a monotonic counter, not
 *   a boolean, so the UI can detect each individual completion (for the coin-burst
 *   effect) even if several happen in quick succession. Never incremented for
 *   Steward-managed cycles, which collect silently.
 * @property profitUpgradeLevel Tiers bought of this lair's own Gold Pieces
 *   Profit upgrade line (`domain/model/GpUpgrades.kt`) — boosts only this
 *   lair's income, unlike the account-wide "Everything Profit" line on
 *   [GameState.everythingProfitUpgradeLevel]. Resets on a Level Up
 *   implicitly, since [GameState.lairs] itself resets to the starting map.
 * @property speedUpgradeLevel Tiers bought of this lair's own Gold Pieces
 *   Speed upgrade line, same shape as [profitUpgradeLevel] but for cycle
 *   time instead of income.
 */
data class OwnedLair(
    val lairId: String,
    val count: Int = 0,
    val hasSteward: Boolean = false,
    val cycleProgressSeconds: Double = 0.0,
    val isLoading: Boolean = false,
    val completedLoads: Int = 0,
    val profitUpgradeLevel: Int = 0,
    val speedUpgradeLevel: Int = 0,
)
