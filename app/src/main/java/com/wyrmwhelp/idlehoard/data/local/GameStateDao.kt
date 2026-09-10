package com.wyrmwhelp.idlehoard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface GameStateDao {

    @Query("SELECT * FROM game_state WHERE id = ${GameStateEntity.SINGLETON_ID}")
    suspend fun getGameState(): GameStateEntity?

    @Query("SELECT * FROM owned_lairs")
    suspend fun getOwnedLairs(): List<OwnedLairEntity>

    @Upsert
    suspend fun upsertGameState(entity: GameStateEntity)

    @Upsert
    suspend fun upsertOwnedLairs(entities: List<OwnedLairEntity>)

    @Query("DELETE FROM owned_lairs")
    suspend fun clearOwnedLairs()

    /** Persists the whole save (state + owned lairs) as a single transaction. */
    @Transaction
    suspend fun saveAll(state: GameStateEntity, lairs: List<OwnedLairEntity>) {
        upsertGameState(state)
        if (lairs.isNotEmpty()) {
            upsertOwnedLairs(lairs)
        }
    }

    /**
     * Wholesale replace, for a full reset/wipe (Account Reset/Deletion) —
     * unlike [saveAll], which only ever upserts, this clears every
     * previously owned lair row first, so one no longer present in [lairs]
     * (e.g. every lair but Kobold Warren, after a reset) doesn't linger and
     * get read back on the next [getOwnedLairs].
     */
    @Transaction
    suspend fun replaceAll(state: GameStateEntity, lairs: List<OwnedLairEntity>) {
        clearOwnedLairs()
        upsertGameState(state)
        if (lairs.isNotEmpty()) {
            upsertOwnedLairs(lairs)
        }
    }
}
