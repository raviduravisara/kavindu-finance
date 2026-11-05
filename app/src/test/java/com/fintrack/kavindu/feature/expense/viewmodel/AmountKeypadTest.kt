package com.fintrack.kavindu.feature.expense.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountKeypadTest {

    // Types a whole amount one key at a time, like Kavindu tapping the keypad. '<' is backspace.
    private fun type(vararg keys: Char): String = keys.fold("") { text, key ->
        when (key) {
            '.' -> AmountKeypad.appendDot(text)
            '<' -> AmountKeypad.backspace(text)
            else -> AmountKeypad.appendDigit(text, key)
        }
    }

    @Test
    fun `typing 2 5 0 0 gives 2500 and 250000 cents`() {
        val text = type('2', '5', '0', '0')
        assertEquals("2500", text)
        assertEquals(250_000L, AmountKeypad.cents(text))
    }

    @Test
    fun `a leading zero is replaced, not kept`() {
        assertEquals("5", type('0', '5'))
        assertEquals("0", type('0', '0', '0'))
    }

    @Test
    fun `a point on an empty amount starts with zero`() {
        assertEquals("0.", type('.'))
        assertEquals("0.5", type('.', '5'))
        assertEquals(50L, AmountKeypad.cents("0.5"))
    }

    @Test
    fun `only one decimal point is accepted`() {
        assertEquals("1.5", type('1', '.', '.', '5'))
    }

    @Test
    fun `at most two decimals are accepted`() {
        assertEquals("12.34", type('1', '2', '.', '3', '4', '5', '6'))
        assertEquals(1234L, AmountKeypad.cents("12.34"))
    }

    @Test
    fun `digits stop after nine whole digits`() {
        assertEquals("123456789", type('1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '0'))
    }

    @Test
    fun `decimals are still allowed after nine whole digits`() {
        assertEquals("123456789.5", type('1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '5'))
    }

    @Test
    fun `backspace removes the last key and does nothing on an empty amount`() {
        assertEquals("25", type('2', '5', '0', '<'))
        assertEquals("2", type('2', '5', '<'))
        assertEquals("", type('<', '<'))
        assertEquals("1", type('1', '.', '<'))
    }

    @Test
    fun `zero and empty amounts have no cents, so Save stays disabled`() {
        assertNull(AmountKeypad.cents(""))
        assertNull(AmountKeypad.cents("0"))
        assertNull(AmountKeypad.cents("0."))
        assertNull(AmountKeypad.cents("0.00"))
    }

    @Test
    fun `a trailing point still counts as a whole amount`() {
        assertEquals(500L, AmountKeypad.cents("5."))
    }

    @Test
    fun `display groups thousands and keeps what was typed after the point`() {
        assertEquals("0", AmountKeypad.display(""))
        assertEquals("2,500", AmountKeypad.display("2500"))
        assertEquals("1,234,567", AmountKeypad.display("1234567"))
        assertEquals("2,500.", AmountKeypad.display("2500."))
        assertEquals("2,500.5", AmountKeypad.display("2500.5"))
        assertEquals("0.", AmountKeypad.display("0."))
        assertEquals("999", AmountKeypad.display("999"))
    }
}
