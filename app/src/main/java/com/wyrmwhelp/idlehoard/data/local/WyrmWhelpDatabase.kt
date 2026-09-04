package com.wyrmwhelp.idlehoard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameStateEntity::class, OwnedLairEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WyrmWhelpDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
}
