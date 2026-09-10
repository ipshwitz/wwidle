package com.wyrmwhelp.idlehoard.domain.model

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog

/**
 * One permanent, one-time accomplishment. Unlike every other upgrade or
 * milestone in this game, an [Achievement] is never bought — [isCompleted]
 * is a pure check against persistent, never-resetting `GameState` stats
 * (see [GameState.highestLairCounts] and its siblings' own doc comments),
 * so the moment the underlying stat crosses the threshold, it's complete
 * forever — a Level Up or an Account Reset can't un-complete it, unlike
 * every Gold/Gem upgrade level or milestone rung.
 *
 * Modeled on Adventure Capitalist's own achievement system: completing one
 * contributes a small permanent [bonusPercent] to the account-wide
 * "Achievement Bonus" (see [GameStateExtensions.kt]'s
 * `achievementBonusPercent`/`achievementIncomeMultiplier`) — a genuinely
 * permanent income multiplier, unlike every Gold/Gem upgrade (resets on a
 * Level Up) and even the Platinum-bought permanent boosts (require real
 * ongoing spending to grow). First-pass placeholder tuning — the ~68
 * achievements defined in [Achievements.ALL] sum to +97.5% if every single
 * one is ever completed, comparable in scale to `GpUpgrades`' own maxed-out
 * "Everything" lines — not playtested, same as everywhere else in the
 * economy.
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val bonusPercent: Double,
    val isCompleted: (GameState) -> Boolean,
)

/** Purely how [com.wyrmwhelp.idlehoard.ui.achievements.AchievementsContent] groups the list — has no effect on [Achievement.isCompleted]/[Achievement.bonusPercent]. */
enum class AchievementGroup(val label: String) {
    PER_LAIR("Per-Lair"),
    NET_WORTH("Net Worth"),
    PRESTIGE("Prestige"),
    STEWARDS("Stewards"),
    UPGRADES("Upgrades"),
    PLATINUM("Platinum"),
    MISC("Milestones"),
}

data class CategorizedAchievement(val group: AchievementGroup, val achievement: Achievement)

object Achievements {
    /**
     * Ownership thresholds every per-lair achievement uses — deliberately
     * the same 100/1,000/10,000 rungs `Milestone.kt`'s own ladder reaches,
     * so a per-lair achievement completing lines up with a milestone the
     * player already recognizes, rather than an unrelated arbitrary number.
     */
    private val LAIR_OWNERSHIP_TIERS = listOf(100, 1_000, 10_000)
    private val LAIR_OWNERSHIP_BONUS_PERCENT = listOf(0.25, 0.5, 1.0)

    private val perLairAchievements: List<CategorizedAchievement> =
        CreatureLairCatalog.lairs.flatMap { lair ->
            LAIR_OWNERSHIP_TIERS.mapIndexed { index, threshold ->
                CategorizedAchievement(
                    AchievementGroup.PER_LAIR,
                    Achievement(
                        id = "own_${lair.id}_$threshold",
                        name = "${lair.name} — Own $threshold",
                        description = "Reach $threshold ${lair.name} owned at once (any run, ever).",
                        bonusPercent = LAIR_OWNERSHIP_BONUS_PERCENT[index],
                        isCompleted = { state -> (state.highestLairCounts[lair.id] ?: 0) >= threshold },
                    ),
                )
            }
        }

    private val netWorthAchievements: List<CategorizedAchievement> = listOf(
        Triple(1_000_000.0, "First Million" to "Earn 1,000,000 Gold Pieces in total.", 0.5),
        Triple(1_000_000_000.0, "First Billion" to "Earn 1,000,000,000 Gold Pieces in total.", 1.0),
        Triple(1_000_000_000_000.0, "First Trillion" to "Earn 1,000,000,000,000 Gold Pieces in total.", 2.0),
        Triple(1_000_000_000_000_000.0, "First Quadrillion" to "Earn 1,000,000,000,000,000 Gold Pieces in total.", 4.0),
    ).map { (threshold, nameAndDesc, bonus) ->
        CategorizedAchievement(
            AchievementGroup.NET_WORTH,
            Achievement(
                id = "net_worth_${threshold.toLong()}",
                name = nameAndDesc.first,
                description = nameAndDesc.second,
                bonusPercent = bonus,
                isCompleted = { it.lifetimeGoldEarned >= threshold },
            ),
        )
    }

    private val prestigeAchievements: List<CategorizedAchievement> = listOf(
        Triple(1, "First Level Up" to "Level Up once.", 1.0),
        Triple(5, "Seasoned Prestige" to "Level Up 5 times.", 2.0),
        Triple(10, "Veteran Prestige" to "Level Up 10 times.", 4.0),
        Triple(25, "Legendary Prestige" to "Level Up 25 times.", 8.0),
    ).map { (threshold, nameAndDesc, bonus) ->
        CategorizedAchievement(
            AchievementGroup.PRESTIGE,
            Achievement(
                id = "prestige_$threshold",
                name = nameAndDesc.first,
                description = nameAndDesc.second,
                bonusPercent = bonus,
                isCompleted = { it.totalLevelUps >= threshold },
            ),
        )
    }

    private val gemBatchAchievements: List<CategorizedAchievement> = listOf(
        Triple(50L, "Gem Collector" to "Earn a batch of 50 Gems from a single Level Up.", 1.0),
        Triple(150L, "Gem Hoarder" to "Earn a batch of 150 Gems from a single Level Up.", 2.0),
        Triple(500L, "Gem Tycoon" to "Earn a batch of 500 Gems from a single Level Up.", 4.0),
    ).map { (threshold, nameAndDesc, bonus) ->
        CategorizedAchievement(
            AchievementGroup.PRESTIGE,
            Achievement(
                id = "gem_batch_$threshold",
                name = nameAndDesc.first,
                description = nameAndDesc.second,
                bonusPercent = bonus,
                isCompleted = { it.highestGemsEverEarned >= threshold },
            ),
        )
    }

    private val adsWatchedAchievements: List<CategorizedAchievement> = listOf(
        Triple(10, "Attentive Viewer" to "Watch 10 rewarded ads in total.", 0.5),
        Triple(50, "Dedicated Viewer" to "Watch 50 rewarded ads in total.", 1.0),
        Triple(100, "Ad Devotee" to "Watch 100 rewarded ads in total.", 2.0),
    ).map { (threshold, nameAndDesc, bonus) ->
        CategorizedAchievement(
            AchievementGroup.MISC,
            Achievement(
                id = "ads_watched_$threshold",
                name = nameAndDesc.first,
                description = nameAndDesc.second,
                bonusPercent = bonus,
                isCompleted = { it.totalAdsWatched >= threshold },
            ),
        )
    }

    private val stewardAchievements: List<CategorizedAchievement> = listOf(
        CategorizedAchievement(
            AchievementGroup.STEWARDS,
            Achievement(
                id = "steward_first",
                name = "First Steward",
                description = "Hire a Steward for any lair.",
                bonusPercent = 1.0,
                isCompleted = { it.everHiredStewardForLairs.isNotEmpty() },
            ),
        ),
        CategorizedAchievement(
            AchievementGroup.STEWARDS,
            Achievement(
                id = "steward_all",
                name = "Full Staff",
                description = "Hire a Steward for every lair in the catalog (any run, ever).",
                bonusPercent = 5.0,
                isCompleted = { it.everHiredStewardForLairs.size >= CreatureLairCatalog.lairs.size },
            ),
        ),
        CategorizedAchievement(
            AchievementGroup.STEWARDS,
            Achievement(
                id = "steward_efficiency_first",
                name = "Efficiency Expert",
                description = "Max out Steward Efficiency (99% off) for any one lair.",
                bonusPercent = 2.0,
                isCompleted = { it.everMaxedStewardEfficiencyForLairs.isNotEmpty() },
            ),
        ),
        CategorizedAchievement(
            AchievementGroup.STEWARDS,
            Achievement(
                id = "steward_efficiency_all",
                name = "Master of Efficiency",
                description = "Max out Steward Efficiency for every lair in the catalog (any run, ever).",
                bonusPercent = 8.0,
                isCompleted = { it.everMaxedStewardEfficiencyForLairs.size >= CreatureLairCatalog.lairs.size },
            ),
        ),
        CategorizedAchievement(
            AchievementGroup.MISC,
            Achievement(
                id = "universal_steward",
                name = "Universal Steward",
                description = "Earn the account-wide Universal Steward.",
                bonusPercent = 5.0,
                isCompleted = { it.hasUniversalSteward() },
            ),
        ),
        CategorizedAchievement(
            AchievementGroup.MISC,
            Achievement(
                id = "claimed_all_lairs",
                name = "Claimed Them All",
                description = "Claim at least one of every lair in the catalog (any run, ever).",
                bonusPercent = 5.0,
                isCompleted = { state ->
                    CreatureLairCatalog.lairs.all { (state.highestLairCounts[it.id] ?: 0) >= 1 }
                },
            ),
        ),
    )

    private val upgradeAchievements: List<CategorizedAchievement> = listOf(
        CategorizedAchievement(
            AchievementGroup.UPGRADES,
            Achievement(
                id = "gem_efficiency_maxed",
                name = "Gem Efficiency Mastered",
                description = "Max out the Gem Efficiency upgrade.",
                bonusPercent = 3.0,
                isCompleted = { it.everMaxedGemEfficiency },
            ),
        ),
        CategorizedAchievement(
            AchievementGroup.UPGRADES,
            Achievement(
                id = "any_lair_profit_maxed",
                name = "Profit Perfected",
                description = "Max out any single lair's own Profit upgrade line.",
                bonusPercent = 1.0,
                isCompleted = { it.everMaxedAnyLairProfitLine },
            ),
        ),
        CategorizedAchievement(
            AchievementGroup.UPGRADES,
            Achievement(
                id = "any_lair_speed_maxed",
                name = "Speed Perfected",
                description = "Max out any single lair's own Speed upgrade line.",
                bonusPercent = 1.0,
                isCompleted = { it.everMaxedAnyLairSpeedLine },
            ),
        ),
        CategorizedAchievement(
            AchievementGroup.UPGRADES,
            Achievement(
                id = "everything_maxed",
                name = "Everything, Everywhere",
                description = "Max out both \"Everything Profit\" and \"Everything Speed\".",
                bonusPercent = 5.0,
                isCompleted = { it.everMaxedEverythingProfit && it.everMaxedEverythingSpeed },
            ),
        ),
    )

    private val platinumAchievements: List<CategorizedAchievement> = listOf(
        CategorizedAchievement(
            AchievementGroup.PLATINUM,
            Achievement(
                id = "permanent_boost_first",
                name = "First Investment",
                description = "Buy any permanent Platinum boost tier once.",
                bonusPercent = 1.0,
                isCompleted = { state -> ALL_PERMANENT_BOOST_TIERS.any { state.permanentBoostLevel(it) >= 1 } },
            ),
        ),
        CategorizedAchievement(
            AchievementGroup.PLATINUM,
            Achievement(
                id = "permanent_boost_veteran",
                name = "Platinum Patron",
                description = "Own 5 or more copies of any single permanent Platinum boost tier.",
                bonusPercent = 3.0,
                isCompleted = { state -> ALL_PERMANENT_BOOST_TIERS.any { state.permanentBoostLevel(it) >= 5 } },
            ),
        ),
    )

    /** Every achievement in the game — see this file's class doc for the tuning/total. */
    val ALL: List<CategorizedAchievement> =
        perLairAchievements + netWorthAchievements + prestigeAchievements + gemBatchAchievements +
            adsWatchedAchievements + stewardAchievements + upgradeAchievements + platinumAchievements
}
