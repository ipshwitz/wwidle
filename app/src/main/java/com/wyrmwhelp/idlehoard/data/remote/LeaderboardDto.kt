package com.wyrmwhelp.idlehoard.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** JSON mirror of one `leaderboard_rankings` row (see `SQL/004_create_leaderboards.sql`). */
@Serializable
data class LeaderboardRowDto(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("gold_earned") val goldEarned: Double,
    val rank: Int,
)
