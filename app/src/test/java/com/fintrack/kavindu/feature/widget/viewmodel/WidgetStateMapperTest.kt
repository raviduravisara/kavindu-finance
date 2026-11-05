package com.fintrack.kavindu.feature.widget.viewmodel

import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.Goal
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.dashboard.viewmodel.DashboardBuilder
import com.fintrack.kavindu.feature.dashboard.viewmodel.DashboardDetails
import com.fintrack.kavindu.feature.dashboard.viewmodel.DashboardInput
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// The widget shows the same numbers as the dashboard: these tests run real DashboardBuilder results through the mapper.
class WidgetStateMapperTest {

    private fun lkr(rupees: Long) = rupees * 100

    // 13 Nov 2026 is in the 25 Oct - 24 Nov cycle, with 12 days left counting today.
    private val today = LocalDate.of(2026, 11, 13)
    private val cycleDay1 = LocalDate.of(2026, 10, 25)

    private fun income(rupees: Long) = Income(
        id = "salary", source = IncomeSource.SALARY, amountCents = lkr(rupees), currency = Currency.LKR,
        lkrAmountCents = lkr(rupees), rateCentsPerUnit = 100, date = cycleDay1, createdAt = 1L,
    )

    private fun expense(id: String, rupees: Long, type: CategoryType) = Expense(
        id = id, amountCents = lkr(rupees), date = LocalDate.of(2026, 11, 1), categoryId = "cat-$id",
        categoryType = type, channel = "Card", account = "Main account", createdAt = 1L,
    )

    private val macbook = Goal(
        id = "g", name = "MacBook Pro M4", targetCents = lkr(490_000), deadline = LocalDate.of(2027, 10, 14),
        startingSavingsCents = lkr(11_200), startDate = cycleDay1, createdAt = 1L,
    )

    private fun details(
        on: LocalDate = today,
        goal: Goal? = null,
        incomes: List<Income> = emptyList(),
        expenses: List<Expense> = emptyList(),
    ): DashboardDetails = DashboardBuilder.build(
        DashboardInput(
            on, PayCycle.containing(on, 25), goal, incomes, expenses,
            emptyList(), emptyList(), emptyList(), null, emptyList(),
        )
    )

    @Test
    fun `signed out shows the open-the-app message, even if old details exist`() {
        val d = details(incomes = listOf(income(100_000)))
        assertEquals(WidgetState.SignedOut, WidgetStateMapper.map(userId = null, details = d))
    }

    @Test
    fun `signed in with nothing recorded this cycle shows the start message`() {
        assertEquals(WidgetState.NoData, WidgetStateMapper.map("uid", details()))
        assertEquals(WidgetState.NoData, WidgetStateMapper.map("uid", details = null))
    }

    @Test
    fun `a normal cycle shows the same per-day number as the dashboard hero card`() {
        val d = details(
            goal = macbook,
            incomes = listOf(income(133_333)),
            expenses = listOf(
                expense("rent", 34_000, CategoryType.COMMITTED),
                expense("gym", 14_000, CategoryType.COMMITTED),
                expense("dining", 20_000, CategoryType.DISCRETIONARY),
            ),
        )
        assertEquals(WidgetState.Ready(perDay = "LKR 2,119"), WidgetStateMapper.map("uid", d))
        assertEquals(WidgetState.Ready(d.perDay), WidgetStateMapper.map("uid", d))
    }

    @Test
    fun `overspending shows the plain negative amount for the cycle`() {
        val d = details(
            incomes = listOf(income(100_000)),
            expenses = listOf(
                expense("rent", 60_000, CategoryType.COMMITTED),
                expense("fun", 60_000, CategoryType.DISCRETIONARY),
            ),
        )
        assertEquals(WidgetState.Over(amount = "-LKR 20,000"), WidgetStateMapper.map("uid", d))
    }

    @Test
    fun `on the last day of the cycle the whole remaining amount is today's number`() {
        val lastDay = LocalDate.of(2026, 11, 24)
        val d = details(
            on = lastDay,
            incomes = listOf(income(100_000)),
            expenses = listOf(expense("rent", 40_000, CategoryType.COMMITTED)),
        )
        assertEquals(WidgetState.Ready(perDay = "LKR 60,000"), WidgetStateMapper.map("uid", d))
    }
}
