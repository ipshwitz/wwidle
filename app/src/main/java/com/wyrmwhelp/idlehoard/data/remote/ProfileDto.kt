package com.wyrmwhelp.idlehoard.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One row of the `profiles` table — a player's leaderboard username. See `SQL/003_create_profiles_table.sql`. */
@Serializable
data class ProfileRow(
    @SerialName("user_id") val userId: String,
    val username: String,
)
