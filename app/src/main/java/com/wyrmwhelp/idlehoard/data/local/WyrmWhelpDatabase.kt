package com.wyrmwhelp.idlehoard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Bumped to version 2 for the Boosts feature's two new [GameStateEntity]
 * columns (`speedBoostLevel`/`profitBoostLevel`). No formal [androidx.room.migration.Migration]
 * exists yet — `DatabaseModule` falls back to destructively recreating the
 * database on a schema mismatch instead, a pragmatic pre-release trade-off
 * (see CLAUDE.md) since the app has no real installs to preserve yet.
 */
@Database(
    entities = [GameStateEntity::class, OwnedLairEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class WyrmWhelpDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
}
