package com.wyrmwhelp.idlehoard.domain.model

/**
 * What kind of bonus a [MilestoneStep] grants. Confirmed design: the first
 * six rungs (25 through 400, the repeated-doubling ones) shrink production
 * *time* — [SPEED]; the later, bigger flat-rate jumps (500 and up) instead
 * boost gold *earned* per cycle — [INCOME]. Both compound independently
 * within their own type (see [milestoneMultiplierFor]) rather than one
 * combined multiplier, since they now feed two different calculations
 * ([CreatureLair.effectiveProductionSeconds] vs [CreatureLair.incomePerCycle]).
 */
enum class MilestoneType { SPEED, INCOME }

/**
 * One rung of the shared ownership-count milestone ladder: reaching
 * [threshold] units owned multiplies production by [multiplier] — either
 * shrinking cycle time or boosting income per cycle, depending on [type] —
 * compounding with every earlier rung of the *same type* already reached
 * (see [milestoneMultiplierFor]). The same ladder is used two ways — see
 * [CreatureLair.individualSpeedMilestoneMultiplier]/
 * [CreatureLair.individualIncomeMilestoneMultiplier] (this lair's own owned
 * count) and [GameState.globalSpeedMilestoneMultiplier]/
 * [GameState.globalIncomeMilestoneMultiplier] ("Everything" — the *lowest*
 * owned count across every lair, so it only advances once every lair has
 * caught up).
 */
data class MilestoneStep(val threshold: Int, val multiplier: Double, val type: MilestoneType)

val MILESTONE_STEPS: List<MilestoneStep> = listOf(
    MilestoneStep(25, 2.0, MilestoneType.SPEED),
    MilestoneStep(50, 2.0, MilestoneType.SPEED),
    MilestoneStep(100, 2.0, MilestoneType.SPEED),
    MilestoneStep(200, 2.0, MilestoneType.SPEED),
    MilestoneStep(300, 2.0, MilestoneType.SPEED),
    MilestoneStep(400, 2.0, MilestoneType.SPEED),
    MilestoneStep(500, 4.0, MilestoneType.INCOME),
    MilestoneStep(1_000, 5.0, MilestoneType.INCOME),
    MilestoneStep(5_000, 6.0, MilestoneType.INCOME),
    MilestoneStep(10_000, 7.0, MilestoneType.INCOME),
)

/**
 * The compounding multiplier from every [MILESTONE_STEPS] rung of [type] at
 * or below [unitsOwned] — e.g. with [MilestoneType.SPEED], 400 owned reaches
 * the first six rungs for 2^6 = 64x; a rung of the *other* type in between
 * doesn't interrupt the compounding (there isn't one below 500 today, but
 * the filter is type-safe regardless of how the ladder is reordered later).
 */
fun milestoneMultiplierFor(unitsOwned: Int, type: MilestoneType): Double =
    MILESTONE_STEPS.filter { unitsOwned >= it.threshold && it.type == type }.fold(1.0) { acc, step -> acc * step.multiplier }

/**
 * The smallest [MILESTONE_STEPS] threshold strictly greater than
 * [unitsOwned], or null once every rung has been passed (there's no rung
 * past 10,000 — callers past that point need their own fallback, e.g.
 * `BuyQuantity.NEXT`'s "round up to the next multiple of 10,000"). Not
 * filtered by [MilestoneType] — this just answers "how far to the next rung
 * of any kind," which is all `BuyQuantity.NEXT` needs.
 */
fun nextMilestoneThreshold(unitsOwned: Int): Int? =
    MILESTONE_STEPS.map { it.threshold }.firstOrNull { unitsOwned < it }

/**
 * One milestone rung actually reached by a purchase, ready to show the
 * player as a pop-up (see `GameStateExtensions.milestonesCrossed` and
 * `GameViewModel.milestoneAnnouncement`). [lairName] is either a specific
 * lair's display name (an individual rung) or the literal "Everything" (a
 * global rung) — the same two labels `UnlocksContent` already groups by.
 * [type] is what the pop-up (and `UnlocksContent`) use to label the reward
 * correctly as "Speed" or "Income" rather than assuming one or the other.
 */
data class MilestoneAnnouncement(
    val lairName: String,
    val threshold: Int,
    val multiplier: Double,
    val isGlobal: Boolean,
    val type: MilestoneType,
)
