package com.wyrmwhelp.idlehoard.ui.game

import com.wyrmwhelp.idlehoard.domain.model.CreatureLair
import com.wyrmwhelp.idlehoard.domain.model.MILESTONE_STEPS
import com.wyrmwhelp.idlehoard.domain.model.nextMilestoneThreshold

/**
 * The bulk-purchase quantity shown in [GameHeader] and cycled by tapping its
 * small selector box.
 */
enum class BuyQuantity(val label: String) {
    X1("x1"),
    X10("x10"),
    X100("x100"),
    NEXT("Next"),
    MAX("Max"),
    ;

    fun next(): BuyQuantity = entries[(ordinal + 1) % entries.size]

    /**
     * Resolves this selection to a concrete number of units to buy for one
     * specific [lair], given [unitsOwned] already claimed and [availableGp]
     * on hand. The single source of truth for both what `LairCard` previews
     * (label + cost) and what `GameViewModel.claimLair` actually purchases —
     * keeping those in sync is the whole point of not duplicating this logic
     * in two places.
     *
     * `NEXT` means "buy up to this lair's next milestone rung" (see
     * `domain/model/Milestone.kt`) — owning 21 Kobold Warrens buys 4 more to
     * reach the 25-owned rung. Past the last defined rung (10,000), it falls
     * back to rounding up to the next multiple of 10,000 so "Next" still
     * means something instead of silently buying 1 forever. `MAX` is the
     * most units [lair] can afford for [availableGp] (see
     * [CreatureLair.maxAffordableUnits]), which can be 0 if even one more
     * unit isn't affordable — callers that want a cost preview to show
     * anyway should `coerceAtLeast(1)` the result themselves.
     */
    fun resolve(lair: CreatureLair, unitsOwned: Int, availableGp: Double): Int = when (this) {
        X1 -> 1
        X10 -> 10
        X100 -> 100
        NEXT -> {
            val nextThreshold = nextMilestoneThreshold(unitsOwned)
            if (nextThreshold != null) {
                nextThreshold - unitsOwned
            } else {
                val step = MILESTONE_STEPS.last().threshold
                val remainder = unitsOwned % step
                if (remainder == 0) step else step - remainder
            }
        }
        MAX -> lair.maxAffordableUnits(unitsOwned, availableGp)
    }
}
