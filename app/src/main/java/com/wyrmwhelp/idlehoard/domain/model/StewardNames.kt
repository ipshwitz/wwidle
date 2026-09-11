package com.wyrmwhelp.idlehoard.domain.model

import kotlin.random.Random

/**
 * Flavor-only D&D 5E-style names for a lair's hired Steward
 * (`OwnedLair.stewardName`) — purely cosmetic, no effect on any formula
 * anywhere in the game. Rolled fresh (see [randomStewardName]) every time
 * `GameEngine.hireSteward` actually hires one, so a lair gets a new name
 * each time its previous Steward's employment resets (a Level Up or
 * Account Reset, same as [OwnedLair.hasSteward] itself) — not one fixed
 * name forever, per explicit confirmed design.
 *
 * Grandeur scales with the lair's own [CreatureLair.tier], matching how
 * this game already ties everything else (art, rarity color, milestone
 * theming) to a lair's real 5E Challenge Rating: a Kobold Warren's
 * Steward is some nervous local hand, while an Ancient Dragon's Hoard's
 * is a legend in their own right. Deliberately **not** wired to the
 * account-wide Universal Steward (`domain/model/UniversalSteward.kt`) —
 * that one never claims a specific per-lair hire (see its own class doc,
 * "no more per-lair Steward costs"), so there's no one specific person to
 * name; only a genuine [OwnedLair.hasSteward] hire gets a name, same gate
 * `StewardEfficiency.kt` already uses for the same reason.
 */
object StewardNames {
    /** Tiers 0-3 (CR 1/8-1/2) — nervous locals and hired hands, not yet proven. */
    private val HUMBLE = listOf(
        "Pell Higgins, the Nervous",
        "Tomas Reed, Quickhands",
        "Millie Cobb, the Tidy",
        "Wendel Ashby, the Watchful",
        "Bryn Cooper, Lucky-Foot",
        "Osric Vane, the Diligent",
        "Fenna Marsh, Sharp-Eyed",
        "Gareth Pike, the Steady",
        "Ivy Hollow, the Careful",
        "Dob Fenwick, Ledger-Keeper",
    )

    /** Tiers 4-7 (CR 1/2-2) — competent guild hands and minor sellswords. */
    private val JOURNEYMAN = listOf(
        "Borin Stonefist, the Reliable",
        "Kessa Ironvale, Blade-for-Hire",
        "Dorran Blackwood, the Unshaken",
        "Sable Thorne, Coin-Counter",
        "Alric Grimsby, the Vigilant",
        "Nessa Hawke, Storm-Wise",
        "Tobias Kell, the Bold",
        "Ravena Storm, Oath-Bound",
        "Corin Vale, the Tireless",
        "Wrenna Ashgrove, Keen-Eyed",
    )

    /** Tiers 8-10 (CR 3-6) — seasoned adventurers with a real reputation. */
    private val VETERAN = listOf(
        "Sera Blackthorn, the Unyielding",
        "Captain Aldous Rook, Beastbane",
        "Vashti Nightsong, the Cunning",
        "Bram Ironclad, Wyrm-Warden",
        "Lysandra Vane, the Relentless",
        "Kael Thorncrest, Monster-Slayer",
        "Odessa Marrow, the Fearless",
        "Grendel Oakhart, Beastmaster",
        "Thessaly Ravenscar, the Watchful Blade",
        "Marrek Stormbringer, Dungeon-Warden",
    )

    /** Tiers 11-13 (CR 10-24) — legends trusted with a dragon's own hoard. */
    private val LEGENDARY = listOf(
        "Sir Aldric Emberbane, Dragonsbane",
        "Archmage Belvora Starfall, the Undying",
        "Dame Isolde Frostmourne, Hoardkeeper",
        "Lord Kaelthas Duskwarden, the Unbroken",
        "Vaelora Sunstrike, Wyrmslayer",
        "Thorne Ashcrown, the Legend",
        "Zephyra Stormcaller, Dragon-Whisperer",
        "Grand Warden Corvain Nightfall, the Eternal",
        "Seraphine Goldveil, Hoard-Sworn",
        "Baldric Ironsong, the Immortal",
    )

    private fun poolForTier(tier: Int): List<String> = when {
        tier <= 3 -> HUMBLE
        tier <= 7 -> JOURNEYMAN
        tier <= 10 -> VETERAN
        else -> LEGENDARY
    }

    /** A random name+epithet appropriate to [tier] — see this object's class doc. */
    fun randomStewardName(tier: Int, random: Random = Random.Default): String {
        val pool = poolForTier(tier)
        return pool[random.nextInt(pool.size)]
    }
}
