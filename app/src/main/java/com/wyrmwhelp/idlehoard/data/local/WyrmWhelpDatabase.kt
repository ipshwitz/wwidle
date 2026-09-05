package com.wyrmwhelp.idlehoard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Bumped to version 4 for the tap-to-start-load redesign's
 * [OwnedLairEntity] column swap (`isReadyToCollect` → `isLoading` +
 * `completedLoads`) — version 3 was the Shop ad-reward cooldown's
 * `lastPlatinumAdWatchedAtEpochMillis`, version 2 the Boosts feature's
 * `speedBoostLevel`/`profitBoostLevel`. No formal
 * [androidx.room.migration.Migration] exists yet — `DatabaseModule` falls
 * back to destructively recreating the database on a schema mismatch
 * instead, a pragmatic pre-release trade-off (see CLAUDE.md) since the app
 * has no real installs to preserve yet.
 */
@Database(
    entities = [GameStateEntity::class, OwnedLairEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class WyrmWhelpDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
}
