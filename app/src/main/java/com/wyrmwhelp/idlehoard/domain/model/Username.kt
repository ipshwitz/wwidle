package com.wyrmwhelp.idlehoard.domain.model

/**
 * Leaderboard username rules — 3-20 characters, letters/digits/underscore
 * only. Kept deliberately simple (no spaces or punctuation) since these
 * will be displayed on a future leaderboard alongside arbitrary other
 * players' names. Uniqueness itself is enforced case-insensitively at the
 * database level (see `SQL/003_create_profiles_table.sql`'s functional
 * index) — this only checks shape, not availability.
 */
private val USERNAME_PATTERN = Regex("^[A-Za-z0-9_]{3,20}$")

fun isValidUsername(username: String): Boolean = USERNAME_PATTERN.matches(username)
