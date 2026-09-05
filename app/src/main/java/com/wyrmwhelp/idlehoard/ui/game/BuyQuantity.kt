package com.wyrmwhelp.idlehoard.ui.game

/**
 * The bulk-purchase quantity shown in [GameHeader] and cycled by tapping its
 * small selector box. UI-only for now — nothing in [GameViewModel] or
 * [com.wyrmwhelp.idlehoard.domain.engine.GameEngine] reads this yet to
 * actually buy more than one unit at a time; wiring it into `claimLair` is
 * follow-up work once bulk-purchase cost math (and what "Next" means — next
 * milestone? next round number?) is designed.
 */
enum class BuyQuantity(val label: String) {
    X1("x1"),
    X10("x10"),
    X100("x100"),
    NEXT("Next"),
    MAX("Max"),
    ;

    fun next(): BuyQuantity = entries[(ordinal + 1) % entries.size]
}
