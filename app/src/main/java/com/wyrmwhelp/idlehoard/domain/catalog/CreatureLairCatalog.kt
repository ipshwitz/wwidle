package com.wyrmwhelp.idlehoard.domain.catalog

import com.wyrmwhelp.idlehoard.domain.model.CreatureLair

/**
 * The full, ordered set of Creature Lairs available to claim. Ordered by tier,
 * which roughly follows each inhabitant's 5E SRD Challenge Rating — low-CR
 * vermin and humanoids first, working up to dragons, so unlock order mirrors
 * a party's own climb through the Monster Manual. Costs/income/timing are
 * first-pass numbers for playtesting, not final balance.
 */
object CreatureLairCatalog {

    val lairs: List<CreatureLair> = listOf(
        CreatureLair(
            id = "kobold_warren",
            name = "Kobold Warren",
            monster = "Kobold",
            challengeRating = "1/8",
            flavorText = "A tangle of tunnels rigged with snares and trapdoors, " +
                "yielding a trickle of scavenged coin.",
            tier = 0,
            baseCostGp = 10.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 1.0,
            baseProductionSeconds = 2.0,
            stewardCostGp = 100.0,
        ),
        CreatureLair(
            id = "giant_rat_burrow",
            name = "Giant Rat Burrow",
            monster = "Giant Rat",
            challengeRating = "1/8",
            flavorText = "A gnawed-out den beneath the sewers, its floor littered " +
                "with whatever the swarm has dragged home.",
            tier = 1,
            baseCostGp = 60.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 5.0,
            baseProductionSeconds = 3.0,
            stewardCostGp = 400.0,
        ),
        CreatureLair(
            id = "goblin_camp",
            name = "Goblin Camp",
            monster = "Goblin",
            challengeRating = "1/4",
            flavorText = "A ramshackle stockade of lashed-together junk, its raiders " +
                "trading in whatever they can strip from passing wagons.",
            tier = 2,
            baseCostGp = 360.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 25.0,
            baseProductionSeconds = 4.0,
            stewardCostGp = 1_800.0,
        ),
        CreatureLair(
            id = "orc_encampment",
            name = "Orc Encampment",
            monster = "Orc",
            challengeRating = "1/2",
            flavorText = "War-banners over a fortified camp, its raiding parties " +
                "returning with plunder from the borderlands.",
            tier = 3,
            baseCostGp = 2_100.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 130.0,
            baseProductionSeconds = 6.0,
            stewardCostGp = 10_000.0,
        ),
        CreatureLair(
            id = "gnoll_den",
            name = "Gnoll Den",
            monster = "Gnoll",
            challengeRating = "1/2",
            flavorText = "A bone-strewn hollow reeking of the hyena-folk's last hunt, " +
                "its pack hoarding whatever scraps of value they find.",
            tier = 4,
            baseCostGp = 12_000.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 660.0,
            baseProductionSeconds = 8.0,
            stewardCostGp = 55_000.0,
        ),
        CreatureLair(
            id = "hobgoblin_barracks",
            name = "Hobgoblin Barracks",
            monster = "Hobgoblin",
            challengeRating = "1/2",
            flavorText = "A disciplined garrison, drilled and organized, taxing every " +
                "trade route within a day's march.",
            tier = 5,
            baseCostGp = 70_000.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 3_400.0,
            baseProductionSeconds = 10.0,
            stewardCostGp = 300_000.0,
        ),
        CreatureLair(
            id = "bugbear_warcamp",
            name = "Bugbear Warcamp",
            monster = "Bugbear",
            challengeRating = "1",
            flavorText = "Hulking ambushers holed up in a ravine, sitting on a stash " +
                "too heavy for them to bother spending.",
            tier = 6,
            baseCostGp = 400_000.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 17_000.0,
            baseProductionSeconds = 12.0,
            stewardCostGp = 1_600_000.0,
        ),
        CreatureLair(
            id = "ogres_cave",
            name = "Ogre's Cave",
            monster = "Ogre",
            challengeRating = "2",
            flavorText = "A boulder-strewn cave stuffed with everything its owner was " +
                "too strong to be told no about.",
            tier = 7,
            baseCostGp = 2_300_000.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 87_000.0,
            baseProductionSeconds = 15.0,
            stewardCostGp = 9_000_000.0,
        ),
        CreatureLair(
            id = "owlbear_roost",
            name = "Owlbear Roost",
            monster = "Owlbear",
            challengeRating = "3",
            flavorText = "A high, feather-and-fur-lined nest, ringed with the glittering " +
                "castoffs of everything that never made it past the tree line.",
            tier = 8,
            baseCostGp = 13_000_000.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 440_000.0,
            baseProductionSeconds = 18.0,
            stewardCostGp = 50_000_000.0,
        ),
        CreatureLair(
            id = "troll_warren",
            name = "Troll Warren",
            monster = "Troll",
            challengeRating = "5",
            flavorText = "A regenerating menace's flooded lair, its hoard undisturbed " +
                "for decades because nothing that goes in comes back out.",
            tier = 9,
            baseCostGp = 75_000_000.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 2_200_000.0,
            baseProductionSeconds = 20.0,
            stewardCostGp = 280_000_000.0,
        ),
        CreatureLair(
            id = "wyvern_aerie",
            name = "Wyvern Aerie",
            monster = "Wyvern",
            challengeRating = "6",
            flavorText = "A wind-scoured cliffside eyrie, its stolen treasures wedged " +
                "between the stones like a magpie's nest built by something venomous.",
            tier = 10,
            baseCostGp = 430_000_000.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 11_000_000.0,
            baseProductionSeconds = 25.0,
            stewardCostGp = 1_600_000_000.0,
        ),
        CreatureLair(
            id = "young_dragons_lair",
            name = "Young Dragon's Lair",
            monster = "Young Red Dragon",
            challengeRating = "10",
            flavorText = "A smoke-blackened cavern mouth, its owner still young enough " +
                "to prize its hoard over its pride — barely.",
            tier = 11,
            baseCostGp = 2_500_000_000.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 56_000_000.0,
            baseProductionSeconds = 30.0,
            stewardCostGp = 9_000_000_000.0,
        ),
        CreatureLair(
            id = "adult_dragons_lair",
            name = "Adult Dragon's Lair",
            monster = "Adult Red Dragon",
            challengeRating = "17",
            flavorText = "A mountain hollowed out by centuries of hoarding, guarded " +
                "by a wyrm who has long since stopped counting.",
            tier = 12,
            baseCostGp = 14_000_000_000.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 280_000_000.0,
            baseProductionSeconds = 40.0,
            stewardCostGp = 50_000_000_000.0,
        ),
        CreatureLair(
            id = "ancient_dragons_hoard",
            name = "Ancient Dragon's Hoard",
            monster = "Ancient Red Dragon",
            challengeRating = "24",
            flavorText = "The legendary hoard itself — an age's worth of kingdoms' " +
                "wealth, coiled around and beneath something that remembers all of it.",
            tier = 13,
            baseCostGp = 80_000_000_000.0,
            costGrowthRate = 1.10,
            baseIncomeGp = 1_400_000_000.0,
            baseProductionSeconds = 60.0,
            stewardCostGp = 300_000_000_000.0,
        ),
    )

    private val byId: Map<String, CreatureLair> = lairs.associateBy { it.id }

    fun get(id: String): CreatureLair =
        byId[id] ?: error("Unknown Creature Lair id: $id")
}
