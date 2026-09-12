package com.wyrmwhelp.idlehoard.domain.model

/**
 * Leaderboard username rules — 3-20 characters, letters/digits/underscore
 * only. Kept deliberately simple (no spaces or punctuation) since these
 * are displayed on the leaderboard alongside arbitrary other players'
 * names. Uniqueness itself is enforced case-insensitively at the
 * database level (see `SQL/003_create_profiles_table.sql`'s functional
 * index) — this only checks shape, not availability.
 *
 * **Reserved "Anonymous" prefix (v0.50.0)** — every player is auto-assigned
 * a placeholder `AnonymousNNNNNN` username the moment their account exists
 * (`SQL/006_auto_generate_usernames.sql`'s trigger), so a real, chosen
 * username can never start with "Anonymous" (case-insensitive) — otherwise
 * a signed-in player could deliberately rename themselves to look like one
 * of these placeholders (or a specific other guest's), which would be
 * confusing on the leaderboard for no real benefit.
 */
private val USERNAME_PATTERN = Regex("^[A-Za-z0-9_]{3,20}$")

fun isValidUsername(username: String): Boolean =
    USERNAME_PATTERN.matches(username) && !username.startsWith("Anonymous", ignoreCase = true)
