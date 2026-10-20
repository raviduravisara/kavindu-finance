package com.fintrack.kavindu.core.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One financial month. It runs from the start day up to the day before the start day of the next
 * month. With start day 25: 25 Sep - 24 Oct, then 25 Oct - 24 Nov, and so on.
 */
data class PayCycle(val start: LocalDate, val end: LocalDate) {

    /** True if [date] is inside this cycle, including the first and last day. */
    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    /** Number of days in this cycle, counting both the first and the last day. */
    val lengthInDays: Int
        get() = (ChronoUnit.DAYS.between(start, end) + 1).toInt()

    /** Days left from [today] to the end of the cycle, counting today. 0 once the cycle is over. */
    fun daysRemaining(today: LocalDate): Int = when {
        today.isAfter(end) -> 0
        today.isBefore(start) -> lengthInDays
        else -> (ChronoUnit.DAYS.between(today, end) + 1).toInt()
    }

    companion object {
        const val DEFAULT_START_DAY = 25
        const val MIN_START_DAY = 1

        // Limited to 28 so every month has the start day (February has only 28 days in most years).
        const val MAX_START_DAY = 28

        fun isValidStartDay(day: Int): Boolean = day in MIN_START_DAY..MAX_START_DAY

        /** The cycle that contains [date], for a cycle that starts on [startDay] (1-28). */
        fun containing(date: LocalDate, startDay: Int): PayCycle {
            require(isValidStartDay(startDay)) { "Start day must be between 1 and 28, was $startDay" }
            val start = if (date.dayOfMonth >= startDay) {
                date.withDayOfMonth(startDay)
            } else {
                date.minusMonths(1).withDayOfMonth(startDay)
            }
            return PayCycle(start = start, end = start.plusMonths(1).minusDays(1))
        }
    }
}
