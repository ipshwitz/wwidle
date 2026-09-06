package com.wyrmwhelp.idlehoard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Bumped to version 7 for dropping [GameStateEntity]'s `totalGemsEarned`
 * column — Gems turned out to be a deliberately *temporary*, replaced-not-
 * accumulated currency (see `domain/model/LevelUp.kt`), so the "every Gem
 * ever earned" running ledger that column existed for was never needed.
 * Version 6 had added that column alongside `lifetimeGoldEarned` for the
 * Level Up gating rework; version 5 was the same feature's initial rename
 * of the never-wired-up `scaleShards`/`totalMolts` columns to
 * `gems`/`totalLevelUps`; version 4 the tap-to-start-load redesign's
 * [OwnedLairEntity] column swap (`isReadyToCollect` → `isLoading` +
 * `completedLoads`); version 3 the Shop ad-reward cooldown's
 * `lastPlatinumAdWatchedAtEpochMillis`; version 2 the Boosts feature's
 * `speedBoostLevel`/`profitBoostLevel`. No formal
 * [androidx.room.migration.Migration] exists yet — `DatabaseModule` falls
 * back to destructively recreating the database on a schema mismatch
 * instead, a pragmatic pre-release trade-off (see CLAUDE.md) since the app
 * has no real installs to preserve yet.
 */
@Database(
    entities = [GameStateEntity::class, OwnedLairEntity::class],
    version = 7,
    exportSchema = false,
)
abstract class WyrmWhelpDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
}
