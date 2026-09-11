package com.wyrmwhelp.idlehoard.domain.model

import com.wyrmwhelp.idlehoard.domain.catalog.CreatureLairCatalog
import kotlin.random.Random
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StewardNamesTest {

    @Test
    fun `every lair tier in the catalog produces a non-blank name`() {
        for (lair in CreatureLairCatalog.lairs) {
            val name = StewardNames.randomStewardName(lair.tier)
            assertTrue("tier ${lair.tier} produced a blank name", name.isNotBlank())
        }
    }

    @Test
    fun `a humble-tier lair and a legendary-tier lair draw from different pools`() {
        val random = Random(42)
        val humbleNames = (1..20).map { StewardNames.randomStewardName(0, random) }.toSet()
        val legendaryNames = (1..20).map { StewardNames.randomStewardName(13, random) }.toSet()
        assertTrue(humbleNames.intersect(legendaryNames).isEmpty())
    }

    @Test
    fun `rolling many times for the same tier can produce more than one name`() {
        val random = Random(7)
        val names = (1..30).map { StewardNames.randomStewardName(8, random) }.toSet()
        assertTrue("expected some variety across 30 rolls, got only ${names.size}", names.size > 1)
    }

    @Test
    fun `the same tier's pool is stable across repeated calls with a fixed seed`() {
        val first = StewardNames.randomStewardName(5, Random(123))
        val second = StewardNames.randomStewardName(5, Random(123))
        assertNotEquals("", first)
        org.junit.Assert.assertEquals(first, second)
    }
}
