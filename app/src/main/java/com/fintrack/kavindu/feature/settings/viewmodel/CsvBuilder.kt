package com.fintrack.kavindu.feature.settings.viewmodel

import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Turns income and expenses into CSV text (RFC 4180: comma-separated, quoted when needed, CRLF line ends).
 * Plain Kotlin with no Android imports, so it is unit tested. One row per entry, newest first.
 */
object CsvBuilder {

    val HEADER = listOf(
        "Date", "Type", "Category or source", "Category type", "Amount (LKR)", "Original amount",
        "Currency", "Rate (LKR per unit)", "Channel", "Account", "Origin", "Note",
    )

    private const val UNKNOWN_CATEGORY = "Unknown category"

    fun build(incomes: List<Income>, expenses: List<Expense>, categories: List<Category>): String {
        val categoryNames = categories.associate { it.id to it.name }

        val rows = expenses.map { expense ->
            Row(
                date = expense.date,
                createdAt = expense.createdAt,
                cells = listOf(
                    expense.date.toString(),
                    "Expense",
                    text(categoryNames[expense.categoryId] ?: UNKNOWN_CATEGORY),
                    categoryTypeLabel(expense.categoryType),
                    amount(expense.amountCents), // the app saves expenses in LKR
                    amount(expense.amountCents),
                    expense.currency.name,
                    "",
                    text(expense.channel),
                    text(expense.account),
                    originLabel(expense),
                    text(expense.note.orEmpty()),
                ),
            )
        } + incomes.map { income ->
            Row(
                date = income.date,
                createdAt = income.createdAt,
                cells = listOf(
                    income.date.toString(),
                    "Income",
                    sourceLabel(income.source),
                    "",
                    amount(income.lkrAmountCents), // what actually reached the bank
                    amount(income.amountCents),
                    income.currency.name,
                    // The rate saved with the entry; never recalculated. LKR has no rate.
                    if (income.currency == Currency.LKR) "" else amount(income.rateCentsPerUnit),
                    "",
                    "",
                    "",
                    text(income.note.orEmpty()),
                ),
            )
        }

        return buildString {
            appendRow(HEADER)
            rows.sortedWith(compareByDescending<Row> { it.date }.thenByDescending { it.createdAt })
                .forEach { appendRow(it.cells) }
        }
    }

    private class Row(val date: LocalDate, val createdAt: Long, val cells: List<String>)

    private fun StringBuilder.appendRow(cells: List<String>) {
        append(cells.joinToString(",") { escape(it) })
        append("\r\n")
    }

    /** Cents to a plain number a spreadsheet can add up: 125050 -> "1250.50". Exact: no Double. */
    fun amount(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()

    /**
     * Text the user typed (notes, category names, channel, account). A value starting with = + - @ would run as a
     * formula when the file is opened in Excel (CSV injection), so it gets a leading ' and stays plain text.
     */
    fun text(value: String): String =
        if (value.isNotEmpty() && value[0] in FORMULA_STARTS) "'$value" else value

    private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')

    /** Quotes a cell that contains a comma, quote or line break, and doubles any quotes inside it. */
    fun escape(cell: String): String =
        if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }

    private fun categoryTypeLabel(type: CategoryType) = when (type) {
        CategoryType.COMMITTED -> "Committed"
        CategoryType.DISCRETIONARY -> "Discretionary"
    }

    private fun sourceLabel(source: IncomeSource) = when (source) {
        IncomeSource.SALARY -> "Salary"
        IncomeSource.FREELANCE -> "Freelance"
        IncomeSource.ADSENSE -> "AdSense"
        IncomeSource.OTHER -> "Other"
    }

    private fun originLabel(expense: Expense) = when (expense.origin) {
        ExpenseOrigin.MANUAL -> "Added by hand"
        ExpenseOrigin.RECURRING -> if (expense.isConfirmed) "Recurring" else "Recurring (estimate)"
        ExpenseOrigin.BALANCE_CHECK -> "Unrecorded spending"
    }
}
