package com.fintrack.kavindu.feature.dashboard.viewmodel

import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.CryptoDirection
import com.fintrack.kavindu.core.model.CryptoTransfer
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.Goal
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import com.fintrack.kavindu.core.model.Milestone
import com.fintrack.kavindu.core.model.MilestoneStatus
import com.fintrack.kavindu.core.model.RecurringItem
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.goal.domain.GoalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DashboardBuilderTest {

    private fun lkr(rupees: Long) = rupees * 100

    // 13 Nov 2026 is in the 25 Oct - 24 Nov cycle, with 12 days left counting today.
    private val today = LocalDate.of(2026, 11, 13)
    private val cycle = PayCycle.containing(today, 25)
    private val cycleDay1 = LocalDate.of(2026, 10, 25)

    private fun income(id: String, rupees: Long, date: LocalDate = cycleDay1) = Income(
        id = id, source = IncomeSource.SALARY, amountCents = lkr(rupees), currency = Currency.LKR,
        lkrAmountCents = lkr(rupees), rateCentsPerUnit = 100, date = date, createdAt = 1L,
    )

    private fun expense(
        id: String,
        rupees: Long,
        type: CategoryType,
        categoryId: String = "cat-$id",
        date: LocalDate = LocalDate.of(2026, 11, 1),
        origin: ExpenseOrigin = ExpenseOrigin.MANUAL,
    ) = Expense(
        id = id, amountCents = lkr(rupees), date = date, categoryId = categoryId, categoryType = type,
        channel = "Card", account = "Main account", origin = origin, createdAt = 1L,
    )

    private fun category(id: String, name: String, type: CategoryType) = Category(id, name, type, 0, 1L)

    private val macbook = Goal(
        id = "g", name = "MacBook Pro M4", targetCents = lkr(490_000), deadline = LocalDate.of(2027, 10, 14),
        startingSavingsCents = lkr(11_200), startDate = cycleDay1, createdAt = 1L,
    )

    private fun input(
        goal: Goal? = null,
        incomes: List<Income> = emptyList(),
        expenses: List<Expense> = emptyList(),
        categories: List<Category> = emptyList(),
        transfers: List<CryptoTransfer> = emptyList(),
        milestones: List<Milestone> = emptyList(),
        lastCheck: LocalDate? = null,
        items: List<RecurringItem> = emptyList(),
    ) = DashboardInput(today, cycle, goal, incomes, expenses, categories, transfers, milestones, lastCheck, items)

    // ---- the wireframe's numbers ----

    @Test
    fun `the wireframe example - 25,433 safe to spend, 2,119 a day for 12 days`() {
        val d = DashboardBuilder.build(
            input(
                goal = macbook,
                incomes = listOf(income("salary", 133_333)),
                expenses = listOf(
                    expense("rent", 34_000, CategoryType.COMMITTED),
                    expense("gym", 14_000, CategoryType.COMMITTED),
                    expense("dining", 20_000, CategoryType.DISCRETIONARY),
                ),
            )
        )
        assertEquals("LKR 25,433", d.safeToSpend)
        assertFalse(d.isOver)
        assertEquals("LKR 2,119", d.perDay)
        assertEquals(12, d.daysLeft)
        assertEquals("LKR 133,333", d.received)
        assertEquals("LKR 68,000", d.spent)
        assertEquals("25 Oct – 24 Nov", d.cycleRange)
        assertEquals("25 Oct", d.cycleStart)
    }

    @Test
    fun `committed and discretionary split gives 71 and 29 percent`() {
        val d = DashboardBuilder.build(
            input(
                expenses = listOf(
                    expense("rent", 48_000, CategoryType.COMMITTED),
                    expense("dining", 20_000, CategoryType.DISCRETIONARY),
                )
            )
        )
        assertEquals("LKR 48,000", d.committed)
        assertEquals(71, d.committedPercent)
        assertEquals("LKR 20,000", d.discretionary)
        assertEquals(29, d.discretionaryPercent)
        assertEquals(48_000f / 68_000f, d.committedFraction, 0.0001f)
    }

    @Test
    fun `the goal card uses the same numbers as the goal tab`() {
        val d = DashboardBuilder.build(
            input(
                goal = macbook,
                incomes = listOf(income("salary", 133_333)),
                expenses = listOf(expense("e", 68_000, CategoryType.DISCRETIONARY)),
            )
        )
        val goal = d.goal
        assertNotNull(goal)
        assertEquals("MacBook Pro M4", goal!!.name)
        // saved so far = starting 11,200 + (133,333 - 68,000)
        assertEquals("LKR 76,533 of LKR 490,000", goal.savedOfTarget)
        assertEquals(15, goal.percent)
        assertEquals(GoalStatus.ON_TRACK, goal.status)
        assertTrue(goal.isCycleSavingDone) // 65,333 saved this cycle is above the 39,900 needed
    }

    @Test
    fun `before any saving this cycle the goal card says how much is left to save`() {
        val d = DashboardBuilder.build(input(goal = macbook))
        assertEquals("LKR 39,900", d.goal!!.toGoThisCycle)
        assertFalse(d.goal.isCycleSavingDone)
    }

    // ---- calm overspending ----

    @Test
    fun `overspending is a plain negative number with no per-day amount`() {
        val d = DashboardBuilder.build(
            input(
                incomes = listOf(income("salary", 100_000)),
                expenses = listOf(
                    expense("rent", 60_000, CategoryType.COMMITTED),
                    expense("fun", 60_000, CategoryType.DISCRETIONARY),
                ),
            )
        )
        assertTrue(d.isOver)
        assertEquals("-LKR 20,000", d.safeToSpend)
        assertEquals("LKR 20,000", d.overBy)
        assertEquals("LKR 0", d.perDay)
    }

    @Test
    fun `a cycle with no income yet gives a calm negative number, never a crash`() {
        val d = DashboardBuilder.build(input(goal = macbook))
        assertTrue(d.isOver)
        assertEquals("LKR 0", d.received)
        assertEquals("-LKR 39,900", d.safeToSpend)
    }

    // ---- empty account ----

    @Test
    fun `a brand-new account shows zeros, no goal, no chart and the first-expense prompt state`() {
        val d = DashboardBuilder.build(input())
        assertFalse(d.hasEntries)
        assertEquals("LKR 0", d.safeToSpend)
        assertFalse(d.isOver)
        assertEquals("LKR 0", d.received)
        assertEquals("LKR 0", d.spent)
        assertEquals(0, d.committedPercent)
        assertEquals(0, d.discretionaryPercent)
        assertEquals(0f, d.committedFraction, 0f)
        assertNull(d.goal)
        assertTrue(d.categorySlices.isEmpty())
        assertNull(d.pendingNotCounted)
        assertEquals(0, d.overdueInvoices)
        assertTrue(d.showCheckIn) // no check-in yet
    }

    // ---- received, pending, crypto ----

    @Test
    fun `a pending invoice is shown but never counted as received`() {
        val pending = Milestone(
            id = "m", projectId = "p", title = "Design", amountCents = lkr(85_000), currency = Currency.LKR,
            dueDate = LocalDate.of(2026, 12, 1), status = MilestoneStatus.PENDING, createdAt = 1L,
        )
        val d = DashboardBuilder.build(input(incomes = listOf(income("salary", 133_333)), milestones = listOf(pending)))
        assertEquals("LKR 133,333", d.received)
        assertEquals("LKR 85,000", d.pendingNotCounted)
    }

    @Test
    fun `overdue invoices are counted, received and cancelled ones are not`() {
        fun milestone(id: String, status: MilestoneStatus, due: LocalDate) = Milestone(
            id = id, projectId = "p", title = id, amountCents = lkr(1_000), currency = Currency.LKR,
            dueDate = due, status = status, incomeId = if (status == MilestoneStatus.RECEIVED) "i" else null, createdAt = 1L,
        )
        val d = DashboardBuilder.build(
            input(
                milestones = listOf(
                    milestone("late1", MilestoneStatus.PENDING, today.minusDays(3)),
                    milestone("late2", MilestoneStatus.PENDING, today.minusDays(40)),
                    milestone("future", MilestoneStatus.PENDING, today.plusDays(5)),
                    milestone("paid", MilestoneStatus.RECEIVED, today.minusDays(10)),
                    milestone("cancelled", MilestoneStatus.CANCELLED, today.minusDays(10)),
                )
            )
        )
        assertEquals(2, d.overdueInvoices)
    }

    @Test
    fun `crypto profit that became real this cycle counts as received`() {
        val transfers = listOf(
            CryptoTransfer("t1", CryptoDirection.INTO_BINANCE, lkr(100_000), LocalDate.of(2026, 10, 10), null, 1L),
            CryptoTransfer("t2", CryptoDirection.OUT_OF_BINANCE, lkr(140_000), LocalDate.of(2026, 11, 5), null, 2L),
        )
        val d = DashboardBuilder.build(input(incomes = listOf(income("salary", 100_000)), transfers = transfers))
        assertEquals("LKR 140,000", d.received) // 100,000 salary + 40,000 realised profit
    }

    @Test
    fun `a crypto loss never reduces received`() {
        val transfers = listOf(
            CryptoTransfer("t1", CryptoDirection.INTO_BINANCE, lkr(100_000), LocalDate.of(2026, 10, 10), null, 1L),
            CryptoTransfer("t2", CryptoDirection.OUT_OF_BINANCE, lkr(60_000), LocalDate.of(2026, 11, 5), null, 2L),
        )
        val d = DashboardBuilder.build(input(incomes = listOf(income("salary", 100_000)), transfers = transfers))
        assertEquals("LKR 100,000", d.received)
    }

    @Test
    fun `entries from other cycles are not in this cycle's totals`() {
        val d = DashboardBuilder.build(
            input(
                incomes = listOf(income("old", 120_000, LocalDate.of(2026, 9, 25)), income("now", 133_333)),
                expenses = listOf(expense("old", 9_999, CategoryType.COMMITTED, date = LocalDate.of(2026, 10, 24))),
            )
        )
        assertEquals("LKR 133,333", d.received)
        assertEquals("LKR 0", d.spent)
    }

    // ---- weekly check-in prompt ----

    @Test
    fun `the weekly check-in is suggested after 7 days or when there has been none`() {
        assertTrue(DashboardBuilder.build(input(lastCheck = null)).showCheckIn)
        assertTrue(DashboardBuilder.build(input(lastCheck = today.minusDays(7))).showCheckIn)
        assertTrue(DashboardBuilder.build(input(lastCheck = today.minusDays(30))).showCheckIn)
        assertFalse(DashboardBuilder.build(input(lastCheck = today.minusDays(6))).showCheckIn)
        assertFalse(DashboardBuilder.build(input(lastCheck = today)).showCheckIn)
    }

    // ---- subscriptions ----

    @Test
    fun `subscriptions due for a review are counted for the gentle nudge`() {
        val zone = ZoneId.systemDefault()
        fun sub(id: String, createdDaysAgo: Long) = RecurringItem(
            id = id, name = id, amountCents = lkr(1_000), categoryId = "c", dayOfMonth = 5, isSubscription = true,
            createdAt = today.minusDays(createdDaysAgo).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
        val d = DashboardBuilder.build(input(items = listOf(sub("old1", 200), sub("old2", 100), sub("new", 2))))
        assertEquals(2, d.subscriptionsToReview)
    }

    // ---- category chart ----

    @Test
    fun `categories are listed biggest first with their share of the spending`() {
        val categories = listOf(
            category("coffee", "Coffee & dining", CategoryType.DISCRETIONARY),
            category("rent", "Rent", CategoryType.COMMITTED),
        )
        val d = DashboardBuilder.build(
            input(
                categories = categories,
                expenses = listOf(
                    expense("a", 24_000, CategoryType.DISCRETIONARY, "coffee"),
                    expense("b", 12_000, CategoryType.DISCRETIONARY, "coffee"),
                    expense("c", 36_000, CategoryType.COMMITTED, "rent"),
                    expense("d", 8_000, CategoryType.COMMITTED, "rent"),
                ),
            )
        )
        val slices = d.categorySlices
        assertEquals(listOf("Rent", "Coffee & dining"), slices.map { it.name })
        assertEquals("LKR 44,000", slices[0].amount)
        assertEquals(55, slices[0].percent)
        assertEquals(1f, slices[0].fraction, 0f)
        assertEquals(CategoryType.COMMITTED, slices[0].type)
        assertEquals(45, slices[1].percent)
        assertEquals(36_000f / 44_000f, slices[1].fraction, 0.0001f)
    }

    @Test
    fun `unrecorded spending from the weekly check-in gets its own bar`() {
        val categories = listOf(category("other", "Other", CategoryType.DISCRETIONARY))
        val d = DashboardBuilder.build(
            input(
                categories = categories,
                expenses = listOf(
                    expense("manual", 1_000, CategoryType.DISCRETIONARY, "other"),
                    expense("check", 6_400, CategoryType.DISCRETIONARY, "other", origin = ExpenseOrigin.BALANCE_CHECK),
                ),
            )
        )
        assertEquals(2, d.categorySlices.size)
        assertEquals(SliceKind.UNRECORDED, d.categorySlices[0].kind)
        assertNull(d.categorySlices[0].name)
        assertEquals("LKR 6,400", d.categorySlices[0].amount)
        assertEquals(SliceKind.CATEGORY, d.categorySlices[1].kind)
        assertEquals("Other", d.categorySlices[1].name)
    }

    @Test
    fun `more than six groups fold the smallest into one last bar`() {
        val categories = (1..8).map { category("c$it", "Cat $it", CategoryType.DISCRETIONARY) }
        val expenses = (1..8).map { expense("e$it", it * 1_000L, CategoryType.DISCRETIONARY, "c$it") }
        val d = DashboardBuilder.build(input(categories = categories, expenses = expenses))
        assertEquals(6, d.categorySlices.size)
        val last = d.categorySlices.last()
        assertEquals(SliceKind.REST, last.kind)
        assertNull(last.type)
        // Five bars stay on their own (8k, 7k, 6k, 5k, 4k); the three smallest fold: 3,000 + 2,000 + 1,000
        assertEquals("LKR 6,000", last.amount)
        assertEquals("Cat 8", d.categorySlices.first().name)
    }

    @Test
    fun `an expense whose category was removed still shows, with no name`() {
        val d = DashboardBuilder.build(
            input(expenses = listOf(expense("x", 5_000, CategoryType.COMMITTED, "gone")))
        )
        val slice = d.categorySlices.single()
        assertEquals(SliceKind.CATEGORY, slice.kind)
        assertNull(slice.name)
        assertEquals(CategoryType.COMMITTED, slice.type)
    }
}
