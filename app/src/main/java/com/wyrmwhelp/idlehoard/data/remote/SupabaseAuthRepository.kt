package com.wyrmwhelp.idlehoard.data.remote

import com.wyrmwhelp.idlehoard.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
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

    override suspend fun signUp(email: String, password: String): String {
        // updateUser (not signUpWith) is what upgrades the *current* session's
        // identity in place — signUpWith would create an unrelated new user.
        supabaseClient.auth.updateUser {
            this.email = email
            this.password = password
        }
        return requireNotNull(supabaseClient.auth.currentUserOrNull()?.id) {
            "Account upgrade completed but no user id was returned"
        }
    }

    override suspend fun verifySignUpCode(email: String, code: String): String {
        // EMAIL_CHANGE, not SIGNUP: from Supabase's point of view the account
        // already exists (it's our anonymous user) and we're just setting
        // its previously-empty email, the same flow as a normal email change.
        supabaseClient.auth.verifyEmailOtp(OtpType.Email.EMAIL_CHANGE, email = email, token = code)
        return requireNotNull(supabaseClient.auth.currentUserOrNull()?.id) {
            "Verification completed but no user id was returned"
        }
    }

    override suspend fun resendSignUpCode(email: String) {
        supabaseClient.auth.resendEmail(OtpType.Email.EMAIL_CHANGE, email = email)
    }

    override suspend fun signIn(email: String, password: String): String {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return requireNotNull(supabaseClient.auth.currentUserOrNull()?.id) {
            "Sign-in completed but no user id was returned"
        }
    }

    override suspend fun signOut() {
        supabaseClient.auth.signOut()
    }

    override fun currentUserEmail(): String? =
        // Supabase returns "" (not null) for a guest's email, not just an
        // absent field — normalize blank to null so callers have one clean
        // signal for "this is a guest".
        supabaseClient.auth.currentUserOrNull()?.email?.takeIf { it.isNotBlank() }
}
