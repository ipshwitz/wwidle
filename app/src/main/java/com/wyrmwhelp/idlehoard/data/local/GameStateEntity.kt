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
    val scaleShards: Long,
    val offlineCapHours: Double,
    val lastSavedAtEpochMillis: Long,
    val totalMolts: Int,
    val speedBoostLevel: Int = 0,
    val profitBoostLevel: Int = 0,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
