package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FeaturedLairEventTest {

    @Test
    fun `no owned lairs means nothing to pick`() {
        assertNull(pickFeaturedLairId(emptyMap(), excludeLairId = null))
    }

    @Test
    fun `the only owned lair is picked even if it is the one to exclude`() {
        val lairs = mapOf("kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1))
        assertEquals("kobold_warren", pickFeaturedLairId(lairs, excludeLairId = "kobold_warren"))
    }

    @Test
    fun `a lair with zero units owned is never eligible`() {
        val lairs = mapOf(
            "kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 0),
            "giant_rat_burrow" to OwnedLair(lairId = "giant_rat_burrow", count = 3),
        )
        assertEquals("giant_rat_burrow", pickFeaturedLairId(lairs, excludeLairId = null))
    }

    @Test
    fun `the excluded lair is never picked when exactly one real alternative exists`() {
        val lairs = mapOf(
            "kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1),
            "giant_rat_burrow" to OwnedLair(lairId = "giant_rat_burrow", count = 1),
        )
        repeat(50) { seed ->
            assertEquals("giant_rat_burrow", pickFeaturedLairId(lairs, excludeLairId = "kobold_warren", random = Random(seed)))
        }
    }

    @Test
    fun `never picks the excluded lair across many random rolls when multiple alternatives exist`() {
        val lairs = mapOf(
            "kobold_warren" to OwnedLair(lairId = "kobold_warren", count = 1),
            "giant_rat_burrow" to OwnedLair(lairId = "giant_rat_burrow", count = 1),
            "goblin_camp" to OwnedLair(lairId = "goblin_camp", count = 1),
        )
        repeat(100) { seed ->
            assertTrue(pickFeaturedLairId(lairs, excludeLairId = "kobold_warren", random = Random(seed)) != "kobold_warren")
        }
    }

    @Test
    fun `the random interval always falls within its documented bounds`() {
        repeat(200) { seed ->
            val interval = randomFeaturedLairInterval(Random(seed))
            assertTrue(interval.seconds in 180..480)
        }
    }
}
