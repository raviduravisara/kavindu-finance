package com.fintrack.kavindu.feature.goal.viewmodel

import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.Goal
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.util.Money
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.goal.domain.GoalCalculator
import com.fintrack.kavindu.feature.goal.domain.GoalImpactCalculator
import com.fintrack.kavindu.feature.goal.domain.GoalInput
import com.fintrack.kavindu.feature.goal.domain.GoalStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** When the goal will be reached at his current pace, compared with the deadline. */
enum class FinishTiming { BY_DEADLINE, AFTER_DEADLINE, NOT_YET_KNOWN }

/** One line in "What changed your date": a discretionary expense and the days it moved the goal. */
data class EffectRow(
    val id: String,
    val title: String?, // category name; null for a weekly check-in or an unknown category
    val isCheckIn: Boolean, // "unrecorded spending" found by the weekly balance check
    val amount: String, // e.g. "LKR 2,500"
    val date: String, // e.g. "23 Oct"
    val days: Int, // 0 = under a day
)

/** Everything the Goal screen shows, already worked out and formatted. */
data class GoalDetails(
    val name: String,
    val savedSoFar: String,
    val target: String,
    val percent: Int,
    val progress: Float, // 0..1 for the progress bar
    val deadline: String, // e.g. "14 Oct 2027"
    val cycleLabel: String, // e.g. "25 Oct – 24 Nov"
    val neededThisCycle: String,
    val savedThisCycle: String,
    val toGoThisCycle: String,
    val isCycleSavingDone: Boolean,
    val status: GoalStatus,
    val finishMonth: String?, // e.g. "October 2027"; null until there is a full cycle to judge from
    val finishTiming: FinishTiming,
    val monthsAfterDeadline: Int, // only meaningful for AFTER_DEADLINE
    val shortfall: String, // what is still missing, for the deadline-passed state
    val effects: List<EffectRow>,
)

/**
 * Turns the goal and the user's entries into the Goal screen's numbers (FR-37, FR-32).
 * Plain Kotlin with no Android code, so it is unit tested; the ViewModel only feeds it data from Room.
 *
 * "Saved" means income received minus money spent, counted from the day the goal was set.
 */
object GoalOverviewBuilder {

    const val MAX_EFFECTS = 5

    fun build(
        goal: Goal,
        incomes: List<Income>,
        expenses: List<Expense>,
        categories: List<Category>,
        cycle: PayCycle,
        today: LocalDate,
    ): GoalDetails {
        val startDay = cycle.start.dayOfMonth
        val sinceStartIncomes = incomes.filter { !it.date.isBefore(goal.startDate) }
        val sinceStartExpenses = expenses.filter { !it.date.isBefore(goal.startDate) }

        val savedSinceStart = sinceStartIncomes.sumOf { it.lkrAmountCents } - sinceStartExpenses.sumOf { it.amountCents }
        val savedThisCycle = sinceStartIncomes.filter { cycle.contains(it.date) }.sumOf { it.lkrAmountCents } -
            sinceStartExpenses.filter { cycle.contains(it.date) }.sumOf { it.amountCents }

        val progress = GoalCalculator().calculate(
            GoalInput(
                targetCents = goal.targetCents,
                startingSavingsCents = goal.startingSavingsCents,
                savedSinceStartCents = savedSinceStart,
                savedThisCycleCents = savedThisCycle,
                goalStartDate = goal.startDate,
                deadline = goal.deadline,
                today = today,
                cycleStartDay = startDay,
            )
        )

        val finish = progress.projectedFinish
        val monthsAfter = if (finish == null) 0
        else ChronoUnit.MONTHS.between(YearMonth.from(goal.deadline), YearMonth.from(finish)).toInt()
        val timing = when {
            finish == null -> FinishTiming.NOT_YET_KNOWN
            monthsAfter > 0 -> FinishTiming.AFTER_DEADLINE
            else -> FinishTiming.BY_DEADLINE
        }

        val namesById = categories.associate { it.id to it.name }
        val impact = GoalImpactCalculator()
        val effects = sinceStartExpenses
            .filter { cycle.contains(it.date) && it.categoryType == CategoryType.DISCRETIONARY }
            .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAt })
            .take(MAX_EFFECTS)
            .map { expense ->
                val isCheckIn = expense.origin == ExpenseOrigin.BALANCE_CHECK
                EffectRow(
                    id = expense.id,
                    title = if (isCheckIn) null else namesById[expense.categoryId],
                    isCheckIn = isCheckIn,
                    amount = Money.formatLkr(expense.amountCents),
                    date = expense.date.format(DAY_MONTH),
                    days = impact.daysDelayed(expense.amountCents, progress.neededThisCycleCents, today, startDay),
                )
            }

        return GoalDetails(
            name = goal.name,
            savedSoFar = Money.formatLkr(progress.savedSoFarCents),
            target = Money.formatLkr(goal.targetCents),
            percent = progress.percent,
            progress = progress.percent / 100f,
            deadline = goal.deadline.format(DAY_MONTH_YEAR),
            cycleLabel = "${cycle.start.format(DAY_MONTH)} – ${cycle.end.format(DAY_MONTH)}",
            neededThisCycle = Money.formatLkr(progress.neededThisCycleCents),
            savedThisCycle = Money.formatLkr(savedThisCycle),
            toGoThisCycle = Money.formatLkr(progress.toGoThisCycleCents),
            isCycleSavingDone = progress.toGoThisCycleCents == 0L,
            status = progress.status,
            finishMonth = finish?.format(MONTH_YEAR),
            finishTiming = timing,
            monthsAfterDeadline = monthsAfter.coerceAtLeast(0),
            shortfall = Money.formatLkr(progress.remainingCents),
            effects = effects,
        )
    }

    private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
    private val DAY_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
}
