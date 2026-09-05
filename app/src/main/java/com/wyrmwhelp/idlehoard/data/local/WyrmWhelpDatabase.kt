package com.wyrmwhelp.idlehoard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Bumped to version 3 for the Shop ad-reward cooldown's new
 * [GameStateEntity] column (`lastPlatinumAdWatchedAtEpochMillis`) — version
 * 2 was the Boosts feature's `speedBoostLevel`/`profitBoostLevel`. No formal
 * [androidx.room.migration.Migration] exists yet — `DatabaseModule` falls
 * back to destructively recreating the database on a schema mismatch
 * instead, a pragmatic pre-release trade-off (see CLAUDE.md) since the app
 * has no real installs to preserve yet.
 */
@Database(
    entities = [GameStateEntity::class, OwnedLairEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class WyrmWhelpDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
}
