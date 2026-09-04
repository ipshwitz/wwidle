package com.wyrmwhelp.idlehoard.data.remote

import com.wyrmwhelp.idlehoard.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Inject

class SupabaseAuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : AuthRepository {

    override suspend fun ensureSignedIn(): String {
        supabaseClient.auth.awaitInitialization()

        supabaseClient.auth.currentUserOrNull()?.id?.let { return it }

        supabaseClient.auth.signInAnonymously()
        return requireNotNull(supabaseClient.auth.currentUserOrNull()?.id) {
            "Anonymous sign-in completed but no user id was returned"
        }
    }
}
