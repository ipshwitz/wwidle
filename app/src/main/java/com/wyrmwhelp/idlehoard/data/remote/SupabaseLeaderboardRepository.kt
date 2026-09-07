package com.wyrmwhelp.idlehoard.data.remote

import com.wyrmwhelp.idlehoard.domain.model.LeaderboardEntry
import com.wyrmwhelp.idlehoard.domain.model.LeaderboardPeriod
import com.wyrmwhelp.idlehoard.domain.repository.LeaderboardRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

private const val LEADERBOARD_TABLE = "leaderboard_rankings"

class SupabaseLeaderboardRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : LeaderboardRepository {

    override suspend fun fetchTop(period: LeaderboardPeriod, limit: Int): List<LeaderboardEntry> {
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
        return supabaseClient.from(LEADERBOARD_TABLE)
            .select {
                filter { eq("period", period.wireName) }
                order("rank", Order.ASCENDING)
                limit(limit.toLong())
            }
            .decodeList<LeaderboardRowDto>()
            .map { it.toDomain(isCurrentUser = it.userId == currentUserId) }
    }

    override suspend fun fetchCurrentUserEntry(period: LeaderboardPeriod): LeaderboardEntry? {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return null
        return supabaseClient.from(LEADERBOARD_TABLE)
            .select {
                filter {
                    eq("period", period.wireName)
                    eq("user_id", userId)
                }
            }
            .decodeSingleOrNull<LeaderboardRowDto>()
            ?.toDomain(isCurrentUser = true)
    }
}

private fun LeaderboardRowDto.toDomain(isCurrentUser: Boolean): LeaderboardEntry = LeaderboardEntry(
    rank = rank,
    username = username,
    goldEarned = goldEarned,
    isCurrentUser = isCurrentUser,
)
