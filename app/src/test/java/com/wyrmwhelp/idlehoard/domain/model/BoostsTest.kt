package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BoostsTest {

    @Test
    fun `speed boost cost grows by the growth rate per level`() {
        assertEquals(10.0, speedBoostCost(0), 0.0001)
        assertEquals(15.0, speedBoostCost(1), 0.0001)
        assertEquals(22.5, speedBoostCost(2), 0.0001)
    }

    @Test
    fun `speed boost multiplier compounds 5 percent per level`() {
        assertEquals(1.0, speedBoostMultiplier(0), 0.0001)
        assertEquals(1.05, speedBoostMultiplier(1), 0.0001)
        assertEquals(1.05 * 1.05, speedBoostMultiplier(2), 0.0001)
    }

    @Test
    fun `profit boost cost grows by the growth rate per level`() {
        assertEquals(10.0, profitBoostCost(0), 0.0001)
        assertEquals(15.0, profitBoostCost(1), 0.0001)
        assertEquals(22.5, profitBoostCost(2), 0.0001)
    }

    @Test
    fun `profit boost multiplier compounds 10 percent per level`() {
        assertEquals(1.0, profitBoostMultiplier(0), 0.0001)
        assertEquals(1.10, profitBoostMultiplier(1), 0.0001)
        assertEquals(1.10 * 1.10, profitBoostMultiplier(2), 0.0001)
    }
}
