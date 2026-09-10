package com.wyrmwhelp.idlehoard.domain.repository

import com.wyrmwhelp.idlehoard.domain.model.GameState

/**
 * Persists and restores the player's [GameState]. Implemented by the data
 * layer (Room locally for now; a Supabase-backed implementation will layer
 * cloud sync on top later) — the domain and presentation layers only ever
 * see this interface.
 */
interface GameRepository {

    /** Returns the saved game state, or null if there's no save yet (first launch). */
    suspend fun loadGameState(): GameState?

    /** Persists [state] as the current save, replacing whatever was there before. */
    suspend fun saveGameState(state: GameState)

    /**
     * Persists [state] as the current save, guaranteeing no stale owned-lair
     * data lingers from whatever was there before — [saveGameState] is a
     * plain upsert, so a lair no longer present in [state] (e.g. every lair
     * but Kobold Warren, right after a full reset) would otherwise still be
     * read back on the next [loadGameState]. Use for Account Reset/Deletion's
     * full wipe, not routine autosaves.
     */
    suspend fun replaceGameState(state: GameState)
}
