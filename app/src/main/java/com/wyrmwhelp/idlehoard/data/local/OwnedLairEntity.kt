package com.wyrmwhelp.idlehoard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per Creature Lair the player has claimed at least one unit of. */
@Entity(tableName = "owned_lairs")
data class OwnedLairEntity(
    @PrimaryKey val lairId: String,
    val count: Int,
    val hasSteward: Boolean,
    val cycleProgressSeconds: Double,
    val isReadyToCollect: Boolean,
)
