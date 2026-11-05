package com.fintrack.kavindu.feature.expense.viewmodel

import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.RecurringItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpenseRulesTest {

    private val today = LocalDate.of(2026, 11, 13)

    private fun category(id: String, order: Int, type: CategoryType = CategoryType.DISCRETIONARY) =
        Category(id = id, name = id, type = type, sortOrder = order, createdAt = 1L)

    private fun expense(
        id: String,
        categoryId: String,
        origin: ExpenseOrigin = ExpenseOrigin.MANUAL,
        recurringItemId: String? = null,
        confirmed: Boolean = true,
    ) = Expense(
        id = id, amountCents = 10_000, date = today, categoryId = categoryId,
        categoryType = CategoryType.DISCRETIONARY, channel = "Card", account = "Main account",
        origin = origin, recurringItemId = recurringItemId, isConfirmed = confirmed, createdAt = 1L,
    )

    private fun item(id: String, active: Boolean = true) = RecurringItem(
        id = id, name = id, amountCents = 10_000, categoryId = "rent", dayOfMonth = 1, isActive = active, createdAt = 1L,
    )

    // ---- most-used first ----

    @Test
    fun `the category he taps most comes first`() {
        val categories = listOf(category("rent", 0), category("coffee", 1), category("transport", 2))
        val expenses = listOf(expense("1", "transport"), expense("2", "transport"), expense("3", "coffee"))
        val ordered = ExpenseRules.orderByUsage(categories, expenses).map { it.id }
        assertEquals(listOf("transport", "coffee", "rent"), ordered)
    }

    @Test
    fun `categories with the same use keep his own order`() {
        val categories = listOf(category("a", 2), category("b", 0), category("c", 1))
        assertEquals(listOf("b", "c", "a"), ExpenseRules.orderByUsage(categories, emptyList()).map { it.id })
    }

    @Test
    fun `entries added by the app do not push categories up the chips`() {
        val categories = listOf(category("rent", 0), category("coffee", 1))
        val expenses = List(5) { expense("r$it", "rent", ExpenseOrigin.RECURRING, "item") } +
            List(5) { expense("b$it", "rent", ExpenseOrigin.BALANCE_CHECK) } +
            expense("m", "coffee")
        assertEquals(listOf("coffee", "rent"), ExpenseRules.orderByUsage(categories, expenses).map { it.id })
    }

    // ---- save button ----

    @Test
    fun `save needs an amount above zero and a category`() {
        assertTrue(ExpenseRules.canSave(250_000, "coffee"))
        assertFalse(ExpenseRules.canSave(null, "coffee"))
        assertFalse(ExpenseRules.canSave(0, "coffee"))
        assertFalse(ExpenseRules.canSave(250_000, null))
        assertFalse(ExpenseRules.canSave(250_000, " "))
    }

    // ---- delete ----

    @Test
    fun `a manual entry can always be deleted`() {
        assertTrue(ExpenseRules.canDelete(expense("1", "coffee"), emptyList()))
    }

    @Test
    fun `an entry from an active recurring item cannot be deleted, because it would come back`() {
        val rent = expense("rent_2026-10-25", "rent", ExpenseOrigin.RECURRING, "rent")
        assertFalse(ExpenseRules.canDelete(rent, listOf(item("rent", active = true))))
    }

    @Test
    fun `an entry from a paused or removed item can be deleted`() {
        val rent = expense("rent_2026-10-25", "rent", ExpenseOrigin.RECURRING, "rent")
        assertTrue(ExpenseRules.canDelete(rent, listOf(item("rent", active = false))))
        assertTrue(ExpenseRules.canDelete(rent, emptyList()))
    }

    @Test
    fun `only an unconfirmed recurring entry is an estimate`() {
        assertTrue(ExpenseRules.isUnconfirmedEstimate(expense("u", "utilities", ExpenseOrigin.RECURRING, "u", confirmed = false)))
        assertFalse(ExpenseRules.isUnconfirmedEstimate(expense("u", "utilities", ExpenseOrigin.RECURRING, "u", confirmed = true)))
        assertFalse(ExpenseRules.isUnconfirmedEstimate(expense("m", "coffee", confirmed = false)))
    }

    // ---- edit form ----

    @Test
    fun `an amount is shown in the edit form without extra zeros`() {
        assertEquals("2500", ExpenseRules.toInputText(250_000))
        assertEquals("2500.50", ExpenseRules.toInputText(250_050))
        assertEquals("0.05", ExpenseRules.toInputText(5))
    }

    @Test
    fun `a good form is valid`() {
        val v = ExpenseRules.validate("2,500.50", "coffee", today, today, "  with Nimal ")
        assertTrue(v.isValid)
        assertEquals(250_050L, v.amountCents)
        assertEquals("with Nimal", v.note)
    }

    @Test
    fun `a bad amount is rejected`() {
        for (text in listOf("", "0", "abc", "-5", "12.345")) {
            val v = ExpenseRules.validate(text, "coffee", today, today, "")
            assertEquals(text, ExpenseFieldError.AMOUNT_INVALID, v.amountError)
            assertFalse(v.isValid)
        }
    }

    @Test
    fun `a missing category is rejected`() {
        assertEquals(ExpenseFieldError.CATEGORY_REQUIRED, ExpenseRules.validate("100", null, today, today, "").categoryError)
    }

    @Test
    fun `a future date is rejected, today and earlier are fine`() {
        assertEquals(ExpenseFieldError.DATE_IN_FUTURE, ExpenseRules.validate("100", "c", today.plusDays(1), today, "").dateError)
        assertNull(ExpenseRules.validate("100", "c", today, today, "").dateError)
        assertNull(ExpenseRules.validate("100", "c", today.minusDays(30), today, "").dateError)
    }

    @Test
    fun `an empty note is stored as no note, and 501 characters are too long`() {
        assertNull(ExpenseRules.validate("100", "c", today, today, "   ").note)
        assertEquals(ExpenseFieldError.NOTE_TOO_LONG, ExpenseRules.validate("100", "c", today, today, "x".repeat(501)).noteError)
        assertNull(ExpenseRules.validate("100", "c", today, today, "x".repeat(500)).noteError)
    }
}
