package com.wyrmwhelp.idlehoard.domain.catalog

import com.wyrmwhelp.idlehoard.domain.model.CreatureLair

/**
 * The full, ordered set of Creature Lairs available to claim. Ordered by tier,
 * which roughly follows each inhabitant's 5E SRD Challenge Rating — low-CR
 * vermin and humanoids first, working up to dragons, so unlock order mirrors
 * a party's own climb through the Monster Manual.
 *
 * Tuning (cost/coefficient/time/income/Steward cost) for tiers 0–9 is lifted
 * directly from AdVenture Capitalist's real Earth Businesses (Lemonade Stand
 * through Oil Company), gold pieces standing in 1:1 for dollars, so the early-
 * to-mid game pacing matches a game already proven to feel right. Earth only
 * has 10 businesses, so tiers 10–13 (Wyvern Aerie onward — D&D has no Earth
 * equivalent) extend the same patterns AdCap itself uses tier-to-tier: ~12x
 * cost per tier, income ≈ 50% of cost per cycle (true for 6 of Earth's 10
 * tiers), and a gently *decreasing* cost-growth coefficient (later lairs are
 * cheaper, per-unit, to keep stacking them from becoming impossible). Cycle
 * time is the one place we deliberately diverge: AdCap's real jump from Bank
 * to Oil Company is a further 6x (to ~10 hours), and it relies on repeated
 * Angel Investor prestige resets to claw that back down — a system we don't
 * have yet (see Molt in CLAUDE.md's open questions) — so tiers 10–13 use a
 * tempered ~1.5–2x per tier instead, capping the current top end at a few
 * days rather than compounding into weeks.
 */
object CreatureLairCatalog {

    val lairs: List<CreatureLair> = listOf(
        // AdCap: Lemonade Stand ($3.738, x1.07, 0.6s, $1, manager $1,000)
        CreatureLair(
            id = "kobold_warren",
            name = "Kobold Warren",
            monster = "Kobold",
            challengeRating = "1/8",
            flavorText = "A tangle of tunnels rigged with snares and trapdoors, " +
                "yielding a trickle of scavenged coin.",
            tier = 0,
            baseCostGp = 3.738,
            costGrowthRate = 1.07,
            baseIncomeGp = 1.0,
            baseProductionSeconds = 0.6,
            stewardCostGp = 1_000.0,
        ),
        // AdCap: Newspaper Delivery ($60, x1.15, 3s, $60, manager $15,000)
        CreatureLair(
            id = "giant_rat_burrow",
            name = "Giant Rat Burrow",
            monster = "Giant Rat",
            challengeRating = "1/8",
            flavorText = "A gnawed-out den beneath the sewers, its floor littered " +
                "with whatever the swarm has dragged home.",
            tier = 1,
            baseCostGp = 60.0,
            costGrowthRate = 1.15,
            baseIncomeGp = 60.0,
            baseProductionSeconds = 3.0,
            stewardCostGp = 15_000.0,
        ),
        // AdCap: Car Wash ($720, x1.14, 6s, $540, manager $100,000)
        CreatureLair(
            id = "goblin_camp",
            name = "Goblin Camp",
            monster = "Goblin",
            challengeRating = "1/4",
            flavorText = "A ramshackle stockade of lashed-together junk, its raiders " +
                "trading in whatever they can strip from passing wagons.",
            tier = 2,
            baseCostGp = 720.0,
            costGrowthRate = 1.14,
            baseIncomeGp = 540.0,
            baseProductionSeconds = 6.0,
            stewardCostGp = 100_000.0,
        ),
        // AdCap: Pizza Delivery ($8,640, x1.13, 12s, $4,320, manager cost not
        // documented on the wiki — interpolated between Car Wash's $100K and
        // Donut Shop's $1.2M).
        CreatureLair(
            id = "orc_encampment",
            name = "Orc Encampment",
            monster = "Orc",
            challengeRating = "1/2",
            flavorText = "War-banners over a fortified camp, its raiding parties " +
                "returning with plunder from the borderlands.",
            tier = 3,
            baseCostGp = 8_640.0,
            costGrowthRate = 1.13,
            baseIncomeGp = 4_320.0,
            baseProductionSeconds = 12.0,
            stewardCostGp = 500_000.0,
        ),
        // AdCap: Donut Shop ($103,680, x1.12, 24s, $51,840, manager $1,200,000)
        CreatureLair(
            id = "gnoll_den",
            name = "Gnoll Den",
            monster = "Gnoll",
            challengeRating = "1/2",
            flavorText = "A bone-strewn hollow reeking of the hyena-folk's last hunt, " +
                "its pack hoarding whatever scraps of value they find.",
            tier = 4,
            baseCostGp = 103_680.0,
            costGrowthRate = 1.12,
            baseIncomeGp = 51_840.0,
            baseProductionSeconds = 24.0,
            stewardCostGp = 1_200_000.0,
        ),
        // AdCap: Shrimp Boat ($1,244,160, x1.11, 96s, $622,080, manager $10,000,000)
        CreatureLair(
            id = "hobgoblin_barracks",
            name = "Hobgoblin Barracks",
            monster = "Hobgoblin",
            challengeRating = "1/2",
            flavorText = "A disciplined garrison, drilled and organized, taxing every " +
                "trade route within a day's march.",
            tier = 5,
            baseCostGp = 1_244_160.0,
            costGrowthRate = 1.11,
            baseIncomeGp = 622_080.0,
            baseProductionSeconds = 96.0,
            stewardCostGp = 10_000_000.0,
        ),
        // AdCap: Hockey Team ($14,929,920, x1.10, 384s, $7,464,960, manager $111,111,111)
        CreatureLair(
            id = "bugbear_warcamp",
            name = "Bugbear Warcamp",
            monster = "Bugbear",
            challengeRating = "1",
            flavorText = "Hulking ambushers holed up in a ravine, sitting on a stash " +
                "too heavy for them to bother spending.",
            tier = 6,
            baseCostGp = 14_929_920.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 7_464_960.0,
            baseProductionSeconds = 384.0,
            stewardCostGp = 111_111_111.0,
        ),
        // AdCap: Movie Studio ($179,159,040, x1.09, 1536s, $89,579,520, manager $555,555,555)
        CreatureLair(
            id = "ogres_cave",
            name = "Ogre's Cave",
            monster = "Ogre",
            challengeRating = "2",
            flavorText = "A boulder-strewn cave stuffed with everything its owner was " +
                "too strong to be told no about.",
            tier = 7,
            baseCostGp = 179_159_040.0,
            costGrowthRate = 1.09,
            baseIncomeGp = 89_579_520.0,
            baseProductionSeconds = 1_536.0,
            stewardCostGp = 555_555_555.0,
        ),
        // AdCap: Bank ($2,149,908,480, x1.08, 6144s, $1,074,954,240, manager $10,000,000,000)
        CreatureLair(
            id = "owlbear_roost",
            name = "Owlbear Roost",
            monster = "Owlbear",
            challengeRating = "3",
            flavorText = "A high, feather-and-fur-lined nest, ringed with the glittering " +
                "castoffs of everything that never made it past the tree line.",
            tier = 8,
            baseCostGp = 2_149_908_480.0,
            costGrowthRate = 1.08,
            baseIncomeGp = 1_074_954_240.0,
            baseProductionSeconds = 6_144.0,
            stewardCostGp = 10_000_000_000.0,
        ),
        // AdCap: Oil Company ($25,798,901,760, x1.07, 36864s/~10.2h,
        // $29,668,737,024, manager $100,000,000,000)
        CreatureLair(
            id = "troll_warren",
            name = "Troll Warren",
            monster = "Troll",
            challengeRating = "5",
            flavorText = "A regenerating menace's flooded lair, its hoard undisturbed " +
                "for decades because nothing that goes in comes back out.",
            tier = 9,
            baseCostGp = 25_798_901_760.0,
            costGrowthRate = 1.07,
            baseIncomeGp = 29_668_737_024.0,
            baseProductionSeconds = 36_864.0,
            stewardCostGp = 100_000_000_000.0,
        ),
        // Beyond Earth: continuing AdCap's ~12x cost-per-tier and ~50%
        // income/cost ratio, tempered cycle-time growth (see class doc).
        CreatureLair(
            id = "wyvern_aerie",
            name = "Wyvern Aerie",
            monster = "Wyvern",
            challengeRating = "6",
            flavorText = "A wind-scoured cliffside eyrie, its stolen treasures wedged " +
                "between the stones like a magpie's nest built by something venomous.",
            tier = 10,
            baseCostGp = 309_586_821_120.0,
            costGrowthRate = 1.06,
            baseIncomeGp = 154_793_410_560.0,
            baseProductionSeconds = 73_728.0,
            stewardCostGp = 1_100_000_000_000.0,
        ),
        CreatureLair(
            id = "young_dragons_lair",
            name = "Young Dragon's Lair",
            monster = "Young Red Dragon",
            challengeRating = "10",
            flavorText = "A smoke-blackened cavern mouth, its owner still young enough " +
                "to prize its hoard over its pride — barely.",
            tier = 11,
            baseCostGp = 3_715_041_853_440.0,
            costGrowthRate = 1.05,
            baseIncomeGp = 1_857_520_926_720.0,
            baseProductionSeconds = 110_592.0,
            stewardCostGp = 11_000_000_000_000.0,
        ),
        CreatureLair(
            id = "adult_dragons_lair",
            name = "Adult Dragon's Lair",
            monster = "Adult Red Dragon",
            challengeRating = "17",
            flavorText = "A mountain hollowed out by centuries of hoarding, guarded " +
                "by a wyrm who has long since stopped counting.",
            tier = 12,
            baseCostGp = 44_580_502_241_280.0,
            costGrowthRate = 1.04,
            baseIncomeGp = 22_290_251_120_640.0,
            baseProductionSeconds = 165_888.0,
            stewardCostGp = 110_000_000_000_000.0,
        ),
        CreatureLair(
            id = "ancient_dragons_hoard",
            name = "Ancient Dragon's Hoard",
            monster = "Ancient Red Dragon",
            challengeRating = "24",
            flavorText = "The legendary hoard itself — an age's worth of kingdoms' " +
                "wealth, coiled around and beneath something that remembers all of it.",
            tier = 13,
            baseCostGp = 534_966_026_895_360.0,
            costGrowthRate = 1.03,
            baseIncomeGp = 267_483_013_447_680.0,
            baseProductionSeconds = 248_832.0,
            stewardCostGp = 1_100_000_000_000_000.0,
        ),
    )

    private val byId: Map<String, CreatureLair> = lairs.associateBy { it.id }

    fun get(id: String): CreatureLair =
        byId[id] ?: error("Unknown Creature Lair id: $id")
}
