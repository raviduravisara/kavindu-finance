package com.fintrack.kavindu.feature.recurring.viewmodel

import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.RecurringItem
import com.fintrack.kavindu.core.util.PayCycle
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Works out which expenses the app must add for Kavindu's recurring items (M5).
 * Plain Kotlin with no Android code, so it is unit tested; [RecurringEntryGenerator] only feeds it data and
 * saves the result.
 *
 * Rent, gym and subscriptions leave his account without him doing anything ("silent spending"), so the app
 * records them for him. Every entry gets the fixed ID "<recurringItemId>_<date>", for example
 * "rent_2026-10-25". The security rules require exactly that ID for a recurring expense, so the same entry
 * written twice, even by two phones at once, is still one document and never a duplicate (decision 4.7).
 *
 * Forgiving by design (RC5): if Kavindu did not open the app for a few weeks, the entries that fell due in the
 * meantime are added when he comes back, so a gap never leaves his totals wrong. Catching up goes back at most
 * to the start of the previous pay cycle, so a long pause never floods his history.
 */
object RecurringSchedule {

    // A recurring item has no channel or account of its own; the entry can be edited in history.
    const val DEFAULT_CHANNEL = "Bank transfer"
    const val DEFAULT_ACCOUNT = "Main account"

    /** "<recurringItemId>_<yyyy-MM-dd>", the ID the security rules demand for a recurring expense. */
    fun entryId(itemId: String, date: LocalDate): String = "${itemId}_$date"

    /**
     * Every date on which [item] fell due and has not been added yet, up to and including [today].
     * Nothing for a paused item, nothing before the day the item was created, and nothing earlier than the
     * start of the previous pay cycle.
     */
    fun dueDates(item: RecurringItem, today: LocalDate, cycleStartDay: Int, zone: ZoneId = ZoneId.systemDefault()): List<LocalDate> {
        if (!item.isActive) return emptyList()
        val createdOn = Instant.ofEpochMilli(item.createdAt).atZone(zone).toLocalDate()
        val previousCycleStart = PayCycle.containing(today, cycleStartDay).start.minusMonths(1)
        val earliest = maxOf(createdOn, previousCycleStart)
        val day = item.dayOfMonth.coerceIn(PayCycle.MIN_START_DAY, PayCycle.MAX_START_DAY)

        val dates = mutableListOf<LocalDate>()
        var month = YearMonth.from(earliest)
        val lastMonth = YearMonth.from(today)
        while (!month.isAfter(lastMonth)) {
            val due = month.atDay(day)
            if (!due.isBefore(earliest) && !due.isAfter(today)) dates += due
            month = month.plusMonths(1)
        }
        return dates
    }

    /**
     * The expenses to add now. [existingIds] are the expense IDs already saved, so an entry that exists is
     * never written again (and an amount Kavindu corrected is never overwritten).
     *
     * A utility bill ([RecurringItem.isEstimate]) is added unconfirmed, as an estimate he confirms in history.
     * An item whose category no longer exists is skipped, because the entry could not say whether it is
     * committed or discretionary.
     */
    fun dueEntries(
        items: List<RecurringItem>,
        categories: List<Category>,
        existingIds: Set<String>,
        today: LocalDate,
        cycleStartDay: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Expense> {
        val categoriesById = categories.associateBy { it.id }
        return items.flatMap { item ->
            val category = categoriesById[item.categoryId] ?: return@flatMap emptyList()
            dueDates(item, today, cycleStartDay, zone)
                .map { date -> date to entryId(item.id, date) }
                .filter { (_, id) -> id !in existingIds }
                .map { (date, id) ->
                    Expense(
                        id = id,
                        amountCents = item.amountCents,
                        currency = Currency.LKR,
                        date = date,
                        categoryId = item.categoryId,
                        categoryType = category.type,
                        channel = DEFAULT_CHANNEL,
                        account = DEFAULT_ACCOUNT,
                        note = item.name,
                        origin = ExpenseOrigin.RECURRING,
                        recurringItemId = item.id,
                        isConfirmed = !item.isEstimate,
                        // The same on every phone, so two phones writing one entry write identical data.
                        createdAt = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                    )
                }
        }
    }
}
