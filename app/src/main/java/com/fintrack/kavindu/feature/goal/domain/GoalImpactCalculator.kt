package com.fintrack.kavindu.feature.goal.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * How many days one discretionary expense moves the goal date (FR-32).
 *
 * Saving needed per day = needed this cycle ÷ days in the cycle.
 * Days = expense ÷ saving needed per day, rounded to the nearest whole day.
 * Example from the guide: LKR 2,500 with LKR 39,900 needed over a 30-day cycle → about 2 days.
 *
 * The screen turns the result into calm wording, e.g. "This moves your MacBook date by about 2 days".
 */
class GoalImpactCalculator {

    /**
     * @return whole days (0 means "less than a day"); 0 when there is nothing left to save.
     */
    fun daysDelayed(
        expenseCents: Long,
        neededThisCycleCents: Long,
        today: LocalDate,
        cycleStartDay: Int = 25,
    ): Int {
        if (expenseCents <= 0 || neededThisCycleCents <= 0) return 0
        val daysInCycle = CycleDates.daysInCycle(today, cycleStartDay)
        // days = expense × daysInCycle ÷ needed, done in BigDecimal so rounding is exact.
        return BigDecimal.valueOf(expenseCents)
            .multiply(BigDecimal.valueOf(daysInCycle.toLong()))
            .divide(BigDecimal.valueOf(neededThisCycleCents), 0, RoundingMode.HALF_UP)
            .toInt()
    }
}
