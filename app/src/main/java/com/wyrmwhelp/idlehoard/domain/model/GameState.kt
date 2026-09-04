package com.wyrmwhelp.idlehoard.domain.model

import java.time.Instant

/**
 * Aggregate root for a player's save. This is the shape that gets persisted to
 * Room locally and synced to Supabase's `cloud_saves` table as a single jsonb
 * blob (see CLAUDE.md). No persistence wiring yet — this is the in-memory model
 * [com.wyrmwhelp.idlehoard.domain.engine.GameEngine] operates on.
 *
 * @property goldPieces Primary currency (gp), earned from Creature Lairs.
 * @property platinumPieces Premium currency (pp) — 5E's rarest standard coin,
 *   used here for IAP-sourced value (1 pp = 10 gp per 5E convention, flavor only
 *   for now; no automatic conversion is implemented).
 * @property scaleShards Prestige currency earned by Molting; permanent bonuses
 *   carry across Molts.
 * @property lairs Owned-lair state keyed by [CreatureLair.id]. A missing key
 *   means that lair hasn't been claimed yet.
 * @property offlineCapHours Maximum hours of offline production the player can
 *   collect on return, upgradeable via progression.
 * @property lastSavedAt Timestamp of the last save, used to compute offline
 *   earnings on the next launch.
 * @property totalMolts Number of times the player has Molted (prestiged).
 */
data class GameState(
    val goldPieces: Double = 0.0,
    val platinumPieces: Double = 0.0,
    val scaleShards: Long = 0,
    val lairs: Map<String, OwnedLair> = emptyMap(),
    val offlineCapHours: Double = 4.0,
    val lastSavedAt: Instant = Instant.now(),
    val totalMolts: Int = 0,
) {
    /** Returns the owned state for [lairId], or an unclaimed (count 0) default. */
    fun ownedLair(lairId: String): OwnedLair = lairs[lairId] ?: OwnedLair(lairId)
}
