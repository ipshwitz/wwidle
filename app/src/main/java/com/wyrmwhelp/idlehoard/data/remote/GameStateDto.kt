package com.wyrmwhelp.idlehoard.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** JSON mirror of [com.wyrmwhelp.idlehoard.domain.model.GameState] for the `state` jsonb column. */
@Serializable
data class GameStateDto(
    @SerialName("gold_pieces") val goldPieces: Double,
    @SerialName("platinum_pieces") val platinumPieces: Double,
    @SerialName("scale_shards") val scaleShards: Long,
    val lairs: Map<String, OwnedLairDto>,
    @SerialName("offline_cap_hours") val offlineCapHours: Double,
    @SerialName("last_saved_at_epoch_millis") val lastSavedAtEpochMillis: Long,
    @SerialName("total_molts") val totalMolts: Int,
)

/** JSON mirror of [com.wyrmwhelp.idlehoard.domain.model.OwnedLair]. */
@Serializable
data class OwnedLairDto(
    val count: Int,
    @SerialName("has_steward") val hasSteward: Boolean,
    @SerialName("cycle_progress_seconds") val cycleProgressSeconds: Double,
    @SerialName("is_ready_to_collect") val isReadyToCollect: Boolean,
)

/** One row of the `cloud_saves` table. */
@Serializable
data class CloudSaveRow(
    @SerialName("user_id") val userId: String,
    val state: GameStateDto,
    @SerialName("updated_at") val updatedAt: String,
)
