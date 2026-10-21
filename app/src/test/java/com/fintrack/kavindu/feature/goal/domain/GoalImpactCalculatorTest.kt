package com.fintrack.kavindu.feature.goal.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GoalImpactCalculatorTest {

    private val calculator = GoalImpactCalculator()
    private fun lkr(rupees: Long) = rupees * 100

    // 25 Nov – 24 Dec is a 30-day cycle, matching the guide's "39,900 ÷ 30 = 1,330 a day".
    private val thirtyDayCycle = LocalDate.of(2026, 11, 30)
    private val needed = lkr(39_900)

    @Test
    fun `UberEats 2,500 moves the date about 2 days`() {
        assertEquals(2, calculator.daysDelayed(lkr(2_500), needed, thirtyDayCycle))
    }

    @Test
    fun `coffee 1,200 moves the date about 1 day`() {
        assertEquals(1, calculator.daysDelayed(lkr(1_200), needed, thirtyDayCycle))
    }

    @Test
    fun `weekly check-in gap of 6,400 moves the date about 5 days`() {
        assertEquals(5, calculator.daysDelayed(lkr(6_400), needed, thirtyDayCycle))
    }

    @Test
    fun `a small expense rounds to 0 days`() {
        assertEquals(0, calculator.daysDelayed(lkr(300), needed, thirtyDayCycle))
    }

    @Test
    fun `nothing left to save means no delay`() {
        assertEquals(0, calculator.daysDelayed(lkr(2_500), 0, thirtyDayCycle))
    }

    @Test
    fun `zero or negative expense means no delay`() {
        assertEquals(0, calculator.daysDelayed(0, needed, thirtyDayCycle))
        assertEquals(0, calculator.daysDelayed(lkr(-100), needed, thirtyDayCycle))
    }
}
