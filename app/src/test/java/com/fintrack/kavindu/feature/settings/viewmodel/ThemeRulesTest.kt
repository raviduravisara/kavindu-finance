package com.fintrack.kavindu.feature.settings.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRulesTest {

    @Test
    fun `system follows the phone`() {
        assertTrue(ThemeRules.isDark(ThemeMode.SYSTEM, systemIsDark = true))
        assertFalse(ThemeRules.isDark(ThemeMode.SYSTEM, systemIsDark = false))
    }

    @Test
    fun `light is light even on a dark phone`() {
        assertFalse(ThemeRules.isDark(ThemeMode.LIGHT, systemIsDark = true))
    }

    @Test
    fun `dark is dark even on a light phone`() {
        assertTrue(ThemeRules.isDark(ThemeMode.DARK, systemIsDark = false))
    }

    @Test
    fun `saved values are read back`() {
        assertEquals(ThemeMode.LIGHT, ThemeRules.fromStored("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeRules.fromStored("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeRules.fromStored("SYSTEM"))
    }

    @Test
    fun `missing or unknown value means system`() {
        assertEquals(ThemeMode.SYSTEM, ThemeRules.fromStored(null))
        assertEquals(ThemeMode.SYSTEM, ThemeRules.fromStored("PURPLE"))
    }
}
