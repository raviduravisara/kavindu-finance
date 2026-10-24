package com.fintrack.kavindu.feature.goal.viewmodel

import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.Goal
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.goal.domain.GoalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalOverviewBuilderTest {

    private fun lkr(rupees: Long) = rupees * 100
    private val today = LocalDate.of(2026, 11, 13)
    private val cycle = PayCycle.containing(today, 25) // 25 Oct – 24 Nov, 31 days

    private val goal = Goal(
        id = "g1",
        name = "MacBook Pro M4",
        targetCents = lkr(490_000),
        deadline = LocalDate.of(2027, 10, 24),
        startingSavingsCents = lkr(11_200),
        startDate = LocalDate.of(2026, 10, 25),
        createdAt = 1,
    )

    private val categories = listOf(
        Category("rent", "Rent", CategoryType.COMMITTED, 0, 1),
        Category("food", "Food delivery", CategoryType.DISCRETIONARY, 1, 1),
        Category("coffee", "Coffee & dining", CategoryType.DISCRETIONARY, 2, 1),
        Category("other", "Other", CategoryType.DISCRETIONARY, 3, 1),
    )

    private fun income(rupees: Long, date: LocalDate) = Income(
        id = "i$date", source = IncomeSource.SALARY, amountCents = lkr(rupees), currency = Currency.LKR,
        lkrAmountCents = lkr(rupees), rateCentsPerUnit = 100, date = date, createdAt = 1,
    )

    private fun expense(
        id: String,
        rupees: Long,
        date: LocalDate,
        categoryId: String,
        type: CategoryType = CategoryType.DISCRETIONARY,
        origin: ExpenseOrigin = ExpenseOrigin.MANUAL,
    ) = Expense(
        id = id, amountCents = lkr(rupees), date = date, categoryId = categoryId, categoryType = type,
        channel = "Card", account = "Main account", origin = origin, createdAt = date.toEpochDay(),
    )

    private val salary = income(133_333, LocalDate.of(2026, 10, 25))
    private val rent = expense("rent1", 34_000, LocalDate.of(2026, 10, 26), "rent", CategoryType.COMMITTED)
    private val uberEats = expense("e1", 2_500, LocalDate.of(2026, 11, 13), "food")
    private val coffee = expense("e2", 1_200, LocalDate.of(2026, 11, 12), "coffee")
    private val checkIn = expense("e3", 6_400, LocalDate.of(2026, 11, 10), "other", origin = ExpenseOrigin.BALANCE_CHECK)

    private fun build(incomes: List<Income> = listOf(salary), expenses: List<Expense> = listOf(rent, uberEats, coffee, checkIn), g: Goal = goal, t: LocalDate = today) =
        GoalOverviewBuilder.build(g, incomes, expenses, categories, PayCycle.containing(t, 25), t)

    @Test
    fun `first cycle numbers match the scenario`() {
        val d = build()
        assertEquals("LKR 39,900", d.neededThisCycle) // 478,800 ÷ 12 cycles
        assertEquals("LKR 89,233", d.savedThisCycle) // 133,333 − 44,100 spent
        assertEquals("LKR 100,433", d.savedSoFar) // 11,200 starting + 89,233
        assertEquals(20, d.percent)
        assertTrue(d.isCycleSavingDone)
        assertEquals(GoalStatus.ON_TRACK, d.status)
        assertEquals("25 Oct – 24 Nov", d.cycleLabel)
        assertEquals("24 Oct 2027", d.deadline)
    }

    @Test
    fun `no finish date until a full cycle has passed`() {
        val d = build()
        assertEquals(FinishTiming.NOT_YET_KNOWN, d.finishTiming)
        assertNull(d.finishMonth)
    }

    @Test
    fun `what changed your date lists discretionary spending, newest first, with days`() {
        val effects = build().effects
        assertEquals(listOf("e1", "e2", "e3"), effects.map { it.id }) // rent is committed, so it is not listed
        assertEquals("Food delivery", effects[0].title)
        assertEquals(2, effects[0].days) // 2,500 × 31 ÷ 39,900 ≈ 1.9
        assertEquals(1, effects[1].days) // 1,200 → about 1 day
        assertTrue(effects[2].isCheckIn)
        assertNull(effects[2].title)
        assertEquals(5, effects[2].days) // 6,400 → about 5 days
        assertEquals("LKR 2,500", effects[0].amount)
        assertEquals("13 Nov", effects[0].date)
    }

    @Test
    fun `shows at most five effects`() {
        val many = (1..8).map { expense("x$it", 500, LocalDate.of(2026, 11, it), "coffee") }
        assertEquals(GoalOverviewBuilder.MAX_EFFECTS, build(expenses = many).effects.size)
    }

    @Test
    fun `entries from before the goal started are not counted`() {
        val before = income(500_000, LocalDate.of(2026, 10, 24))
        val d = build(incomes = listOf(before), expenses = emptyList())
        assertEquals("LKR 11,200", d.savedSoFar)
    }

    @Test
    fun `saving too little in finished cycles shows a finish after the deadline`() {
        val early = goal.copy(startDate = LocalDate.of(2026, 8, 25), deadline = LocalDate.of(2027, 1, 24))
        val incomes = listOf(income(30_000, LocalDate.of(2026, 8, 25)), income(30_000, LocalDate.of(2026, 9, 25)))
        val d = build(incomes = incomes, expenses = emptyList(), g = early, t = LocalDate.of(2026, 10, 30))
        assertEquals(GoalStatus.A_LITTLE_BEHIND, d.status)
        assertEquals(FinishTiming.AFTER_DEADLINE, d.finishTiming)
        assertTrue(d.monthsAfterDeadline > 0)
    }

    @Test
    fun `goal reached`() {
        val d = build(incomes = listOf(income(600_000, LocalDate.of(2026, 10, 25))), expenses = emptyList())
        assertEquals(GoalStatus.REACHED, d.status)
        assertEquals(100, d.percent)
        assertEquals(1f, d.progress)
    }

    @Test
    fun `deadline passed shows the shortfall`() {
        val d = build(expenses = emptyList(), t = LocalDate.of(2027, 11, 1))
        assertEquals(GoalStatus.DEADLINE_PASSED, d.status)
        assertEquals("LKR 345,467", d.shortfall) // 490,000 − 11,200 − 133,333
    }

    @Test
    fun `no entries at all does not crash`() {
        val d = build(incomes = emptyList(), expenses = emptyList())
        assertEquals("LKR 11,200", d.savedSoFar)
        assertEquals("LKR 0", d.savedThisCycle)
        assertFalse(d.isCycleSavingDone)
        assertTrue(d.effects.isEmpty())
    }
}
