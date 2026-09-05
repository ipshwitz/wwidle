package com.wyrmwhelp.idlehoard.ui.navigation

import kotlinx.serialization.Serializable

/** The main lair screen — the app's start destination. */
@Serializable
object GameRoute

/**
 * A placeholder for a section whose real screen doesn't exist yet. [title] is
 * shown as-is, so it should already be the human-facing menu label.
 */
@Serializable
data class ComingSoonRoute(val title: String)
