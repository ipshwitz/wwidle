package com.wyrmwhelp.idlehoard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Bumped to version 16 for the Universal Steward feature: one new
 * `totalAdsWatched` column (see `GameStateMappers.kt`) — see
 * `domain/model/UniversalSteward.kt`. Plain `Int` column, no JSON
 * encoding needed for a single value.
 * Version 15 was the avatar-selection feature: one new nullable
 * `selectedAvatarId` column (see `GameStateMappers.kt`) — see
 * `domain/model/Avatar.kt`/`GameHeader.kt`'s `MedallionEmblem`. Plain
 * nullable `String` column, no JSON encoding needed for a single value.
 * Version 14 was a deliberate one-off reset, explicitly requested
 * to start testing this build's app-icon/loading-screen changes from a
 * completely fresh local save — no schema change behind this bump.
 * [DatabaseModule]'s `fallbackToDestructiveMigration` (see below) means the
 * very next launch after updating simply wipes the local Room database and
 * recreates it empty, same mechanism every other version bump here relies
 * on. **Only wipes local state** — a signed-in/linked account with cloud
 * sync will still re-download and merge its existing cloud save on that
 * same launch (`GameViewModel`'s init sequence), which can restore the
 * "old" progress right back if that cloud save is still ahead. For a
 * truly blank slate on an account that's already synced, sign out (or
 * clear the app's data / reinstall) *in addition* to this bump.
 * Bumped to version 13 for the second ad-watch reward (Income boost): one
 * new `incomeBoostAdWatchTimestampsJson` column (see `GameStateMappers.kt`)
 * — see `domain/model/AdRewards.kt`'s `INCOME_BOOST_AD_MAX_SLOTS`. Version
 * 12 was the Upgrades half of the "new feature"
 * notification badge: one new `seenUpgradeOpportunitiesJson` column (see
 * `GameStateMappers.kt`) — see `domain/model/GameStateExtensions.kt`'s
 * `withUpgradeOpportunitiesSeen`. Version 11 was that same badge's
 * Stewards half: one new `seenStewardOpportunitiesJson` column (see
 * `GameStateMappers.kt`) — see
 * `domain/model/GameStateExtensions.kt`'s `withStewardOpportunitiesSeen`.
 * Version 10 was the Shop's ad-watch Speed-boost reward: one new
 * `speedBoostAdWatchTimestampsJson` column (see `GameStateMappers.kt`) —
 * see `domain/model/AdRewards.kt`. Version 9 was the Platinum Upgrades
 * feature: replaced
 * [GameStateEntity]'s old `speedBoostLevel`/`profitBoostLevel` columns with
 * nine permanent-boost-tier columns
 * (`permanentSpeedBoost2xLevel`/`5xLevel`/`10xLevel`,
 * `permanentProfitBoost15xLevel`/`2xLevel`/`5xLevel`,
 * `permanentGemBoost15xLevel`/`2xLevel`/`5xLevel`) plus one new
 * `activeTemporaryBoostsJson` column (see `GameStateMappers.kt`) — see
 * `domain/model/Boosts.kt`. Version 8 was the Upgrades feature's new
 * columns: [GameStateEntity.everythingProfitUpgradeLevel]/
 * `everythingSpeedUpgradeLevel`/`gemEfficiencyLevel`, and
 * [OwnedLairEntity.profitUpgradeLevel]/`speedUpgradeLevel`. Version 7
 * dropped `totalGemsEarned` once Gems turned out to be a deliberately
 * *temporary*, replaced-not-accumulated currency (see
 * `domain/model/LevelUp.kt`); version 6 had added that column alongside
 * `lifetimeGoldEarned` for the Level Up gating rework; version 5 was the
 * same feature's initial rename of the never-wired-up
 * `scaleShards`/`totalMolts` columns to `gems`/`totalLevelUps`; version 4
 * the tap-to-start-load redesign's [OwnedLairEntity] column swap
 * (`isReadyToCollect` → `isLoading` + `completedLoads`); version 3 the
 * Shop ad-reward cooldown's `lastPlatinumAdWatchedAtEpochMillis`; version 2
 * the Boosts feature's original `speedBoostLevel`/`profitBoostLevel`. No
 * formal [androidx.room.migration.Migration] exists yet — `DatabaseModule`
 * falls back to destructively recreating the database on a schema mismatch
 * instead, a pragmatic pre-release trade-off (see CLAUDE.md) since the app
 * has no real installs to preserve yet.
 */
@Database(
    entities = [GameStateEntity::class, OwnedLairEntity::class],
    version = 16,
    exportSchema = false,
)
abstract class WyrmWhelpDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
}
