package com.wyrmwhelp.idlehoard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding the save's top-level currencies/meta. [id] is
 * always [SINGLETON_ID] — there's only ever one save per install.
 */
@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val goldPieces: Double,
    val platinumPieces: Double,
    val gems: Long,
    val lifetimeGoldEarned: Double = 0.0,
    val everythingProfitUpgradeLevel: Int = 0,
    val everythingSpeedUpgradeLevel: Int = 0,
    val gemEfficiencyLevel: Int = 0,
    val offlineCapHours: Double,
    val lastSavedAtEpochMillis: Long,
    val totalLevelUps: Int,
    val permanentSpeedBoost2xLevel: Int = 0,
    val permanentSpeedBoost5xLevel: Int = 0,
    val permanentSpeedBoost10xLevel: Int = 0,
    val permanentProfitBoost15xLevel: Int = 0,
    val permanentProfitBoost2xLevel: Int = 0,
    val permanentProfitBoost5xLevel: Int = 0,
    val permanentGemBoost15xLevel: Int = 0,
    val permanentGemBoost2xLevel: Int = 0,
    val permanentGemBoost5xLevel: Int = 0,
    /**
     * [com.wyrmwhelp.idlehoard.domain.model.GameState.activeTemporaryBoosts]
     * JSON-encoded as a single column (a small list of `{category,
     * multiplier, expiresAtEpochMillis}` records — see
     * `GameStateMappers.kt`'s (de)serialization) rather than a separate
     * Room table, since it's a handful of short-lived entries at most and
     * doesn't need relational querying the way [OwnedLairEntity] does.
     * `"[]"` for an empty list, never blank/null.
     */
    val activeTemporaryBoostsJson: String = "[]",
    val lastPlatinumAdWatchedAtEpochMillis: Long? = null,
    /**
     * [com.wyrmwhelp.idlehoard.domain.model.GameState.speedBoostAdWatchTimestamps]
     * JSON-encoded as a single column (a small list of epoch-millis longs —
     * see `GameStateMappers.kt`'s (de)serialization), same reasoning as
     * [activeTemporaryBoostsJson]. `"[]"` for an empty list, never
     * blank/null.
     */
    val speedBoostAdWatchTimestampsJson: String = "[]",
    /**
     * [com.wyrmwhelp.idlehoard.domain.model.GameState.incomeBoostAdWatchTimestamps]
     * JSON-encoded as a single column, same shape as
     * [speedBoostAdWatchTimestampsJson] but for the Income-boost ad-watch
     * reward. `"[]"` for an empty list, never blank/null.
     */
    val incomeBoostAdWatchTimestampsJson: String = "[]",
    /**
     * [com.wyrmwhelp.idlehoard.domain.model.GameState.seenStewardOpportunities]
     * JSON-encoded as a single column (a small list of lair id strings —
     * see `GameStateMappers.kt`'s (de)serialization), same reasoning as
     * [activeTemporaryBoostsJson]. `"[]"` for an empty set, never
     * blank/null.
     */
    val seenStewardOpportunitiesJson: String = "[]",
    /**
     * [com.wyrmwhelp.idlehoard.domain.model.GameState.seenUpgradeOpportunities]
     * JSON-encoded as a single column (a small list of upgrade line id
     * strings — see `GameStateMappers.kt`'s (de)serialization), same
     * reasoning as [activeTemporaryBoostsJson]. `"[]"` for an empty set,
     * never blank/null.
     */
    val seenUpgradeOpportunitiesJson: String = "[]",
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.selectedAvatarId] — a plain nullable column, no JSON encoding needed for a single string. */
    val selectedAvatarId: String? = null,
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.totalAdsWatched] — a plain `Int` column. */
    val totalAdsWatched: Int = 0,
    /**
     * [com.wyrmwhelp.idlehoard.domain.model.GameState.highestLairCounts]
     * JSON-encoded as a single column (a small `{lairId: count}` object —
     * see `GameStateMappers.kt`'s (de)serialization), same reasoning as
     * [activeTemporaryBoostsJson]. `"{}"` for an empty map, never
     * blank/null.
     */
    val highestLairCountsJson: String = "{}",
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.everHiredStewardForLairs] JSON-encoded as a single column (a small list of lair id strings), same shape as [seenStewardOpportunitiesJson]. `"[]"` for an empty set, never blank/null. */
    val everHiredStewardForLairsJson: String = "[]",
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.everMaxedStewardEfficiencyForLairs] JSON-encoded as a single column, same shape as [everHiredStewardForLairsJson]. `"[]"` for an empty set, never blank/null. */
    val everMaxedStewardEfficiencyForLairsJson: String = "[]",
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.everMaxedGemEfficiency] — a plain `Boolean` column. */
    val everMaxedGemEfficiency: Boolean = false,
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.everMaxedEverythingProfit] — a plain `Boolean` column. */
    val everMaxedEverythingProfit: Boolean = false,
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.everMaxedEverythingSpeed] — a plain `Boolean` column. */
    val everMaxedEverythingSpeed: Boolean = false,
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.everMaxedAnyLairProfitLine] — a plain `Boolean` column. */
    val everMaxedAnyLairProfitLine: Boolean = false,
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.everMaxedAnyLairSpeedLine] — a plain `Boolean` column. */
    val everMaxedAnyLairSpeedLine: Boolean = false,
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.highestGemsEverEarned] — a plain `Long` column. */
    val highestGemsEverEarned: Long = 0,
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.seenAchievements] JSON-encoded as a single column (a small list of achievement id strings), same shape as [seenStewardOpportunitiesJson]. `"[]"` for an empty set, never blank/null. */
    val seenAchievementsJson: String = "[]",
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.dailyRewardStreakDay] — a plain `Int` column. */
    val dailyRewardStreakDay: Int = 0,
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.dailyRewardLastClaimedEpochDay] — a plain nullable `Long` column (an epoch day, not epoch millis). */
    val dailyRewardLastClaimedEpochDay: Long? = null,
    /** [com.wyrmwhelp.idlehoard.domain.model.GameState.dailyRewardAutoPopupShownEpochDay] — a plain nullable `Long` column (an epoch day, not epoch millis). */
    val dailyRewardAutoPopupShownEpochDay: Long? = null,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
