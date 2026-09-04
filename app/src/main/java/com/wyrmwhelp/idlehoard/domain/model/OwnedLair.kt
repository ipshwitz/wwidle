package com.wyrmwhelp.idlehoard.domain.model

/**
 * Player-owned state for one [CreatureLair]. Absence of an entry for a lair id
 * (in [GameState.lairs]) is equivalent to an [OwnedLair] with [count] 0 — the
 * player hasn't claimed that lair yet.
 *
 * @property lairId Foreign key into the [CreatureLair] catalog.
 * @property count Number of units of this lair the player has claimed.
 * @property hasSteward Whether a Steward is hired, auto-collecting finished cycles.
 * @property cycleProgressSeconds Seconds elapsed in the current production cycle.
 * @property isReadyToCollect True once a cycle has finished but hasn't been
 *   collected yet (only relevant when [hasSteward] is false — production pauses,
 *   full and waiting, until the player taps to collect it).
 */
data class OwnedLair(
    val lairId: String,
    val count: Int = 0,
    val hasSteward: Boolean = false,
    val cycleProgressSeconds: Double = 0.0,
    val isReadyToCollect: Boolean = false,
)
