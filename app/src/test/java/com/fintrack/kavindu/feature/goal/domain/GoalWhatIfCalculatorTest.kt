package com.fintrack.kavindu.feature.goal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GoalWhatIfCalculatorTest {

    private fun lkr(rupees: Long) = rupees * 100

    // 6 Nov 2026 is in the 25 Oct - 24 Nov cycle. Deadline 14 Oct 2027.
    private fun input(
        remainingRupees: Long = 490_000,
        neededRupees: Long = 40_000,
        averageRupees: Long = 0,
        today: LocalDate = LocalDate.of(2026, 11, 6),
        startDay: Int = 25,
    ) = WhatIfInput(
        remainingCents = lkr(remainingRupees),
        neededThisCycleCents = lkr(neededRupees),
        averagePerCycleCents = lkr(averageRupees),
        today = today,
        cycleStartDay = startDay,
        deadline = LocalDate.of(2027, 10, 14),
    )

    @Test
    fun `saving more finishes before the deadline`() {
        // 490,000 / 49,000 = 10 cycles: 25 Oct 2026 + 10 months - 1 day = 24 Aug 2027.
        val result = GoalWhatIfCalculator.finish(input(), lkr(49_000))!!
        assertEquals(LocalDate.of(2027, 8, 24), result.finish)
        assertEquals(WhatIfTiming.BEFORE_DEADLINE, result.timing)
        assertEquals(2, result.months)
    }

    @Test
    fun `exactly the needed pace finishes in the deadline month`() {
        // 480,000 / 40,000 = 12 cycles: ends 24 Oct 2027, the deadline's month.
        val result = GoalWhatIfCalculator.finish(input(remainingRupees = 480_000), lkr(40_000))!!
        assertEquals(LocalDate.of(2027, 10, 24), result.finish)
        assertEquals(WhatIfTiming.IN_DEADLINE_MONTH, result.timing)
        assertEquals(0, result.months)
    }

    @Test
    fun `saving less finishes after the deadline`() {
        // 480,000 / 20,000 = 24 cycles: ends 24 Oct 2028, 12 months after.
        val result = GoalWhatIfCalculator.finish(input(remainingRupees = 480_000), lkr(20_000))!!
        assertEquals(WhatIfTiming.AFTER_DEADLINE, result.timing)
        assertEquals(12, result.months)
    }

    @Test
    fun `a part cycle still counts as a whole cycle`() {
        // 480,000.01 needs a 13th cycle: ends 24 Nov 2027, 1 month after.
        val remaining = input(remainingRupees = 480_000).copy(remainingCents = lkr(480_000) + 1)
        val result = GoalWhatIfCalculator.finish(remaining, lkr(40_000))!!
        assertEquals(LocalDate.of(2027, 11, 24), result.finish)
        assertEquals(1, result.months)
    }

    @Test
    fun `saving nothing gives no finish date`() {
        assertNull(GoalWhatIfCalculator.finish(input(), 0))
    }

    @Test
    fun `follows a pay cycle that starts on the 1st`() {
        // Cycle 1 Nov - 30 Nov. 30,000 / 10,000 = 3 cycles: ends 31 Jan 2027.
        val result = GoalWhatIfCalculator.finish(input(remainingRupees = 30_000, startDay = 1), lkr(10_000))!!
        assertEquals(LocalDate.of(2027, 1, 31), result.finish)
    }

    @Test
    fun `slider starts at his real pace, rounded to LKR 1,000`() {
        assertEquals(lkr(32_000), GoalWhatIfCalculator.startingAmountCents(input(averageRupees = 32_400)))
    }

    @Test
    fun `slider starts at this cycle's need before the first full cycle`() {
        assertEquals(lkr(40_000), GoalWhatIfCalculator.startingAmountCents(input(neededRupees = 39_900, averageRupees = 0)))
    }

    @Test
    fun `slider end is twice the bigger amount, rounded up to LKR 10,000`() {
        assertEquals(lkr(80_000), GoalWhatIfCalculator.sliderMaxCents(input(neededRupees = 39_900, averageRupees = 30_000)))
        assertEquals(lkr(50_000), GoalWhatIfCalculator.sliderMaxCents(input(neededRupees = 5_000, averageRupees = 0)))
    }
}
