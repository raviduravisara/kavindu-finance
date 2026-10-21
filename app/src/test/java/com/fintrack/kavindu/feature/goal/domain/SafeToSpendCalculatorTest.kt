package com.fintrack.kavindu.feature.goal.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SafeToSpendCalculatorTest {

    private val calculator = SafeToSpendCalculator()
    private fun lkr(rupees: Long) = rupees * 100

    // 13 Nov is in the 25 Oct – 24 Nov cycle, with 12 days left counting today.
    private val today = LocalDate.of(2026, 11, 13)

    @Test
    fun `guide example - 25,433 safe to spend, about 2,119 a day for 12 days`() {
        val s = calculator.calculate(
            receivedThisCycleCents = lkr(133_333),
            committedThisCycleCents = lkr(48_000),
            goalNeededThisCycleCents = lkr(39_900),
            discretionarySpentCents = lkr(20_000),
            today = today,
        )
        assertEquals(lkr(25_433), s.amountCents)
        assertEquals(12, s.daysLeft)
        assertEquals(211_941L, s.perDayCents) // LKR 2,119.41
    }

    @Test
    fun `a cycle with no income yet gives a calm negative number, never a crash`() {
        val s = calculator.calculate(0, lkr(34_000), lkr(39_900), 0, today)
        assertEquals(lkr(-73_900), s.amountCents)
        assertEquals(0L, s.perDayCents)
    }

    @Test
    fun `overspending shows how far over, with zero per day`() {
        val s = calculator.calculate(lkr(100_000), lkr(48_000), lkr(39_900), lkr(20_000), today)
        assertEquals(lkr(-7_900), s.amountCents)
        assertEquals(0L, s.perDayCents)
    }

    @Test
    fun `last day of the cycle counts as one day left`() {
        val s = calculator.calculate(lkr(10_000), 0, 0, 0, LocalDate.of(2026, 11, 24))
        assertEquals(1, s.daysLeft)
        assertEquals(lkr(10_000), s.perDayCents)
    }

    @Test
    fun `first day of the cycle counts the whole cycle`() {
        val s = calculator.calculate(lkr(31_000), 0, 0, 0, LocalDate.of(2026, 10, 25))
        assertEquals(31, s.daysLeft) // 25 Oct – 24 Nov
        assertEquals(lkr(1_000), s.perDayCents)
    }
}
