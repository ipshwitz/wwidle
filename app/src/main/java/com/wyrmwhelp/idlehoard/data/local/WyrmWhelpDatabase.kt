package com.wyrmwhelp.idlehoard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Bumped to version 5 for the Level Up feature's [GameStateEntity] rename
 * of the never-wired-up `scaleShards`/`totalMolts` columns to `gems`/
 * `totalLevelUps` — version 4 was the tap-to-start-load redesign's
 * [OwnedLairEntity] column swap (`isReadyToCollect` → `isLoading` +
 * `completedLoads`), version 3 the Shop ad-reward cooldown's
 * `lastPlatinumAdWatchedAtEpochMillis`, version 2 the Boosts feature's
 * `speedBoostLevel`/`profitBoostLevel`. No formal
 * [androidx.room.migration.Migration] exists yet — `DatabaseModule` falls
 * back to destructively recreating the database on a schema mismatch
 * instead, a pragmatic pre-release trade-off (see CLAUDE.md) since the app
 * has no real installs to preserve yet.
 */
@Database(
    entities = [GameStateEntity::class, OwnedLairEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class WyrmWhelpDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
}
