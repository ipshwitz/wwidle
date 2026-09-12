package com.wyrmwhelp.idlehoard.domain.repository

/**
 * Player identity for cloud sync/leaderboards and IAP eligibility (see
 * CLAUDE.md's Monetization section). Anonymous-first: [ensureSignedIn]
 * transparently creates (or restores) an anonymous ("guest") session, no
 * player action required. [signUp] upgrades that guest session in place to
 * a permanent email/password account — same user id, same cloud save, no
 * merge needed — but doesn't take effect until [verifySignUpCode] confirms
 * the code Supabase emailed to that address (a deliberate anti-bot/anti-spam
 * gate on account creation, not just an email-ownership nicety). [signIn]
 * switches to a *different*, already-existing permanent account (a
 * different user id), so callers must reconcile local vs. that account's
 * cloud save afterward — see `GameViewModel.signIn`. [currentUsername]/
 * [setUsername]/[regenerateUsername] back the inline leaderboard-username
 * field in Settings' Account card — see `GameViewModel.submitUsername`.
 * Every account (guest included) already has a placeholder `AnonymousNNNNNN`
 * username the moment it exists (v0.50.0, `SQL/006_auto_generate_usernames.sql`'s
 * trigger), so [currentUsername] is only ever null before that script has
 * been run against a given Supabase project, not as an ordinary state.
 */
interface AuthRepository {

    /** Returns the current user id, signing in anonymously first if needed. */
    suspend fun ensureSignedIn(): String

    /**
     * Starts upgrading the current guest session to a permanent account
     * with [email]/[password] — sends a verification code to [email] but
     * does *not* take effect until [verifySignUpCode] confirms it (assuming
     * the Supabase project has "Confirm email changes" enabled — see
     * CLAUDE.md's Auth section; if it's disabled, this takes effect
     * immediately with no code needed at all, which callers detect via
     * [currentUserEmail] already being non-null right after this returns).
     * Preserves the same user id and cloud save either way. Returns the
     * (unchanged) user id. Throws on failure (e.g. weak password, email
     * already registered) — callers surface the message.
     */
    suspend fun signUp(email: String, password: String): String

    /**
     * Completes a [signUp] upgrade by verifying the [code] Supabase emailed
     * to [email]. Returns the (unchanged) user id on success. Throws on
     * failure (e.g. wrong or expired code) — callers surface the message.
     */
    suspend fun verifySignUpCode(email: String, code: String): String

    /** Re-sends the verification code for a [signUp] upgrade still pending [verifySignUpCode]. */
    suspend fun resendSignUpCode(email: String)

    /**
     * Signs in to an existing permanent account, replacing whatever session
     * was active. Returns the signed-in user's id — likely different from
     * the caller's previous one. Throws on failure (e.g. wrong password).
     */
    suspend fun signIn(email: String, password: String): String

    /**
     * Signs out of the current session entirely. Callers should call
     * [ensureSignedIn] again afterward to re-establish a fresh guest
     * session, per the anonymous-first design — local play always continues
     * regardless of cloud identity.
     */
    suspend fun signOut()

    /**
     * The current session's confirmed email, or null if it's a guest
     * (anonymous) session — or a permanent one still pending email
     * confirmation, if the Supabase project requires it. Used to gate IAP
     * visibility: guests (and unconfirmed accounts) shouldn't see it.
     */
    fun currentUserEmail(): String?

    /**
     * The current session's leaderboard username (`profiles` table), or
     * null if none has been set yet — including for guests, who are never
     * prompted for one. Suspends since, unlike [currentUserEmail], this
     * isn't cached on the Supabase auth session itself and needs its own
     * network round trip.
     */
    suspend fun currentUsername(): String?

    /**
     * Sets (or changes) the current session's leaderboard username.
     * Case-insensitively unique across all players — throws if [username]
     * is already taken by someone else, or if there's no active session.
     */
    suspend fun setUsername(username: String)

    /**
     * Rolls the current session's `profiles.username` back to a fresh
     * auto-generated `AnonymousNNNNNN` placeholder — backs Settings' "Reset
     * Account" (`GameViewModel.resetAccount`), a fresh run getting a fresh
     * anonymous identity same as everything else Reset wipes. Calls the
     * `regenerate_own_username()` RPC (`SQL/006_auto_generate_usernames.sql`)
     * rather than deleting the row outright (the pre-v0.50.0 behavior) —
     * every player is guaranteed to always have *some* username now (see
     * that script's trigger), so deleting the row instead of replacing it
     * would leave this account invisible on the leaderboard until some
     * other event happened to recreate it, which nothing does.
     */
    suspend fun regenerateUsername()

    /**
     * Permanently deletes the current session's own Supabase account —
     * backs Settings' "Delete Account" (`GameViewModel.deleteAccount`).
     * Calls the `delete_own_account` SECURITY DEFINER function
     * (`SQL/005_account_management.sql`, which must be run once against the
     * project before this works) since the client SDK has no direct
     * self-delete call; that function cascades to the account's
     * `cloud_saves`/`profiles`/leaderboard rows automatically. Irreversible
     * — the caller's session is invalid immediately afterward, so callers
     * should call [signOut] then [ensureSignedIn] to re-establish a fresh
     * guest session. Throws on failure.
     */
    suspend fun deleteAccount()
}
