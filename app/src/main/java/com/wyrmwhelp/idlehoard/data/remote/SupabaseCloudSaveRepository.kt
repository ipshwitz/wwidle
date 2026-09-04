package com.wyrmwhelp.idlehoard.data.remote

import com.wyrmwhelp.idlehoard.domain.model.GameState
import com.wyrmwhelp.idlehoard.domain.repository.CloudSaveRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import javax.inject.Inject

private const val TABLE_NAME = "cloud_saves"

class SupabaseCloudSaveRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : CloudSaveRepository {

    override suspend fun downloadSave(userId: String): GameState? {
        val row = supabaseClient.from(TABLE_NAME)
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeSingleOrNull<CloudSaveRow>()
        return row?.state?.toDomain()
    }

    override suspend fun uploadSave(userId: String, state: GameState) {
        val row = CloudSaveRow(
            userId = userId,
            state = state.toDto(),
            updatedAt = Instant.now().toString(),
        )
        supabaseClient.from(TABLE_NAME).upsert(row) {
            onConflict = "user_id"
        }
    }
}
