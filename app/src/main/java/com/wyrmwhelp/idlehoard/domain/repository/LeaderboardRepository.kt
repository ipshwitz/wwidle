package com.wyrmwhelp.idlehoard.domain.repository

import com.wyrmwhelp.idlehoard.domain.model.LeaderboardEntry
import com.wyrmwhelp.idlehoard.domain.model.LeaderboardPeriod

/**
 * Reads the precomputed `leaderboard_rankings` table (see
 * `SQL/004_create_leaderboards.sql`) — never writes to it; the rankings
 * themselves are only ever produced server-side, once an hour, by that
 * script's `refresh_leaderboards()` job.
 */
interface LeaderboardRepository {

    /** The top [limit] entries for [period], ordered by rank ascending. */
    suspend fun fetchTop(period: LeaderboardPeriod, limit: Int = 50): List<LeaderboardEntry>

    /**
     * The current signed-in player's own entry for [period], or null if
     * they're a guest, have no username set yet, or simply haven't earned
     * anything measurable in that period yet (no row exists until the next
     * hourly refresh observes them).
     */
    suspend fun fetchCurrentUserEntry(period: LeaderboardPeriod): LeaderboardEntry?
}
