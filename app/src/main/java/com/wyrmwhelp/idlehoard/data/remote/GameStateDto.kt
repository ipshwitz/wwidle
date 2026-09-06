package com.wyrmwhelp.idlehoard.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** JSON mirror of [com.wyrmwhelp.idlehoard.domain.model.GameState] for the `state` jsonb column. */
@Serializable
data class GameStateDto(
    @SerialName("gold_pieces") val goldPieces: Double,
    @SerialName("platinum_pieces") val platinumPieces: Double,
    @SerialName("gems") val gems: Long = 0,
    @SerialName("lifetime_gold_earned") val lifetimeGoldEarned: Double = 0.0,
    val lairs: Map<String, OwnedLairDto>,
    @SerialName("offline_cap_hours") val offlineCapHours: Double,
    @SerialName("last_saved_at_epoch_millis") val lastSavedAtEpochMillis: Long,
    @SerialName("total_level_ups") val totalLevelUps: Int = 0,
    @SerialName("speed_boost_level") val speedBoostLevel: Int = 0,
    @SerialName("profit_boost_level") val profitBoostLevel: Int = 0,
    @SerialName("last_platinum_ad_watched_at_epoch_millis") val lastPlatinumAdWatchedAtEpochMillis: Long? = null,
)

/** JSON mirror of [com.wyrmwhelp.idlehoard.domain.model.OwnedLair]. */
@Serializable
data class OwnedLairDto(
    val count: Int,
    @SerialName("has_steward") val hasSteward: Boolean,
    @SerialName("cycle_progress_seconds") val cycleProgressSeconds: Double,
    @SerialName("is_loading") val isLoading: Boolean = false,
    @SerialName("completed_loads") val completedLoads: Int = 0,
)

/** One row of the `cloud_saves` table. */
@Serializable
data class CloudSaveRow(
    @SerialName("user_id") val userId: String,
    val state: GameStateDto,
    @SerialName("updated_at") val updatedAt: String,
)
