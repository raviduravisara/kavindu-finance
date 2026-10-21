package com.fintrack.kavindu.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun formatLkr_addsThousandSeparators_andDecimalsOnlyWhenThereAreCents() {
        assertEquals("LKR 1,250.50", Money.formatLkr(125050))
        assertEquals("LKR 490,000", Money.formatLkr(49000000))
        assertEquals("LKR 1,000,000,000", Money.formatLkr(100000000000))
    }

    @Test
    fun formatLkr_smallAmounts() {
        assertEquals("LKR 0", Money.formatLkr(0))
        assertEquals("LKR 0.05", Money.formatLkr(5))
        assertEquals("LKR 999.99", Money.formatLkr(99999))
    }

    @Test
    fun formatLkr_negativeAmountShowsMinusSign() {
        assertEquals("-LKR 1,250.50", Money.formatLkr(-125050))
    }

    @Test
    fun parseToCents_acceptsValidAmounts() {
        assertEquals(125050L, Money.parseToCents("1250.50"))
        assertEquals(125050L, Money.parseToCents("1,250.5"))
        assertEquals(125000L, Money.parseToCents("1250"))
        assertEquals(125000L, Money.parseToCents("1250."))
        assertEquals(50L, Money.parseToCents(".5"))
        assertEquals(125000L, Money.parseToCents("  1250  "))
        assertEquals(0L, Money.parseToCents("0"))
    }

    @Test
    fun parseToCents_rejectsInvalidInput() {
        assertNull(Money.parseToCents(""))
        assertNull(Money.parseToCents("   "))
        assertNull(Money.parseToCents("."))
        assertNull(Money.parseToCents("abc"))
        assertNull(Money.parseToCents("-5"))
        assertNull(Money.parseToCents("12.345"))
        assertNull(Money.parseToCents("1.2.3"))
        assertNull(Money.parseToCents("99999999999999999999"))
    }

    @Test
    fun parseThenFormat_givesTheSameAmountBack() {
        val cents = Money.parseToCents("490000")!!
        assertEquals("LKR 490,000", Money.formatLkr(cents))
    }
}
