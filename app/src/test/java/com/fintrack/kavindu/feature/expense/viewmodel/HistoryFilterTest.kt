package com.fintrack.kavindu.feature.expense.viewmodel

import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HistoryFilterTest {

    private val day = LocalDate.of(2026, 11, 5)

    private fun expense(
        cents: Long = 250_000,
        type: CategoryType = CategoryType.DISCRETIONARY,
        origin: ExpenseOrigin = ExpenseOrigin.MANUAL,
        note: String? = "Daraz order",
    ) = Expense(
        id = "e", amountCents = cents, date = day, categoryId = "shopping", categoryType = type,
        channel = "Card", account = "Main account", note = note, origin = origin, createdAt = 1L,
    )

    private val usdIncome = Income(
        id = "i", source = IncomeSource.FREELANCE, amountCents = 30_000, currency = Currency.USD,
        lkrAmountCents = 9_900_000, rateCentsPerUnit = 33_000, date = day, note = "Logo project", createdAt = 1L,
    )

    private fun search(query: String) = HistoryFilter(query = query)

    @Test
    fun `empty search matches everything`() {
        assertTrue(HistoryFilterRules.matches(expense(), "Shopping", HistoryFilter()))
        assertTrue(HistoryFilterRules.matches(usdIncome, HistoryFilter()))
    }

    @Test
    fun `text search ignores capital letters`() {
        assertTrue(HistoryFilterRules.matches(expense(), "Shopping", search("daraz")))
        assertTrue(HistoryFilterRules.matches(expense(), "Shopping", search("SHOP")))
        assertFalse(HistoryFilterRules.matches(expense(), "Shopping", search("rent")))
    }

    @Test
    fun `search covers channel and account`() {
        assertTrue(HistoryFilterRules.matches(expense(), "Shopping", search("card")))
        assertTrue(HistoryFilterRules.matches(expense(), "Shopping", search("main acc")))
    }

    @Test
    fun `a number finds the amount, with or without a comma`() {
        assertTrue(HistoryFilterRules.matches(expense(), "Shopping", search("2500")))
        assertTrue(HistoryFilterRules.matches(expense(), "Shopping", search("2,500")))
        assertTrue(HistoryFilterRules.matches(expense(), "Shopping", search("LKR 2,5")))
        assertFalse(HistoryFilterRules.matches(expense(), "Shopping", search("3000")))
    }

    @Test
    fun `amount digits are read from typical searches`() {
        assertEquals("2500", HistoryFilterRules.amountDigits("2,500"))
        assertEquals("2500", HistoryFilterRules.amountDigits("LKR 2500.50"))
        assertNull(HistoryFilterRules.amountDigits("uber"))
        assertNull(HistoryFilterRules.amountDigits("."))
    }

    @Test
    fun `type chips split expenses and income`() {
        assertTrue(HistoryFilterRules.matches(expense(), "Shopping", HistoryFilter(type = HistoryType.EXPENSES)))
        assertFalse(HistoryFilterRules.matches(expense(), "Shopping", HistoryFilter(type = HistoryType.INCOME)))
        assertTrue(HistoryFilterRules.matches(usdIncome, HistoryFilter(type = HistoryType.INCOME)))
        assertFalse(HistoryFilterRules.matches(usdIncome, HistoryFilter(type = HistoryType.EXPENSES)))
    }

    @Test
    fun `committed and discretionary follow the category type`() {
        val rent = expense(type = CategoryType.COMMITTED)
        assertTrue(HistoryFilterRules.matches(rent, "Rent", HistoryFilter(type = HistoryType.COMMITTED)))
        assertFalse(HistoryFilterRules.matches(rent, "Rent", HistoryFilter(type = HistoryType.DISCRETIONARY)))
    }

    @Test
    fun `unrecorded chip shows only weekly check-in entries, which can also be searched by name`() {
        val unrecorded = expense(origin = ExpenseOrigin.BALANCE_CHECK, note = null)
        assertTrue(HistoryFilterRules.matches(unrecorded, null, HistoryFilter(type = HistoryType.UNRECORDED)))
        assertFalse(HistoryFilterRules.matches(expense(), "Shopping", HistoryFilter(type = HistoryType.UNRECORDED)))
        assertTrue(HistoryFilterRules.matches(unrecorded, null, search("unrecorded")))
    }

    @Test
    fun `a recurring item is found by its own name`() {
        val netflix = expense(origin = ExpenseOrigin.RECURRING, note = "Netflix", type = CategoryType.COMMITTED)
        assertTrue(HistoryFilterRules.matches(netflix, "Subscriptions", search("netflix")))
    }

    @Test
    fun `income is found by source, note, LKR amount or original amount`() {
        assertTrue(HistoryFilterRules.matches(usdIncome, search("freelance")))
        assertTrue(HistoryFilterRules.matches(usdIncome, search("logo")))
        assertTrue(HistoryFilterRules.matches(usdIncome, search("99,000")))
        assertTrue(HistoryFilterRules.matches(usdIncome, search("300")))
    }

    @Test
    fun `search and type must both match`() {
        val filter = HistoryFilter(query = "daraz", type = HistoryType.COMMITTED)
        assertFalse(HistoryFilterRules.matches(expense(), "Shopping", filter))
    }
}
