package com.fintrack.kavindu.feature.expense.viewmodel

import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.RecurringItem
import com.fintrack.kavindu.core.util.Money
import java.time.LocalDate

/** Why a field on the edit-expense form cannot be saved. Shown under that field (the one place red is allowed). */
enum class ExpenseFieldError {
    AMOUNT_INVALID, // empty, zero, not a number, or more than 2 decimals
    CATEGORY_REQUIRED,
    DATE_IN_FUTURE, // an expense is recorded on or after the day it happened: today or earlier
    NOTE_TOO_LONG, // the security rules allow up to 500 characters
}

data class ExpenseValidation(
    val amountCents: Long?,
    val note: String?,
    val amountError: ExpenseFieldError?,
    val categoryError: ExpenseFieldError?,
    val dateError: ExpenseFieldError?,
    val noteError: ExpenseFieldError?,
) {
    val isValid: Boolean
        get() = amountError == null && categoryError == null && dateError == null && noteError == null && amountCents != null
}

/**
 * The expense rules behind quick-add, history and edit (M3, M4, M8). Plain Kotlin with no Android code,
 * so it is unit tested; the ViewModels only feed it data from Room.
 */
object ExpenseRules {

    const val MAX_NOTE_LENGTH = 500

    /**
     * Category chips, most-used first: the chip Kavindu taps most is the first one he sees.
     * Only expenses he typed himself count: rent and gym are added by the app every cycle and would always
     * crowd out coffee. Categories with the same count keep the order he set (sortOrder).
     */
    fun orderByUsage(categories: List<Category>, recentExpenses: List<Expense>): List<Category> {
        val uses = recentExpenses
            .filter { it.origin == ExpenseOrigin.MANUAL }
            .groupingBy { it.categoryId }
            .eachCount()
        return categories.sortedWith(compareByDescending<Category> { uses[it.id] ?: 0 }.thenBy { it.sortOrder })
    }

    /** Save on the quick-add sheet is enabled once there is an amount above zero and one category. */
    fun canSave(amountCents: Long?, categoryId: String?): Boolean =
        amountCents != null && amountCents > 0 && !categoryId.isNullOrBlank()

    /**
     * Whether [expense] may be deleted from history.
     *
     * An entry the app added from a recurring item is added again every time the app opens while the item is
     * active, so deleting it would only bring it back. It can be deleted once its item is paused or removed;
     * until then he can still correct its amount. Everything else can always be deleted (with Undo).
     */
    fun canDelete(expense: Expense, recurringItems: List<RecurringItem>): Boolean {
        if (expense.origin != ExpenseOrigin.RECURRING) return true
        val item = recurringItems.firstOrNull { it.id == expense.recurringItemId }
        return item == null || !item.isActive
    }

    /** An amount in cents as the edit form's text: 250000 -> "2500", 250050 -> "2500.50". */
    fun toInputText(cents: Long): String {
        val whole = cents / 100
        val fraction = cents % 100
        return if (fraction == 0L) "$whole" else "$whole.${fraction.toString().padStart(2, '0')}"
    }

    /** A utility bill the app added as an estimate and Kavindu has not confirmed yet. */
    fun isUnconfirmedEstimate(expense: Expense): Boolean =
        expense.origin == ExpenseOrigin.RECURRING && !expense.isConfirmed

    /** Checks the edit-expense form. The limits match the security rules. */
    fun validate(
        amountText: String,
        categoryId: String?,
        date: LocalDate,
        today: LocalDate,
        note: String,
    ): ExpenseValidation {
        val cents = Money.parseToCents(amountText)?.takeIf { it > 0 }
        val cleanNote = note.trim().ifEmpty { null }
        return ExpenseValidation(
            amountCents = cents,
            note = cleanNote,
            amountError = if (cents == null) ExpenseFieldError.AMOUNT_INVALID else null,
            categoryError = if (categoryId.isNullOrBlank()) ExpenseFieldError.CATEGORY_REQUIRED else null,
            dateError = if (date.isAfter(today)) ExpenseFieldError.DATE_IN_FUTURE else null,
            noteError = if ((cleanNote?.length ?: 0) > MAX_NOTE_LENGTH) ExpenseFieldError.NOTE_TOO_LONG else null,
        )
    }
}
