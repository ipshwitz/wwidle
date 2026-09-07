package com.wyrmwhelp.idlehoard.ui.common

import androidx.annotation.DrawableRes
import com.wyrmwhelp.idlehoard.R

/**
 * Maps an [com.wyrmwhelp.idlehoard.domain.model.AvatarOption.id] to its real
 * portrait art (`drawable-nodpi/avatar_<f|m>_<class>.png`, copied from
 * `/assets/avatar-<f|m>-<class>.png` — see CLAUDE.md's Assets section).
 * Kept as a UI-layer `when` (same pattern as `LairRow.kt`'s private
 * `lairPortraitRes`) rather than a field on the domain model, since the
 * domain layer doesn't reference Android resources — shared (not private)
 * because both [com.wyrmwhelp.idlehoard.ui.game.GameHeader] and the avatar
 * picker dialog need it.
 */
@DrawableRes
fun avatarDrawableRes(avatarId: String): Int? = when (avatarId) {
    "f_artificer" -> R.drawable.avatar_f_artificer
    "f_barbarian" -> R.drawable.avatar_f_barbarian
    "f_bard" -> R.drawable.avatar_f_bard
    "f_cleric" -> R.drawable.avatar_f_cleric
    "f_druid" -> R.drawable.avatar_f_druid
    "f_fighter" -> R.drawable.avatar_f_fighter
    "f_monk" -> R.drawable.avatar_f_monk
    "f_paladin" -> R.drawable.avatar_f_paladin
    "f_ranger" -> R.drawable.avatar_f_ranger
    "f_rogue" -> R.drawable.avatar_f_rogue
    "f_sorcerer" -> R.drawable.avatar_f_sorcerer
    "f_warlock" -> R.drawable.avatar_f_warlock
    "f_wizard" -> R.drawable.avatar_f_wizard
    "m_artificer" -> R.drawable.avatar_m_artificer
    "m_barbarian" -> R.drawable.avatar_m_barbarian
    "m_bard" -> R.drawable.avatar_m_bard
    "m_cleric" -> R.drawable.avatar_m_cleric
    "m_druid" -> R.drawable.avatar_m_druid
    "m_fighter" -> R.drawable.avatar_m_fighter
    "m_monk" -> R.drawable.avatar_m_monk
    "m_paladin" -> R.drawable.avatar_m_paladin
    "m_ranger" -> R.drawable.avatar_m_ranger
    "m_rogue" -> R.drawable.avatar_m_rogue
    "m_sorcerer" -> R.drawable.avatar_m_sorcerer
    "m_warlock" -> R.drawable.avatar_m_warlock
    "m_wizard" -> R.drawable.avatar_m_wizard
    else -> null
}
