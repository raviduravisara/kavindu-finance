package com.fintrack.kavindu.feature.goal.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pay-cycle date maths used by the goal calculators.
 *
 * A pay cycle runs from [startDay] of one month to the day before [startDay] of the next
 * (default 25th to 24th, because Kavindu's salary arrives on the 25th). [startDay] is limited
 * to 1..28 so short months such as February never break the maths.
 *
 * TODO: if core/util/PayCycle already provides these functions, switch to it and delete this file.
 */
internal object CycleDates {

    /** First day of the pay cycle that contains [date]. */
    fun cycleStart(date: LocalDate, startDay: Int): LocalDate {
        require(startDay in 1..28) { "Cycle start day must be between 1 and 28" }
        val thisMonth = date.withDayOfMonth(startDay)
        return if (date.dayOfMonth >= startDay) thisMonth else thisMonth.minusMonths(1)
    }

    /** Last day of the pay cycle that contains [date]. */
    fun cycleEnd(date: LocalDate, startDay: Int): LocalDate =
        cycleStart(date, startDay).plusMonths(1).minusDays(1)

    /** Number of days in the pay cycle that contains [date]. */
    fun daysInCycle(date: LocalDate, startDay: Int): Int =
        (ChronoUnit.DAYS.between(cycleStart(date, startDay), cycleEnd(date, startDay)) + 1).toInt()

    /** Days left in the current cycle, counting today. Always at least 1. */
    fun daysLeftIncludingToday(date: LocalDate, startDay: Int): Int =
        (ChronoUnit.DAYS.between(date, cycleEnd(date, startDay)) + 1).toInt().coerceAtLeast(1)

    /**
     * Number of cycles from the cycle containing [from] to the cycle containing [to], counting both.
     * Returns 0 when [to] is in an earlier cycle than [from].
     */
    fun cyclesBetweenInclusive(from: LocalDate, to: LocalDate, startDay: Int): Int {
        val a = cycleStart(from, startDay)
        val b = cycleStart(to, startDay)
        if (b.isBefore(a)) return 0
        return (ChronoUnit.MONTHS.between(a, b) + 1).toInt()
    }
}
