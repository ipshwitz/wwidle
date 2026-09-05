package com.wyrmwhelp.idlehoard.domain.repository

/**
 * Player identity for cloud sync/leaderboards and IAP eligibility (see
 * CLAUDE.md's Monetization section). Anonymous-first: [ensureSignedIn]
 * transparently creates (or restores) an anonymous ("guest") session, no
 * player action required. [signUp] upgrades that guest session in place to
 * a permanent email/password account — same user id, same cloud save, no
 * merge needed. [signIn] switches to a *different*, already-existing
 * permanent account (a different user id), so callers must reconcile local
 * vs. that account's cloud save afterward — see `GameViewModel.signIn`.
 */
interface AuthRepository {

    /** Returns the current user id, signing in anonymously first if needed. */
    suspend fun ensureSignedIn(): String

    /**
     * Upgrades the current guest session to a permanent account with
     * [email]/[password], preserving the same user id and cloud save.
     * Returns the (unchanged) user id. Throws on failure (e.g. weak
     * password, email already registered) — callers surface the message.
     */
    suspend fun signUp(email: String, password: String): String

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
}
