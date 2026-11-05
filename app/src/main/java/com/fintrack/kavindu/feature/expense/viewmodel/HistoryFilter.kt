package com.fintrack.kavindu.feature.expense.viewmodel

import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource

/** The type chips on the History screen. Only one is selected at a time. */
enum class HistoryType { ALL, EXPENSES, INCOME, COMMITTED, DISCRETIONARY, UNRECORDED }

/** What the History list is narrowed to. Kept in the ViewModel only; nothing is saved. */
data class HistoryFilter(
    val query: String = "",
    val type: HistoryType = HistoryType.ALL,
    val allCycles: Boolean = false, // search every pay cycle, not only the one on screen
) {
    val isActive: Boolean get() = query.isNotBlank() || type != HistoryType.ALL || allCycles
}

/**
 * History search and filters (E2). Plain Kotlin with no Android code, so it is unit tested.
 * Text matches ignore capital letters; a number also matches amounts ("2500" or "2,500" finds LKR 2,500).
 */
object HistoryFilterRules {

    fun matches(expense: Expense, categoryName: String?, filter: HistoryFilter): Boolean {
        val typeMatches = when (filter.type) {
            HistoryType.ALL, HistoryType.EXPENSES -> true
            HistoryType.INCOME -> false
            HistoryType.COMMITTED -> expense.categoryType == CategoryType.COMMITTED
            HistoryType.DISCRETIONARY -> expense.categoryType == CategoryType.DISCRETIONARY
            HistoryType.UNRECORDED -> expense.origin == ExpenseOrigin.BALANCE_CHECK
        }
        if (!typeMatches) return false
        // A recurring entry keeps its item's name (e.g. "Netflix") in the note, so the note covers that too.
        val texts = listOfNotNull(
            categoryName,
            expense.note,
            expense.channel,
            expense.account,
            UNRECORDED_LABEL.takeIf { expense.origin == ExpenseOrigin.BALANCE_CHECK },
        )
        return matchesQuery(filter.query, texts, listOf(expense.amountCents))
    }

    fun matches(income: Income, filter: HistoryFilter): Boolean {
        if (filter.type != HistoryType.ALL && filter.type != HistoryType.INCOME) return false
        val texts = listOfNotNull(income.note, sourceName(income.source))
        // Both the LKR received and the original amount, so "300" finds a USD 300 payment.
        return matchesQuery(filter.query, texts, listOf(income.lkrAmountCents, income.amountCents))
    }

    /**
     * The whole-rupee digits of a search that is a number: "2,500", "LKR 2500" and "2500.50" all give "2500".
     * Null when the search is not a number.
     */
    fun amountDigits(query: String): String? {
        val cleaned = query.replace("LKR", "", ignoreCase = true).replace(",", "").replace(" ", "")
        val whole = cleaned.substringBefore(".")
        return whole.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
    }

    private fun matchesQuery(query: String, texts: List<String>, amountsCents: List<Long>): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true
        if (texts.any { it.contains(trimmed, ignoreCase = true) }) return true
        val digits = amountDigits(trimmed) ?: return false
        return amountsCents.any { (it / 100).toString().startsWith(digits) }
    }

    // English words for searching; the same words the screen shows (strings_income.xml, strings_expense.xml).
    private const val UNRECORDED_LABEL = "Unrecorded spending"

    private fun sourceName(source: IncomeSource) = when (source) {
        IncomeSource.SALARY -> "Salary"
        IncomeSource.FREELANCE -> "Freelance"
        IncomeSource.ADSENSE -> "AdSense"
        IncomeSource.OTHER -> "Other"
    }
}
