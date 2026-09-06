package com.wyrmwhelp.idlehoard.domain.model

import java.time.Instant

/**
 * Aggregate root for a player's save. This is the shape that gets persisted to
 * Room locally (see `data/local/`) and will sync to Supabase's `cloud_saves`
 * table as a single jsonb blob (see CLAUDE.md) once that layer exists. This is
 * also the in-memory model [com.wyrmwhelp.idlehoard.domain.engine.GameEngine]
 * operates on.
 *
 * @property goldPieces Primary currency (gp), earned from Creature Lairs.
 * @property platinumPieces Premium currency (pp) — 5E's rarest standard coin,
 *   used here for IAP-sourced value (1 pp = 10 gp per 5E convention, flavor only
 *   for now; no automatic conversion is implemented).
 * @property gems Prestige currency earned by Leveling Up (see
 *   `domain/model/LevelUp.kt`) — the player's current *spendable* balance.
 *   Distinct from [totalGemsEarned] (which never decreases) so a future
 *   Gem-spending system can draw this down without ever letting the player
 *   earn the same Gems again by leveling up a second time.
 * @property totalGemsEarned Every Gem ever earned across every past Level
 *   Up, whether still held in [gems] or since spent — the running baseline
 *   `gemsEarnedFromLevelUp` subtracts from the lifetime-earnings target so
 *   spending (once that exists) can never be "refunded" by leveling up
 *   again for free.
 * @property lifetimeGoldEarned Total Gold Pieces ever earned from
 *   production (lair income, Time Skips, ad-doubled offline earnings) —
 *   unlike [goldPieces], this never decreases (not on spending, and not on
 *   a Level Up reset) and is what actually gates how many Gems the next
 *   Level Up can earn — see `domain/model/LevelUp.kt`.
 * @property lairs Owned-lair state keyed by [CreatureLair.id]. A missing key
 *   means that lair hasn't been claimed yet.
 * @property offlineCapHours Maximum hours of offline production the player can
 *   collect on return, upgradeable via progression.
 * @property lastSavedAt Timestamp of the last save, used to compute offline
 *   earnings on the next launch.
 * @property totalLevelUps Number of times the player has Leveled Up (prestiged).
 * @property speedBoostLevel Permanent, account-wide production-speed boosts
 *   bought with Platinum Pieces (see `domain/model/Boosts.kt`) — not tied to
 *   any one lair, unlike the ownership milestones.
 * @property profitBoostLevel Permanent, account-wide income boosts bought
 *   with Platinum Pieces, same shape as [speedBoostLevel] but for profit
 *   instead of speed.
 * @property lastPlatinumAdWatchedAt When the Shop's "Watch an Ad" rewarded
 *   placement was last watched to completion, or null if never — see
 *   `domain/model/AdRewards.kt` for the 24-hour cooldown this gates.
 */
data class GameState(
    val goldPieces: Double = 0.0,
    val platinumPieces: Double = 0.0,
    val gems: Long = 0,
    val totalGemsEarned: Long = 0,
    val lifetimeGoldEarned: Double = 0.0,
    // A brand-new save starts owning one Kobold Warren already — matching
    // AdVenture Capitalist's own onboarding (a free first Lemonade Stand) —
    // since 0 gold and 0 owned lairs would be a permanent dead end otherwise.
    val lairs: Map<String, OwnedLair> = mapOf(
        "kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1),
    ),
    val offlineCapHours: Double = 4.0,
    val lastSavedAt: Instant = Instant.now(),
    val totalLevelUps: Int = 0,
    val speedBoostLevel: Int = 0,
    val profitBoostLevel: Int = 0,
    val lastPlatinumAdWatchedAt: Instant? = null,
) {
    /** Returns the owned state for [lairId], or an unclaimed (count 0) default. */
    fun ownedLair(lairId: String): OwnedLair = lairs[lairId] ?: OwnedLair(lairId)
}
