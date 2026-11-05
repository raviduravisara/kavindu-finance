package com.fintrack.kavindu.feature.reminder.viewmodel

import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimeTest {

    private val colombo = ZoneId.of("Asia/Colombo")
    private val eightPm = LocalTime.of(20, 0)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(LocalDateTime.of(year, month, day, hour, minute), colombo)

    // --- delayUntilNext ---

    @Test
    fun `time later today is due today`() {
        val delay = ReminderTime.delayUntilNext(at(2026, 11, 6, 10, 0), eightPm)
        assertEquals(Duration.ofHours(10), delay)
    }

    @Test
    fun `time already passed today is due tomorrow`() {
        val delay = ReminderTime.delayUntilNext(at(2026, 11, 6, 21, 0), eightPm)
        assertEquals(Duration.ofHours(23), delay)
    }

    @Test
    fun `time exactly now is due tomorrow`() {
        val delay = ReminderTime.delayUntilNext(at(2026, 11, 6, 20, 0), eightPm)
        assertEquals(Duration.ofHours(24), delay)
    }

    @Test
    fun `end of month moves to the first of the next month`() {
        val delay = ReminderTime.delayUntilNext(at(2026, 11, 30, 21, 30), eightPm)
        assertEquals(Duration.ofHours(22).plusMinutes(30), delay)
    }

    @Test
    fun `end of year moves to the first of January`() {
        val delay = ReminderTime.delayUntilNext(at(2026, 12, 31, 23, 59), LocalTime.of(8, 0))
        assertEquals(Duration.ofHours(8).plusMinutes(1), delay)
    }

    @Test
    fun `after a run that woke a moment early, the next reminder is tomorrow, not again tonight`() {
        val delay = ReminderTime.delayUntilNext(at(2026, 11, 6, 19, 59), eightPm, minimumGap = Duration.ofHours(1))
        assertEquals(Duration.ofHours(24).plusMinutes(1), delay)
    }

    @Test
    fun `after a late run past midnight, today's reminder still happens`() {
        // Phone was off at 8 PM; the job runs at 00:30 instead. Tonight's reminder is still due.
        val delay = ReminderTime.delayUntilNext(at(2026, 11, 7, 0, 30), eightPm, minimumGap = Duration.ofHours(1))
        assertEquals(Duration.ofHours(19).plusMinutes(30), delay)
    }

    // --- shouldRemind ---

    private val today = LocalDate.of(2026, 11, 6)

    private fun expense(date: LocalDate, origin: ExpenseOrigin) = Expense(
        id = "e-$date-$origin", amountCents = 50_000, date = date, categoryId = "food",
        categoryType = CategoryType.DISCRETIONARY, channel = "Cash", account = "Wallet",
        origin = origin, createdAt = 1L,
    )

    @Test
    fun `no expenses at all means remind`() {
        assertTrue(ReminderTime.shouldRemind(emptyList(), today))
    }

    @Test
    fun `an expense he added today means no reminder`() {
        assertFalse(ReminderTime.shouldRemind(listOf(expense(today, ExpenseOrigin.MANUAL)), today))
    }

    @Test
    fun `an expense from yesterday still means remind`() {
        assertTrue(ReminderTime.shouldRemind(listOf(expense(today.minusDays(1), ExpenseOrigin.MANUAL)), today))
    }

    @Test
    fun `entries the app created today do not count`() {
        val appMade = listOf(expense(today, ExpenseOrigin.RECURRING), expense(today, ExpenseOrigin.BALANCE_CHECK))
        assertTrue(ReminderTime.shouldRemind(appMade, today))
    }
}
