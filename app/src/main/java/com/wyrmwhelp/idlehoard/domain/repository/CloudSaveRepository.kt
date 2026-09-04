package com.wyrmwhelp.idlehoard.domain.repository

import com.wyrmwhelp.idlehoard.domain.model.GameState

/**
 * Cloud-synced save, mirroring [GameRepository] but backed by Supabase's
 * `cloud_saves` table (entire [GameState] as one jsonb blob per CLAUDE.md)
 * instead of local Room.
 */
interface CloudSaveRepository {

    /** Returns [userId]'s cloud save, or null if they have none yet. */
    suspend fun downloadSave(userId: String): GameState?

    /** Uploads [state] as [userId]'s cloud save, replacing whatever was there. */
    suspend fun uploadSave(userId: String, state: GameState)
}
