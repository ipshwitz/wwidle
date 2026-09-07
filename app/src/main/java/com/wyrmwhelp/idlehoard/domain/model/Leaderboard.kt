package com.wyrmwhelp.idlehoard.domain.model

/**
 * Which of the three ranked boards is being shown — see
 * `SQL/004_create_leaderboards.sql`'s `refresh_leaderboards()`, which
 * computes all three the same way: [wireName] is the `period` value stored
 * in `leaderboard_rankings` and filtered on in
 * `SupabaseLeaderboardRepository`.
 */
enum class LeaderboardPeriod(val wireName: String, val label: String) {
    ALL_TIME("all_time", "All-Time"),
    WEEKLY("weekly", "Weekly"),
    MONTHLY("monthly", "Monthly"),
}

/**
 * One row of a leaderboard — [rank] and [goldEarned] are whatever
 * `refresh_leaderboards()` last computed (hourly, not live), so this is
 * always up to an hour stale. [isCurrentUser] drives the highlighted-row
 * treatment in `LeaderboardContent`.
 */
data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val goldEarned: Double,
    val isCurrentUser: Boolean,
)
