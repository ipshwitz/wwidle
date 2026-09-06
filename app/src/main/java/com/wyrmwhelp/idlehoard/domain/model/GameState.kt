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
 *   `domain/model/LevelUp.kt`) — **temporary, not permanent**, unlike a
 *   typical idle-game prestige currency: every Level Up *replaces* this
 *   with a fresh batch (sized off [lifetimeGoldEarned], so later Level
 *   Ups grant more) rather than adding to whatever was already held. The
 *   value isn't accumulation for its own sake — a bigger batch means a
 *   bigger income head start for *that* run, which matters once a
 *   leaderboard exists to compare how fast players ramp up, not how many
 *   Gems they've stockpiled.
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
 * @property permanentSpeedBoost2xLevel Times the permanent "2x Speed" tier has
 *   been bought with Platinum Pieces (see `domain/model/Boosts.kt`) — not tied
 *   to any one lair, unlike the ownership milestones, and unlike Gold/Gem
 *   upgrades, survives a Level Up (see `GameEngine.performLevelUp`).
 * @property permanentSpeedBoost5xLevel Same as [permanentSpeedBoost2xLevel] but
 *   for the "5x Speed" tier — every permanent Speed tier compounds together,
 *   each on its own purchase count (see [permanentSpeedMultiplier]).
 * @property permanentSpeedBoost10xLevel Same as [permanentSpeedBoost2xLevel] but
 *   for the "10x Speed" tier.
 * @property permanentProfitBoost15xLevel Times the permanent "1.5x Profit" tier
 *   has been bought — same shape as the Speed tiers above, but for income
 *   (see [permanentProfitMultiplier]).
 * @property permanentProfitBoost2xLevel Same as [permanentProfitBoost15xLevel]
 *   but for the "2x Profit" tier.
 * @property permanentProfitBoost5xLevel Same as [permanentProfitBoost15xLevel]
 *   but for the "5x Profit" tier.
 * @property permanentGemBoost15xLevel Times the permanent "1.5x Gem %" tier has
 *   been bought — boosts the per-Gem income *percentage* itself (see
 *   [permanentGemPercentMultiplier]/[gemIncomeMultiplier]), distinct from the
 *   Gem-funded Gem Efficiency upgrade (`GemUpgrades.kt`), which resets on
 *   Level Up while this doesn't.
 * @property permanentGemBoost2xLevel Same as [permanentGemBoost15xLevel] but
 *   for the "2x Gem %" tier.
 * @property permanentGemBoost5xLevel Same as [permanentGemBoost15xLevel] but
 *   for the "5x Gem %" tier.
 * @property activeTemporaryBoosts Currently-running temporary Speed/Profit
 *   boosts (`domain/model/Boosts.kt`), each with its own independent expiry —
 *   a list rather than a single "level" per category so two overlapping
 *   purchases (e.g. buying the 100x Speed boost while the 50x one is still
 *   running) stack multiplicatively for their overlap instead of one
 *   replacing the other. Pruned of expired entries once per tick (see
 *   `GameEngine.advance`); survives a Level Up like every other
 *   Platinum-funded boost.
 * @property lastPlatinumAdWatchedAt When the Shop's "Watch an Ad" rewarded
 *   placement was last watched to completion, or null if never — see
 *   `domain/model/AdRewards.kt` for the 24-hour cooldown this gates.
 * @property everythingProfitUpgradeLevel Tiers bought of the Gold Pieces
 *   "Everything Profit" upgrade line (`domain/model/GpUpgrades.kt`) —
 *   boosts every owned lair's income at once. Resets on a Level Up, same
 *   as Gold Pieces themselves (gold-sourced power is tied to the current
 *   run) — the per-lair equivalent lives on [OwnedLair.profitUpgradeLevel]
 *   instead, and resets implicitly since [lairs] itself resets.
 * @property everythingSpeedUpgradeLevel Tiers bought of the Gold Pieces
 *   "Everything Speed" upgrade line, same shape as
 *   [everythingProfitUpgradeLevel] but for cycle time instead of income.
 * @property gemEfficiencyLevel Tiers bought of the Gem-spent "Gem
 *   Efficiency" upgrade (`domain/model/GemUpgrades.kt`) — raises the
 *   per-Gem income bonus [gemIncomeMultiplier] grants. Resets on a Level
 *   Up alongside [gems] itself, since Gems are temporary (see this class's
 *   [gems] doc) — a Gem-bought upgrade to their value can't outlive them.
 */
data class GameState(
    val goldPieces: Double = 0.0,
    val platinumPieces: Double = 0.0,
    val gems: Long = 0,
    val lifetimeGoldEarned: Double = 0.0,
    val everythingProfitUpgradeLevel: Int = 0,
    val everythingSpeedUpgradeLevel: Int = 0,
    val gemEfficiencyLevel: Int = 0,
    // A brand-new save starts owning one Kobold Warren already — matching
    // AdVenture Capitalist's own onboarding (a free first Lemonade Stand) —
    // since 0 gold and 0 owned lairs would be a permanent dead end otherwise.
    val lairs: Map<String, OwnedLair> = mapOf(
        "kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1),
    ),
    val offlineCapHours: Double = 4.0,
    val lastSavedAt: Instant = Instant.now(),
    val totalLevelUps: Int = 0,
    val permanentSpeedBoost2xLevel: Int = 0,
    val permanentSpeedBoost5xLevel: Int = 0,
    val permanentSpeedBoost10xLevel: Int = 0,
    val permanentProfitBoost15xLevel: Int = 0,
    val permanentProfitBoost2xLevel: Int = 0,
    val permanentProfitBoost5xLevel: Int = 0,
    val permanentGemBoost15xLevel: Int = 0,
    val permanentGemBoost2xLevel: Int = 0,
    val permanentGemBoost5xLevel: Int = 0,
    val activeTemporaryBoosts: List<ActiveTemporaryBoost> = emptyList(),
    val lastPlatinumAdWatchedAt: Instant? = null,
) {
    /** Returns the owned state for [lairId], or an unclaimed (count 0) default. */
    fun ownedLair(lairId: String): OwnedLair = lairs[lairId] ?: OwnedLair(lairId)
}
