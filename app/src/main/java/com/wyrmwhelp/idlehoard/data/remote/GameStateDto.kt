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
    @SerialName("everything_profit_upgrade_level") val everythingProfitUpgradeLevel: Int = 0,
    @SerialName("everything_speed_upgrade_level") val everythingSpeedUpgradeLevel: Int = 0,
    @SerialName("gem_efficiency_level") val gemEfficiencyLevel: Int = 0,
    val lairs: Map<String, OwnedLairDto>,
    @SerialName("offline_cap_hours") val offlineCapHours: Double,
    @SerialName("last_saved_at_epoch_millis") val lastSavedAtEpochMillis: Long,
    @SerialName("total_level_ups") val totalLevelUps: Int = 0,
    @SerialName("permanent_speed_boost_2x_level") val permanentSpeedBoost2xLevel: Int = 0,
    @SerialName("permanent_speed_boost_5x_level") val permanentSpeedBoost5xLevel: Int = 0,
    @SerialName("permanent_speed_boost_10x_level") val permanentSpeedBoost10xLevel: Int = 0,
    @SerialName("permanent_profit_boost_15x_level") val permanentProfitBoost15xLevel: Int = 0,
    @SerialName("permanent_profit_boost_2x_level") val permanentProfitBoost2xLevel: Int = 0,
    @SerialName("permanent_profit_boost_5x_level") val permanentProfitBoost5xLevel: Int = 0,
    @SerialName("permanent_gem_boost_15x_level") val permanentGemBoost15xLevel: Int = 0,
    @SerialName("permanent_gem_boost_2x_level") val permanentGemBoost2xLevel: Int = 0,
    @SerialName("permanent_gem_boost_5x_level") val permanentGemBoost5xLevel: Int = 0,
    @SerialName("active_temporary_boosts") val activeTemporaryBoosts: List<ActiveTemporaryBoostDto> = emptyList(),
    @SerialName("last_platinum_ad_watched_at_epoch_millis") val lastPlatinumAdWatchedAtEpochMillis: Long? = null,
)

/** JSON mirror of [com.wyrmwhelp.idlehoard.domain.model.ActiveTemporaryBoost]. */
@Serializable
data class ActiveTemporaryBoostDto(
    val category: String,
    val multiplier: Double,
    @SerialName("expires_at_epoch_millis") val expiresAtEpochMillis: Long,
)

/** JSON mirror of [com.wyrmwhelp.idlehoard.domain.model.OwnedLair]. */
@Serializable
data class OwnedLairDto(
    val count: Int,
    @SerialName("has_steward") val hasSteward: Boolean,
    @SerialName("cycle_progress_seconds") val cycleProgressSeconds: Double,
    @SerialName("is_loading") val isLoading: Boolean = false,
    @SerialName("completed_loads") val completedLoads: Int = 0,
    @SerialName("profit_upgrade_level") val profitUpgradeLevel: Int = 0,
    @SerialName("speed_upgrade_level") val speedUpgradeLevel: Int = 0,
)

/** One row of the `cloud_saves` table. */
@Serializable
data class CloudSaveRow(
    @SerialName("user_id") val userId: String,
    val state: GameStateDto,
    @SerialName("updated_at") val updatedAt: String,
)
