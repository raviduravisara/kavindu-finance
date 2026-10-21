package com.fintrack.kavindu.feature.goal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalCalculatorTest {

    private val calculator = GoalCalculator()
    private fun lkr(rupees: Long) = rupees * 100

    // Kavindu on day one: LKR 490,000 target, LKR 11,200 saved, 12 pay cycles until the deadline.
    private val dayOne = GoalInput(
        targetCents = lkr(490_000),
        startingSavingsCents = lkr(11_200),
        savedSinceStartCents = 0,
        savedThisCycleCents = 0,
        goalStartDate = LocalDate.of(2026, 10, 25),
        deadline = LocalDate.of(2027, 10, 24),
        today = LocalDate.of(2026, 10, 25),
    )

    @Test
    fun `day one matches the guide - 478,800 over 12 cycles is 39,900 a cycle`() {
        val p = calculator.calculate(dayOne)
        assertEquals(lkr(11_200), p.savedSoFarCents)
        assertEquals(lkr(478_800), p.remainingCents)
        assertEquals(12, p.cyclesLeft)
        assertEquals(lkr(39_900), p.neededThisCycleCents)
        assertEquals(lkr(39_900), p.toGoThisCycleCents)
        assertEquals(2, p.percent)
    }

    @Test
    fun `first cycle is never judged as behind`() {
        val p = calculator.calculate(dayOne)
        assertEquals(GoalStatus.ON_TRACK, p.status)
        assertNull(p.projectedFinish) // no completed cycle yet, so no honest estimate
    }

    @Test
    fun `needed this cycle stays the same while he saves during the cycle`() {
        val midCycle = dayOne.copy(
            savedSinceStartCents = lkr(25_000),
            savedThisCycleCents = lkr(25_000),
            today = LocalDate.of(2026, 11, 13),
        )
        val p = calculator.calculate(midCycle)
        assertEquals(lkr(39_900), p.neededThisCycleCents)
        assertEquals(lkr(14_900), p.toGoThisCycleCents)
    }

    @Test
    fun `saving less than needed in completed cycles is a little behind, with a later finish`() {
        val behind = dayOne.copy(
            goalStartDate = LocalDate.of(2026, 8, 25),
            savedSinceStartCents = lkr(30_000), // 20,000 over two finished cycles + 10,000 now
            savedThisCycleCents = lkr(10_000),
            today = LocalDate.of(2026, 11, 13),
        )
        val p = calculator.calculate(behind)
        assertEquals(GoalStatus.A_LITTLE_BEHIND, p.status)
        assertNotNull(p.projectedFinish)
        assertTrue(p.projectedFinish!!.isAfter(behind.deadline))
    }

    @Test
    fun `saving enough in completed cycles is on track and finishes by the deadline`() {
        val onTrack = dayOne.copy(
            goalStartDate = LocalDate.of(2026, 8, 25),
            savedSinceStartCents = lkr(100_000), // 50,000 per finished cycle
            savedThisCycleCents = 0,
            today = LocalDate.of(2026, 10, 30),
        )
        val p = calculator.calculate(onTrack)
        assertEquals(GoalStatus.ON_TRACK, p.status)
        assertTrue(!p.projectedFinish!!.isAfter(onTrack.deadline))
    }

    @Test
    fun `goal reached shows 100 percent and nothing more to save`() {
        val p = calculator.calculate(dayOne.copy(savedSinceStartCents = lkr(500_000)))
        assertEquals(GoalStatus.REACHED, p.status)
        assertEquals(100, p.percent)
        assertEquals(0L, p.remainingCents)
        assertEquals(0L, p.neededThisCycleCents)
    }

    @Test
    fun `deadline passed shows the shortfall as needed now`() {
        val p = calculator.calculate(dayOne.copy(today = LocalDate.of(2027, 11, 1)))
        assertEquals(GoalStatus.DEADLINE_PASSED, p.status)
        assertEquals(0, p.cyclesLeft)
        assertEquals(lkr(478_800), p.neededThisCycleCents)
    }

    @Test
    fun `spending more than earned gives a negative saved total without crashing`() {
        val p = calculator.calculate(dayOne.copy(savedSinceStartCents = lkr(-20_000), savedThisCycleCents = lkr(-20_000)))
        assertEquals(lkr(-8_800), p.savedSoFarCents)
        assertEquals(0, p.percent)
        assertEquals(lkr(498_800), p.remainingCents)
    }

    @Test
    fun `works with a cycle that starts on the 1st`() {
        val p = calculator.calculate(
            dayOne.copy(
                cycleStartDay = 1,
                goalStartDate = LocalDate.of(2026, 11, 1),
                today = LocalDate.of(2026, 11, 1),
                deadline = LocalDate.of(2027, 10, 31),
            )
        )
        assertEquals(12, p.cyclesLeft)
        assertEquals(lkr(39_900), p.neededThisCycleCents)
    }
}
