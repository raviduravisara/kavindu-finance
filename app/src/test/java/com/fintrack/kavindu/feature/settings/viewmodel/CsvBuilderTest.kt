package com.fintrack.kavindu.feature.settings.viewmodel

import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CsvBuilderTest {

    private val food = Category(id = "food", name = "Food", type = CategoryType.DISCRETIONARY, sortOrder = 1, createdAt = 1L)

    private fun expense(
        id: String = "e1",
        cents: Long = 125_050,
        date: LocalDate = LocalDate.of(2026, 11, 5),
        note: String? = null,
        origin: ExpenseOrigin = ExpenseOrigin.MANUAL,
        isConfirmed: Boolean = true,
        categoryId: String = "food",
        createdAt: Long = 1L,
    ) = Expense(
        id = id, amountCents = cents, date = date, categoryId = categoryId,
        categoryType = CategoryType.DISCRETIONARY, channel = "Card", account = "Main account",
        note = note, origin = origin, isConfirmed = isConfirmed, createdAt = createdAt,
    )

    private val usdIncome = Income(
        id = "i1", source = IncomeSource.FREELANCE, amountCents = 30_000, currency = Currency.USD,
        lkrAmountCents = 9_900_000, rateCentsPerUnit = 33_000, date = LocalDate.of(2026, 11, 1),
        note = "Logo project", createdAt = 1L,
    )

    /** The data lines of the CSV, without the header. */
    private fun dataLines(csv: String): List<String> = csv.split("\r\n").drop(1).filter { it.isNotEmpty() }

    @Test
    fun `empty data gives only the header`() {
        val csv = CsvBuilder.build(emptyList(), emptyList(), emptyList())
        assertEquals(CsvBuilder.HEADER.joinToString(",") + "\r\n", csv)
    }

    @Test
    fun `expense row has every column`() {
        val csv = CsvBuilder.build(emptyList(), listOf(expense(note = "lunch")), listOf(food))
        assertEquals(
            listOf("2026-11-05,Expense,Food,Discretionary,1250.50,1250.50,LKR,,Card,Main account,Added by hand,lunch"),
            dataLines(csv),
        )
    }

    @Test
    fun `foreign income keeps its saved rate and LKR amount`() {
        val csv = CsvBuilder.build(listOf(usdIncome), emptyList(), emptyList())
        assertEquals(
            listOf("2026-11-01,Income,Freelance,,99000.00,300.00,USD,330.00,,,,Logo project"),
            dataLines(csv),
        )
    }

    @Test
    fun `LKR income has no rate`() {
        val salary = usdIncome.copy(
            source = IncomeSource.SALARY, currency = Currency.LKR, amountCents = 15_000_000,
            lkrAmountCents = 15_000_000, rateCentsPerUnit = 100, note = null,
        )
        val csv = CsvBuilder.build(listOf(salary), emptyList(), emptyList())
        assertEquals(listOf("2026-11-01,Income,Salary,,150000.00,150000.00,LKR,,,,,"), dataLines(csv))
    }

    @Test
    fun `amounts convert exactly from cents`() {
        assertEquals("0.05", CsvBuilder.amount(5))
        assertEquals("1250.50", CsvBuilder.amount(125_050))
        assertEquals("490000.00", CsvBuilder.amount(49_000_000))
    }

    @Test
    fun `commas, quotes and line breaks are escaped`() {
        assertEquals("\"rice, curry\"", CsvBuilder.escape("rice, curry"))
        assertEquals("\"the \"\"good\"\" one\"", CsvBuilder.escape("the \"good\" one"))
        assertEquals("\"line one\nline two\"", CsvBuilder.escape("line one\nline two"))
        assertEquals("plain", CsvBuilder.escape("plain"))
    }

    @Test
    fun `text that would run as a spreadsheet formula is kept as plain text`() {
        assertEquals("'=HYPERLINK(\"x\")", CsvBuilder.text("=HYPERLINK(\"x\")"))
        assertEquals("'+94 phone top-up", CsvBuilder.text("+94 phone top-up"))
        assertEquals("'-refund", CsvBuilder.text("-refund"))
        assertEquals("'@bob", CsvBuilder.text("@bob"))
        assertEquals("Bus fare", CsvBuilder.text("Bus fare"))
    }

    @Test
    fun `rows are newest first across income and expenses`() {
        val older = expense(id = "old", date = LocalDate.of(2026, 10, 30))
        val newer = expense(id = "new", date = LocalDate.of(2026, 11, 5))
        val csv = CsvBuilder.build(listOf(usdIncome), listOf(older, newer), listOf(food))
        assertEquals(
            listOf("2026-11-05", "2026-11-01", "2026-10-30"),
            dataLines(csv).map { it.substringBefore(",") },
        )
    }

    @Test
    fun `origin labels include unrecorded spending and estimates`() {
        val expenses = listOf(
            expense(id = "a", origin = ExpenseOrigin.BALANCE_CHECK, createdAt = 3L),
            expense(id = "b", origin = ExpenseOrigin.RECURRING, isConfirmed = false, createdAt = 2L),
            expense(id = "c", origin = ExpenseOrigin.RECURRING, createdAt = 1L),
        )
        val origins = dataLines(CsvBuilder.build(emptyList(), expenses, listOf(food))).map { it.split(",")[10] }
        assertEquals(listOf("Unrecorded spending", "Recurring (estimate)", "Recurring"), origins)
    }

    @Test
    fun `missing category shows a fallback name`() {
        val csv = CsvBuilder.build(emptyList(), listOf(expense(categoryId = "gone")), listOf(food))
        assertEquals("Unknown category", dataLines(csv).single().split(",")[2])
    }
}
