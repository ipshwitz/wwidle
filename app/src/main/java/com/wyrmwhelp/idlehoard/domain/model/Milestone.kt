package com.wyrmwhelp.idlehoard.domain.model

/**
 * One rung of the shared ownership-count milestone ladder: reaching
 * [threshold] units owned multiplies production by [multiplier], compounding
 * with every earlier rung already reached (see [milestoneMultiplierFor]).
 * The same ladder is used two ways — see [CreatureLair.individualMilestoneMultiplier]
 * (this lair's own owned count) and [GameState.globalMilestoneMultiplier]
 * ("Everything" — the *lowest* owned count across every lair, so it only
 * advances once every lair has caught up).
 */
data class MilestoneStep(val threshold: Int, val multiplier: Double)

val MILESTONE_STEPS: List<MilestoneStep> = listOf(
    MilestoneStep(25, 2.0),
    MilestoneStep(50, 2.0),
    MilestoneStep(100, 2.0),
    MilestoneStep(200, 2.0),
    MilestoneStep(300, 2.0),
    MilestoneStep(400, 2.0),
    MilestoneStep(500, 4.0),
    MilestoneStep(1_000, 5.0),
    MilestoneStep(5_000, 6.0),
    MilestoneStep(10_000, 7.0),
)

/**
 * The compounding multiplier from every [MILESTONE_STEPS] rung at or below
 * [unitsOwned] — e.g. 500 owned reaches the first seven rungs (25 through
 * 500), for 2^6 * 4 = 256x.
 */
fun milestoneMultiplierFor(unitsOwned: Int): Double =
    MILESTONE_STEPS.filter { unitsOwned >= it.threshold }.fold(1.0) { acc, step -> acc * step.multiplier }

/**
 * The smallest [MILESTONE_STEPS] threshold strictly greater than
 * [unitsOwned], or null once every rung has been passed (there's no rung
 * past 10,000 — callers past that point need their own fallback, e.g.
 * `BuyQuantity.NEXT`'s "round up to the next multiple of 10,000").
 */
fun nextMilestoneThreshold(unitsOwned: Int): Int? =
    MILESTONE_STEPS.map { it.threshold }.firstOrNull { unitsOwned < it }

/**
 * One milestone rung actually reached by a purchase, ready to show the
 * player as a pop-up (see `GameStateExtensions.milestonesCrossed` and
 * `GameViewModel.milestoneAnnouncement`). [lairName] is either a specific
 * lair's display name (an individual rung) or the literal "Everything" (a
 * global rung) — the same two labels `UnlocksContent` already groups by.
 */
data class MilestoneAnnouncement(
    val lairName: String,
    val threshold: Int,
    val multiplier: Double,
    val isGlobal: Boolean,
)
