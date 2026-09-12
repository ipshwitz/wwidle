package com.wyrmwhelp.idlehoard.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsernameTest {

    @Test
    fun `accepts letters, digits, and underscores between 3 and 20 characters`() {
        assertTrue(isValidUsername("abc"))
        assertTrue(isValidUsername("Wyrm_Whelp_123"))
        assertTrue(isValidUsername("a".repeat(20)))
    }

    @Test
    fun `rejects too short or too long`() {
        assertFalse(isValidUsername("ab"))
        assertFalse(isValidUsername(""))
        assertFalse(isValidUsername("a".repeat(21)))
    }

    @Test
    fun `rejects spaces and punctuation`() {
        assertFalse(isValidUsername("dragon lord"))
        assertFalse(isValidUsername("dragon-lord"))
        assertFalse(isValidUsername("dragon@lord"))
        assertFalse(isValidUsername("dragon.lord"))
    }

    @Test
    fun `rejects anything starting with Anonymous, case-insensitively, reserved for auto-generated names`() {
        assertFalse(isValidUsername("Anonymous123"))
        assertFalse(isValidUsername("anonymous_guy"))
        assertFalse(isValidUsername("ANONYMOUSxyz"))
    }
}
