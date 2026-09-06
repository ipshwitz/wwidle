package com.wyrmwhelp.idlehoard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Bumped to version 8 for the Upgrades feature's new columns:
 * [GameStateEntity.everythingProfitUpgradeLevel]/`everythingSpeedUpgradeLevel`/
 * `gemEfficiencyLevel`, and [OwnedLairEntity.profitUpgradeLevel]/
 * `speedUpgradeLevel`. Version 7 dropped `totalGemsEarned` once Gems turned
 * out to be a deliberately *temporary*, replaced-not-accumulated currency
 * (see `domain/model/LevelUp.kt`); version 6 had added that column
 * alongside `lifetimeGoldEarned` for the Level Up gating rework; version 5
 * was the same feature's initial rename of the never-wired-up
 * `scaleShards`/`totalMolts` columns to `gems`/`totalLevelUps`; version 4
 * the tap-to-start-load redesign's [OwnedLairEntity] column swap
 * (`isReadyToCollect` → `isLoading` + `completedLoads`); version 3 the
 * Shop ad-reward cooldown's `lastPlatinumAdWatchedAtEpochMillis`; version 2
 * the Boosts feature's `speedBoostLevel`/`profitBoostLevel`. No formal
 * [androidx.room.migration.Migration] exists yet — `DatabaseModule` falls
 * back to destructively recreating the database on a schema mismatch
 * instead, a pragmatic pre-release trade-off (see CLAUDE.md) since the app
 * has no real installs to preserve yet.
 */
@Database(
    entities = [GameStateEntity::class, OwnedLairEntity::class],
    version = 8,
    exportSchema = false,
)
abstract class WyrmWhelpDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
}
