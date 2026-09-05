package com.wyrmwhelp.idlehoard.domain.model

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog

/**
 * Rough single-number measure of how far a save has progressed: liquid
 * currency plus what it would have cost to claim every owned lair unit from
 * scratch. Used only to compare two saves (local vs. cloud) during merge —
 * not shown to the player.
 */
fun GameState.estimatedNetWorth(): Double {
    val investedInLairs = lairs.values.sumOf { owned ->
        val lair = CreatureLairCatalog.get(owned.lairId)
        (0 until owned.count).sumOf { unitsOwnedBefore -> lair.costForNextUnit(unitsOwnedBefore) }
    }
    return goldPieces + platinumPieces * 10.0 + investedInLairs
}

/**
 * The "Everything" milestone bonus: the same compounding schedule as
 * [CreatureLair.individualMilestoneMultiplier], but keyed on the *lowest*
 * owned count across every lair in [catalog] — every lair has to reach a
 * rung before the global bonus for it kicks in, not just whichever lair is
 * furthest ahead.
 */
fun GameState.globalMilestoneMultiplier(catalog: List<CreatureLair> = CreatureLairCatalog.lairs): Double {
    if (catalog.isEmpty()) return 1.0
    return milestoneMultiplierFor(catalog.minOf { ownedLair(it.id).count })
}

/**
 * Every [MilestoneStep] rung newly crossed by a purchase of [lairId] that
 * raised its owned count from [previousCount] up to whatever it is now
 * (read off `this`, the *post*-purchase state) — both this lair's own
 * individual rungs and any "Everything" global rung unlocked by this
 * purchase raising the catalog-wide minimum owned count. Used to pop up a
 * reward announcement right after a claim (see `GameViewModel.claimLair`);
 * a single big purchase (e.g. buying `MAX`) can cross several rungs at
 * once, so this returns all of them — this lair's own rungs first
 * (ascending), then any global rungs (ascending), rather than only the
 * single highest one reached.
 */
fun GameState.milestonesCrossed(
    lairId: String,
    previousCount: Int,
    catalog: List<CreatureLair> = CreatureLairCatalog.lairs,
): List<MilestoneAnnouncement> {
    val lair = catalog.firstOrNull { it.id == lairId } ?: return emptyList()
    val currentCount = ownedLair(lairId).count
    val individual = MILESTONE_STEPS
        .filter { it.threshold > previousCount && it.threshold <= currentCount }
        .map { MilestoneAnnouncement(lairName = lair.name, threshold = it.threshold, multiplier = it.multiplier, isGlobal = false) }

    val previousGlobalMin = catalog.minOfOrNull { if (it.id == lairId) previousCount else ownedLair(it.id).count } ?: 0
    val currentGlobalMin = catalog.minOfOrNull { ownedLair(it.id).count } ?: 0
    val global = MILESTONE_STEPS
        .filter { it.threshold > previousGlobalMin && it.threshold <= currentGlobalMin }
        .map { MilestoneAnnouncement(lairName = "Everything", threshold = it.threshold, multiplier = it.multiplier, isGlobal = true) }

    return individual + global
}

/**
 * Picks the more-progressed of a local and a cloud save (higher [totalMolts]
 * wins outright — a Molt resets the economy, so raw net worth isn't
 * comparable across different prestige counts; net worth breaks ties within
 * the same prestige count). Either side may be missing (no save yet).
 */
fun mergeGameStates(local: GameState?, cloud: GameState?): GameState? {
    if (local == null) return cloud
    if (cloud == null) return local
    return if (cloud.isMoreAdvancedThan(local)) cloud else local
}

private fun GameState.isMoreAdvancedThan(other: GameState): Boolean {
    if (totalMolts != other.totalMolts) return totalMolts > other.totalMolts
    return estimatedNetWorth() > other.estimatedNetWorth()
}
