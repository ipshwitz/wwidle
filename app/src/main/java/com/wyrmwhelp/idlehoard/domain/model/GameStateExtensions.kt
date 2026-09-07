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
 * The "Everything" milestone's Speed bonus: the same compounding schedule
 * as [CreatureLair.individualSpeedMilestoneMultiplier], but keyed on the
 * *lowest* owned count across every lair in [catalog] — every lair has to
 * reach a rung before the global bonus for it kicks in, not just whichever
 * lair is furthest ahead.
 */
fun GameState.globalSpeedMilestoneMultiplier(catalog: List<CreatureLair> = CreatureLairCatalog.lairs): Double {
    if (catalog.isEmpty()) return 1.0
    return milestoneMultiplierFor(catalog.minOf { ownedLair(it.id).count }, MilestoneType.SPEED)
}

/** The "Everything" milestone's Income bonus — see [globalSpeedMilestoneMultiplier], same idea for [MilestoneType.INCOME] rungs. */
fun GameState.globalIncomeMilestoneMultiplier(catalog: List<CreatureLair> = CreatureLairCatalog.lairs): Double {
    if (catalog.isEmpty()) return 1.0
    return milestoneMultiplierFor(catalog.minOf { ownedLair(it.id).count }, MilestoneType.INCOME)
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
        .map {
            MilestoneAnnouncement(
                lairName = lair.name,
                threshold = it.threshold,
                multiplier = it.multiplier,
                isGlobal = false,
                type = it.type,
            )
        }

    val previousGlobalMin = catalog.minOfOrNull { if (it.id == lairId) previousCount else ownedLair(it.id).count } ?: 0
    val currentGlobalMin = catalog.minOfOrNull { ownedLair(it.id).count } ?: 0
    val global = MILESTONE_STEPS
        .filter { it.threshold > previousGlobalMin && it.threshold <= currentGlobalMin }
        .map {
            MilestoneAnnouncement(
                lairName = "Everything",
                threshold = it.threshold,
                multiplier = it.multiplier,
                isGlobal = true,
                type = it.type,
            )
        }

    return individual + global
}

/**
 * Picks the more-progressed of a local and a cloud save (higher
 * [totalLevelUps] wins outright — a Level Up resets the economy, so raw net
 * worth isn't comparable across different prestige counts; net worth breaks
 * ties within the same prestige count). Either side may be missing (no save
 * yet).
 */
fun mergeGameStates(local: GameState?, cloud: GameState?): GameState? {
    if (local == null) return cloud
    if (cloud == null) return local
    return if (cloud.isMoreAdvancedThan(local)) cloud else local
}

private fun GameState.isMoreAdvancedThan(other: GameState): Boolean {
    if (totalLevelUps != other.totalLevelUps) return totalLevelUps > other.totalLevelUps
    return estimatedNetWorth() > other.estimatedNetWorth()
}

/** Ids of owned, Steward-less lairs — a fresh "you could hire a Steward here" opportunity. */
private fun GameState.stewardOpportunities(): Set<String> =
    lairs.values.filter { it.count > 0 && !it.hasSteward }.map { it.lairId }.toSet()

/**
 * Owned, Steward-less lairs the player hasn't had a chance to notice yet —
 * i.e. not already in [GameState.seenStewardOpportunities]. Drives the
 * "new feature" star badge on `FloatingMenu`'s chest toggle and its
 * "Stewards" plank (`ui/menu/FloatingMenu.kt`); [withStewardOpportunitiesSeen]
 * is what clears it once the player actually opens that section.
 */
fun GameState.unseenStewardOpportunities(): Set<String> = stewardOpportunities() - seenStewardOpportunities

/** Whether the Stewards badge should show at all — see [unseenStewardOpportunities]. */
fun GameState.hasUnseenStewardOpportunity(): Boolean = unseenStewardOpportunities().isNotEmpty()

/**
 * Marks every *currently* eligible Steward opportunity as seen — called
 * once when the player opens the Stewards section
 * (`GameViewModel.markStewardOpportunitiesSeen`), so the badge won't come
 * back for those same lairs until a Level Up resets [GameState.lairs] (and,
 * with it, [GameState.seenStewardOpportunities] — see that field's doc)
 * or a newly-owned lair creates a fresh opportunity.
 */
fun GameState.withStewardOpportunitiesSeen(): GameState =
    copy(seenStewardOpportunities = seenStewardOpportunities + stewardOpportunities())
