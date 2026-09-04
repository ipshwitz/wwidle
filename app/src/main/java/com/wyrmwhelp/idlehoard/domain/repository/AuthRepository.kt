package com.wyrmwhelp.idlehoard.domain.repository

/**
 * Player identity for cloud sync/leaderboards. Anonymous-first per CLAUDE.md:
 * [ensureSignedIn] transparently creates (or restores) an anonymous session,
 * no player action required. Linking a real identity (email/Google) to carry
 * an anonymous session across devices/reinstalls is future work.
 */
interface AuthRepository {

    /** Returns the current user id, signing in anonymously first if needed. */
    suspend fun ensureSignedIn(): String
}
