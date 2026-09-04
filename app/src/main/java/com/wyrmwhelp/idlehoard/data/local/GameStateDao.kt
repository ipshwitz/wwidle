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

    /** Persists the whole save (state + owned lairs) as a single transaction. */
    @Transaction
    suspend fun saveAll(state: GameStateEntity, lairs: List<OwnedLairEntity>) {
        upsertGameState(state)
        if (lairs.isNotEmpty()) {
            upsertOwnedLairs(lairs)
        }
    }
}
