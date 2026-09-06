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
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
