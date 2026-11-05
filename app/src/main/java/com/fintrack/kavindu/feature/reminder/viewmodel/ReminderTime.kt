package com.fintrack.kavindu.feature.reminder.viewmodel

import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * The daily reminder's rules, in plain Kotlin (no Android), so they can be unit tested:
 * when the next reminder is due, and whether today needs one at all.
 */
object ReminderTime {

    /** The time offered the first time the reminder is switched on: 8:00 PM, after the day's spending. */
    val DEFAULT: LocalTime = LocalTime.of(20, 0)

    /**
     * How long from [now] until the next [time] of day.
     * A time that is due right now, or within [minimumGap], moves to the next day. The reminder job passes one hour
     * here after it has run, so a job that wakes a moment early can never show a second reminder the same evening.
     */
    fun delayUntilNext(now: ZonedDateTime, time: LocalTime, minimumGap: Duration = Duration.ZERO): Duration {
        var next = now.with(time)
        if (Duration.between(now, next) <= minimumGap) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    /**
     * True when nothing he added himself is recorded for [today]. Recurring entries (rent, gym) and the weekly
     * check-in are created by the app, so they do not count as "already added something today".
     */
    fun shouldRemind(expenses: List<Expense>, today: LocalDate): Boolean =
        expenses.none { it.date == today && it.origin == ExpenseOrigin.MANUAL }
}
