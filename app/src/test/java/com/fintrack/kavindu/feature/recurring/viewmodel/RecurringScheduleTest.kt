package com.fintrack.kavindu.feature.recurring.viewmodel

import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.RecurringItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class RecurringScheduleTest {

    private val utc = ZoneOffset.UTC

    // 13 Nov is in the 25 Oct - 24 Nov cycle (start day 25).
    private val today = LocalDate.of(2026, 11, 13)

    private fun millis(date: LocalDate) = date.atStartOfDay(utc).toInstant().toEpochMilli()

    private fun item(
        id: String = "rent",
        day: Int = 25,
        created: LocalDate = LocalDate.of(2026, 10, 1),
        active: Boolean = true,
        estimate: Boolean = false,
        amountCents: Long = 3_400_000,
        categoryId: String = "cat-rent",
    ) = RecurringItem(
        id = id, name = "Rent", amountCents = amountCents, categoryId = categoryId, dayOfMonth = day,
        isEstimate = estimate, isActive = active, createdAt = millis(created),
    )

    private val categories = listOf(
        Category("cat-rent", "Rent", CategoryType.COMMITTED, 0, 1L),
        Category("cat-util", "Utilities", CategoryType.COMMITTED, 1, 1L),
    )

    private fun dates(item: RecurringItem, on: LocalDate = today) = RecurringSchedule.dueDates(item, on, 25, utc)

    // ---- which dates are due ----

    @Test
    fun `rent due on the 25th is added once its day has come`() {
        assertEquals(listOf(LocalDate.of(2026, 10, 25)), dates(item()))
    }

    @Test
    fun `nothing is added before the due day`() {
        assertEquals(emptyList<LocalDate>(), dates(item(day = 25, created = LocalDate.of(2026, 11, 1)), on = LocalDate.of(2026, 11, 24)))
    }

    @Test
    fun `an item created today and due today is added today`() {
        assertEquals(listOf(today), dates(item(day = 13, created = today)))
    }

    @Test
    fun `an item created after its day this month waits for next month`() {
        val created = LocalDate.of(2026, 11, 14)
        assertEquals(emptyList<LocalDate>(), dates(item(day = 13, created = created), on = created))
        assertEquals(listOf(LocalDate.of(2026, 12, 13)), dates(item(day = 13, created = created), on = LocalDate.of(2026, 12, 13)))
    }

    @Test
    fun `a day-1 item falls in the cycle that started on the 25th of the month before`() {
        assertEquals(
            listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1)),
            dates(item(day = 1, created = LocalDate.of(2026, 10, 1))),
        )
    }

    @Test
    fun `after weeks away the missed entries are caught up, but only back to the previous cycle`() {
        val old = item(created = LocalDate.of(2026, 6, 1)) // set up long ago, app not opened since
        // The previous cycle started 25 Sep, so 25 Sep and 25 Oct are added; June to August are not.
        assertEquals(listOf(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 25)), dates(old))
    }

    @Test
    fun `a paused item adds nothing`() {
        assertEquals(emptyList<LocalDate>(), dates(item(active = false)))
    }

    @Test
    fun `the due day is always inside the month, even for February`() {
        val feb = RecurringSchedule.dueDates(
            item(day = 28, created = LocalDate.of(2027, 1, 1)), LocalDate.of(2027, 3, 1), 25, utc,
        )
        assertTrue(LocalDate.of(2027, 2, 28) in feb)
    }

    // ---- the expenses to add ----

    @Test
    fun `the entry uses the fixed id and the data the security rules expect`() {
        val entry = RecurringSchedule.dueEntries(listOf(item()), categories, emptySet(), today, 25, utc).single()
        assertEquals("rent_2026-10-25", entry.id)
        assertEquals(ExpenseOrigin.RECURRING, entry.origin)
        assertEquals("rent", entry.recurringItemId)
        assertEquals(LocalDate.of(2026, 10, 25), entry.date)
        assertEquals(3_400_000L, entry.amountCents)
        assertEquals(CategoryType.COMMITTED, entry.categoryType)
        assertEquals("Rent", entry.note)
        assertTrue(entry.isConfirmed)
        assertTrue(entry.createdAt > 0)
    }

    @Test
    fun `the id is item id, underscore, date, exactly as the rules require`() {
        assertEquals("abc123_2026-10-25", RecurringSchedule.entryId("abc123", LocalDate.of(2026, 10, 25)))
    }

    @Test
    fun `an entry that already exists is never written again`() {
        val entries = RecurringSchedule.dueEntries(listOf(item()), categories, setOf("rent_2026-10-25"), today, 25, utc)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `running twice on the same data gives the same entries, so two phones never differ`() {
        val a = RecurringSchedule.dueEntries(listOf(item()), categories, emptySet(), today, 25, utc)
        val b = RecurringSchedule.dueEntries(listOf(item()), categories, emptySet(), today, 25, utc)
        assertEquals(a, b) // same id and same createdAt: the second write is identical, not a conflict
    }

    @Test
    fun `a utility bill is added as an unconfirmed estimate`() {
        val util = item(id = "util", estimate = true, categoryId = "cat-util")
        val entry = RecurringSchedule.dueEntries(listOf(util), categories, emptySet(), today, 25, utc).single()
        assertFalse(entry.isConfirmed)
    }

    @Test
    fun `an item whose category is gone is skipped, not guessed`() {
        val lost = item(categoryId = "deleted")
        assertTrue(RecurringSchedule.dueEntries(listOf(lost), categories, emptySet(), today, 25, utc).isEmpty())
    }

    @Test
    fun `no items means no entries`() {
        assertTrue(RecurringSchedule.dueEntries(emptyList(), categories, emptySet(), today, 25, utc).isEmpty())
    }
}
