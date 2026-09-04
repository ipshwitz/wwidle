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
}
