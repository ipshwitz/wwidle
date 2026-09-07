package com.wyrmwhelp.idlehoard.domain.model

/** Which of the two pre-drawn art sets an [AvatarOption] belongs to. */
enum class AvatarGender { MALE, FEMALE }

/**
 * One selectable player avatar — a D&D 5E class portrait in one of two
 * genders (`/assets/avatar-<f|m>-<class>.png`, 26 total). [id] (e.g.
 * `"f_wizard"`) is what's actually persisted on [GameState.selectedAvatarId]
 * — stable across catalog reordering, unlike a list index. Purely metadata;
 * mapping an id to its actual drawable resource is a UI-layer concern (see
 * `ui/common/AvatarArt.kt`'s `avatarDrawableRes`) since the domain layer
 * doesn't reference Android resources.
 */
data class AvatarOption(
    val id: String,
    val gender: AvatarGender,
    val className: String,
)

/** Every playable D&D 5E class with avatar art, in the order they're shown. */
private val AVATAR_CLASSES = listOf(
    "Artificer", "Barbarian", "Bard", "Cleric", "Druid", "Fighter", "Monk",
    "Paladin", "Ranger", "Rogue", "Sorcerer", "Warlock", "Wizard",
)

/** All 26 selectable avatars — one [AvatarOption] per gender/class combination. */
val AVATAR_CATALOG: List<AvatarOption> = AvatarGender.entries.flatMap { gender ->
    val prefix = if (gender == AvatarGender.MALE) "m" else "f"
    AVATAR_CLASSES.map { className -> AvatarOption(id = "${prefix}_${className.lowercase()}", gender = gender, className = className) }
}

/** Whether [avatarId] is a real, currently-known avatar — guards [GameEngine.selectAvatar]. */
fun isValidAvatarId(avatarId: String): Boolean = AVATAR_CATALOG.any { it.id == avatarId }
