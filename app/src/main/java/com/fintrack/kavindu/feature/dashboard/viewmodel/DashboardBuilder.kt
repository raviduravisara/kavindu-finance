package com.fintrack.kavindu.feature.dashboard.viewmodel

import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.CryptoTransfer
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.Goal
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.Milestone
import com.fintrack.kavindu.core.model.RecurringItem
import com.fintrack.kavindu.core.util.Money
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.crypto.viewmodel.CryptoCalculator
import com.fintrack.kavindu.feature.freelance.viewmodel.FreelanceRules
import com.fintrack.kavindu.feature.goal.domain.GoalStatus
import com.fintrack.kavindu.feature.goal.domain.SafeToSpendCalculator
import com.fintrack.kavindu.feature.recurring.viewmodel.RecurringRules
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Everything the dashboard needs from Room, for the pay cycle that contains [today]. */
data class DashboardInput(
    val today: LocalDate,
    val cycle: PayCycle,
    val goal: Goal?,
    val incomes: List<Income>, // from the start of the cycle or of the goal, whichever is earlier
    val expenses: List<Expense>, // same range
    val categories: List<Category>,
    val cryptoTransfers: List<CryptoTransfer>,
    val milestones: List<Milestone>,
    val lastCheckDate: LocalDate?, // the latest weekly check-in, null if there has not been one
    val recurringItems: List<RecurringItem>,
)

/** One bar in the "By category" chart (S7). */
enum class SliceKind { CATEGORY, UNRECORDED, REST }

data class CategorySlice(
    val kind: SliceKind,
    val name: String?, // null for UNRECORDED and REST: the screen shows its own wording
    val type: CategoryType?, // null for REST; decides the bar colour
    val amount: String, // e.g. "LKR 24,000"
    val percent: Int, // share of everything spent this cycle
    val fraction: Float, // bar length, relative to the biggest slice (0..1)
)

/** The goal card. Words like "On track" come from the screen's string resources, from [status]. */
data class GoalSummary(
    val name: String,
    val percent: Int,
    val progress: Float, // 0..1 for the progress bar
    val savedOfTarget: String, // e.g. "LKR 51,100 of LKR 490,000"
    val status: GoalStatus,
    val toGoThisCycle: String, // e.g. "LKR 14,900"
    val isCycleSavingDone: Boolean,
)

/** Everything the dashboard shows, already worked out and formatted. */
data class DashboardDetails(
    val cycleRange: String, // e.g. "25 Oct – 24 Nov"
    val daysLeft: Int,
    val cycleStart: String, // e.g. "25 Oct"
    val safeToSpend: String, // e.g. "LKR 25,433", or "-LKR 4,000" when over
    val isOver: Boolean,
    val overBy: String, // e.g. "LKR 4,000", only meaningful when [isOver]
    val perDay: String, // e.g. "LKR 2,119", whole rupees
    val received: String,
    val pendingNotCounted: String?, // e.g. "LKR 85,000"; null when nothing is pending
    val spent: String,
    val committed: String,
    val committedPercent: Int,
    val committedFraction: Float,
    val discretionary: String,
    val discretionaryPercent: Int,
    val discretionaryFraction: Float,
    val categorySlices: List<CategorySlice>,
    val goal: GoalSummary?, // null when no goal is set yet
    val showCheckIn: Boolean,
    val overdueInvoices: Int,
    val subscriptionsToReview: Int,
    val hasEntries: Boolean, // false on a brand-new account: the screen shows its friendly first-expense prompt
)

/**
 * Turns Room's data into the dashboard (M7, S4, S7). Plain Kotlin with no Android code, so it
 * is unit tested; the ViewModel only feeds it data and publishes the result.
 *
 * Every calculation here follows the team guide:
 * - Received = income that has arrived this cycle + crypto profit that became real this cycle. A pending freelance
 *   invoice is shown next to it but never added (FR-14).
 * - Safe to spend = received - committed - this cycle's goal saving - discretionary spent (via [SafeToSpendCalculator]).
 *   Overspending is a plain negative number, never an alarm (NFR-09).
 * - Goal numbers come from [GoalSnapshots], the same maths as the Goal tab.
 */
object DashboardBuilder {

    private const val MAX_SLICES = 6 // bars shown; anything beyond folds into one "everything else" bar
    private const val CHECK_IN_AFTER_DAYS = 7L

    fun build(input: DashboardInput): DashboardDetails {
        val cycle = input.cycle
        val today = input.today
        val cycleIncomes = input.incomes.filter { cycle.contains(it.date) }
        val cycleExpenses = input.expenses.filter { cycle.contains(it.date) }

        val cryptoProfit = CryptoCalculator.realisedBetween(
            CryptoCalculator.position(input.cryptoTransfers), cycle.start, cycle.end,
        )
        val received = cycleIncomes.sumOf { it.lkrAmountCents } + cryptoProfit
        val committed = cycleExpenses.filter { it.categoryType == CategoryType.COMMITTED }.sumOf { it.amountCents }
        val discretionary = cycleExpenses.filter { it.categoryType == CategoryType.DISCRETIONARY }.sumOf { it.amountCents }
        val spent = committed + discretionary

        val goalProgress = input.goal?.let { GoalSnapshots.progress(it, input.incomes, input.expenses, cycle, today) }
        val safe = SafeToSpendCalculator().calculate(
            receivedThisCycleCents = received,
            committedThisCycleCents = committed,
            goalNeededThisCycleCents = goalProgress?.neededThisCycleCents ?: 0L,
            discretionarySpentCents = discretionary,
            today = today,
            cycleStartDay = cycle.start.dayOfMonth,
        )

        val committedPercent = percentOf(committed, spent)
        val goalSummary = if (input.goal != null && goalProgress != null) {
            GoalSummary(
                name = input.goal.name,
                percent = goalProgress.percent,
                progress = goalProgress.percent / 100f,
                savedOfTarget = "${Money.formatLkr(goalProgress.savedSoFarCents)} of ${Money.formatLkr(input.goal.targetCents)}",
                status = goalProgress.status,
                toGoThisCycle = Money.formatLkr(goalProgress.toGoThisCycleCents),
                isCycleSavingDone = goalProgress.toGoThisCycleCents == 0L,
            )
        } else {
            null
        }

        return DashboardDetails(
            cycleRange = "${cycle.start.format(DAY_MONTH)} – ${cycle.end.format(DAY_MONTH)}",
            daysLeft = cycle.daysRemaining(today),
            cycleStart = cycle.start.format(DAY_MONTH),
            safeToSpend = Money.formatLkr(safe.amountCents),
            isOver = safe.amountCents < 0,
            overBy = Money.formatLkr(-safe.amountCents.coerceAtMost(0L)),
            perDay = Money.formatLkr(safe.perDayCents / 100 * 100),
            received = Money.formatLkr(received),
            pendingNotCounted = FreelanceRules.formatPending(FreelanceRules.pendingByCurrency(input.milestones)),
            spent = Money.formatLkr(spent),
            committed = Money.formatLkr(committed),
            committedPercent = committedPercent,
            committedFraction = if (spent > 0) committed.toFloat() / spent else 0f,
            discretionary = Money.formatLkr(discretionary),
            discretionaryPercent = if (spent > 0) 100 - committedPercent else 0,
            discretionaryFraction = if (spent > 0) discretionary.toFloat() / spent else 0f,
            categorySlices = categorySlices(cycleExpenses, input.categories, spent),
            goal = goalSummary,
            showCheckIn = input.lastCheckDate == null || ChronoUnit.DAYS.between(input.lastCheckDate, today) >= CHECK_IN_AFTER_DAYS,
            overdueInvoices = input.milestones.count { FreelanceRules.isOverdue(it, today) },
            subscriptionsToReview = input.recurringItems.count { RecurringRules.needsReview(it, today) },
            hasEntries = cycleIncomes.isNotEmpty() || cycleExpenses.isNotEmpty(),
        )
    }

    /**
     * Where the money went this cycle, by category, biggest first (S7). The "unrecorded spending" that the weekly
     * check-in creates gets its own bar instead of hiding inside "Other". More than [MAX_SLICES] groups fold the
     * smallest ones into one last bar so the chart stays readable.
     */
    fun categorySlices(expenses: List<Expense>, categories: List<Category>, spentCents: Long): List<CategorySlice> {
        if (spentCents <= 0) return emptyList()
        val categoriesById = categories.associateBy { it.id }
        val groups = expenses
            .groupBy { if (it.origin == ExpenseOrigin.BALANCE_CHECK) UNRECORDED_KEY else it.categoryId }
            .map { (key, items) ->
                val total = items.sumOf { it.amountCents }
                if (key == UNRECORDED_KEY) {
                    Group(SliceKind.UNRECORDED, null, CategoryType.DISCRETIONARY, total)
                } else {
                    val category = categoriesById[key]
                    Group(SliceKind.CATEGORY, category?.name, category?.type ?: items.first().categoryType, total)
                }
            }
            .sortedByDescending { it.cents }

        val shown = if (groups.size <= MAX_SLICES) {
            groups
        } else {
            groups.take(MAX_SLICES - 1) + Group(SliceKind.REST, null, null, groups.drop(MAX_SLICES - 1).sumOf { it.cents })
        }
        val biggest = shown.maxOf { it.cents }.coerceAtLeast(1)
        return shown.map { group ->
            CategorySlice(
                kind = group.kind,
                name = group.name,
                type = group.type,
                amount = Money.formatLkr(group.cents),
                percent = percentOf(group.cents, spentCents),
                fraction = group.cents.toFloat() / biggest,
            )
        }
    }

    // Whole percent, rounded to the nearest. 0 when there is nothing to divide by.
    private fun percentOf(part: Long, whole: Long): Int =
        if (whole <= 0) 0 else ((part * 100 + whole / 2) / whole).toInt()

    private class Group(val kind: SliceKind, val name: String?, val type: CategoryType?, val cents: Long)

    private const val UNRECORDED_KEY = "\u0000unrecorded" // cannot clash with a real category id
    private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
}
