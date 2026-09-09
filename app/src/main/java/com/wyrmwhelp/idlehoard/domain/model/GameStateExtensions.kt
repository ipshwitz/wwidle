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
        val costMultiplier = StewardEfficiency.costMultiplier(owned.stewardEfficiencyLevel)
        (0 until owned.count).sumOf { unitsOwnedBefore -> lair.costForNextUnit(unitsOwnedBefore, costMultiplier) }
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

/**
 * Ids of owned, Steward-less lairs the player can *afford* to hire right
 * now — "new" is determined by availability, not just visibility (an
 * owned Steward-less lair can sit there a long time before it's actually
 * affordable; that shouldn't count as a "new" opportunity until it is).
 * Always empty once [hasUniversalSteward] is true — there's no hiring
 * opportunity left to flag once every owned lair already auto-collects.
 */
private fun GameState.stewardOpportunities(catalog: List<CreatureLair> = CreatureLairCatalog.lairs): Set<String> {
    if (hasUniversalSteward()) return emptySet()
    return lairs.values.filter { owned ->
        owned.count > 0 && !owned.hasSteward && goldPieces >= catalog.first { it.id == owned.lairId }.stewardCostGp
    }.map { it.lairId }.toSet()
}

/**
 * Owned, Steward-less, currently-affordable lairs the player hasn't had a
 * chance to notice yet — i.e. not already in
 * [GameState.seenStewardOpportunities]. Drives the "new feature" star
 * badge on `FloatingMenu`'s chest toggle and its "Stewards" plank
 * (`ui/menu/FloatingMenu.kt`); [withStewardOpportunitiesSeen] is what
 * clears it once the player actually opens that section.
 */
fun GameState.unseenStewardOpportunities(catalog: List<CreatureLair> = CreatureLairCatalog.lairs): Set<String> =
    stewardOpportunities(catalog) - seenStewardOpportunities

/** Whether the Stewards badge should show at all — see [unseenStewardOpportunities]. */
fun GameState.hasUnseenStewardOpportunity(catalog: List<CreatureLair> = CreatureLairCatalog.lairs): Boolean =
    unseenStewardOpportunities(catalog).isNotEmpty()

/**
 * Marks every *currently* affordable Steward opportunity as seen — called
 * once when the player opens the Stewards section
 * (`GameViewModel.markStewardOpportunitiesSeen`), so the badge won't come
 * back for those same lairs until a Level Up resets [GameState.lairs]/
 * [GameState.goldPieces] (and, with them,
 * [GameState.seenStewardOpportunities] — see that field's doc), the gold
 * is spent and re-earned, or a newly-affordable lair creates a fresh
 * opportunity.
 */
fun GameState.withStewardOpportunitiesSeen(catalog: List<CreatureLair> = CreatureLairCatalog.lairs): GameState =
    copy(seenStewardOpportunities = seenStewardOpportunities + stewardOpportunities(catalog))

/**
 * Ids of every Gold/Gem upgrade line whose *next* tier the player can
 * currently afford and hasn't maxed out — `"<lairId>:profit"`/
 * `"<lairId>:speed"` for the 28 per-lair lines, `"everything:profit"`/
 * `"everything:speed"` for the two Everything lines, and
 * `"gem_efficiency"` for the single Gem line. Same "availability, not
 * visibility" rule as [stewardOpportunities] — every line is always
 * *visible* in the Upgrades menu regardless of ownership, so affordability
 * is the only meaningful "new" signal here.
 */
private fun GameState.upgradeOpportunities(catalog: List<CreatureLair> = CreatureLairCatalog.lairs): Set<String> {
    val ids = mutableSetOf<String>()
    for (lair in catalog) {
        val owned = ownedLair(lair.id)
        val profitTier = owned.profitUpgradeLevel + 1
        if (profitTier <= GpUpgrades.LAIR_LINE_PHASES.totalTiers &&
            goldPieces >= GpUpgrades.costForLairTier(lair.id, UpgradeCategory.PROFIT, profitTier)
        ) {
            ids += "${lair.id}:profit"
        }
        val speedTier = owned.speedUpgradeLevel + 1
        if (speedTier <= GpUpgrades.LAIR_LINE_PHASES.totalTiers &&
            goldPieces >= GpUpgrades.costForLairTier(lair.id, UpgradeCategory.SPEED, speedTier)
        ) {
            ids += "${lair.id}:speed"
        }
    }
    val everythingProfitTier = everythingProfitUpgradeLevel + 1
    if (everythingProfitTier <= GpUpgrades.EVERYTHING_PROFIT_PHASES.totalTiers &&
        goldPieces >= GpUpgrades.costForEverythingTier(UpgradeCategory.PROFIT, everythingProfitTier)
    ) {
        ids += "everything:profit"
    }
    val everythingSpeedTier = everythingSpeedUpgradeLevel + 1
    if (everythingSpeedTier <= GpUpgrades.EVERYTHING_SPEED_PHASES.totalTiers &&
        goldPieces >= GpUpgrades.costForEverythingTier(UpgradeCategory.SPEED, everythingSpeedTier)
    ) {
        ids += "everything:speed"
    }
    val gemTier = gemEfficiencyLevel + 1
    if (gemTier <= GemUpgrades.PHASES.totalTiers && gems >= GemUpgrades.costForTierGems(gemTier)) {
        ids += "gem_efficiency"
    }
    return ids
}

/** Currently-affordable upgrade lines the player hasn't had a chance to notice yet — see [upgradeOpportunities]. */
fun GameState.unseenUpgradeOpportunities(catalog: List<CreatureLair> = CreatureLairCatalog.lairs): Set<String> =
    upgradeOpportunities(catalog) - seenUpgradeOpportunities

/** Whether the Upgrades badge should show at all — see [unseenUpgradeOpportunities]. */
fun GameState.hasUnseenUpgradeOpportunity(catalog: List<CreatureLair> = CreatureLairCatalog.lairs): Boolean =
    unseenUpgradeOpportunities(catalog).isNotEmpty()

/**
 * Marks every *currently* affordable upgrade line as seen — called once
 * when the player opens the Upgrades section
 * (`GameViewModel.markUpgradeOpportunitiesSeen`), same shape as
 * [withStewardOpportunitiesSeen].
 */
fun GameState.withUpgradeOpportunitiesSeen(catalog: List<CreatureLair> = CreatureLairCatalog.lairs): GameState =
    copy(seenUpgradeOpportunities = seenUpgradeOpportunities + upgradeOpportunities(catalog))
