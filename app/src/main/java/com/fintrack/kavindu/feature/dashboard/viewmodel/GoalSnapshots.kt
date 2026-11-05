package com.fintrack.kavindu.feature.dashboard.viewmodel

import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.Goal
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.goal.domain.GoalCalculator
import com.fintrack.kavindu.feature.goal.domain.GoalInput
import com.fintrack.kavindu.feature.goal.domain.GoalProgress
import java.time.LocalDate

/**
 * The goal numbers the dashboard and quick-add need, worked out in exactly the way the Goal tab does it
 * (same "saved" meaning, same [GoalCalculator]), so "10%" and "LKR 14,900 left to save this cycle" on the home
 * screen always match the Goal screen.
 *
 * "Saved" is income received minus money spent, counted from the day the goal was set.
 * Plain Kotlin with no Android code.
 */
object GoalSnapshots {

    fun progress(
        goal: Goal,
        incomes: List<Income>,
        expenses: List<Expense>,
        cycle: PayCycle,
        today: LocalDate,
    ): GoalProgress {
        val sinceStartIncomes = incomes.filter { !it.date.isBefore(goal.startDate) }
        val sinceStartExpenses = expenses.filter { !it.date.isBefore(goal.startDate) }

        val savedSinceStart = sinceStartIncomes.sumOf { it.lkrAmountCents } - sinceStartExpenses.sumOf { it.amountCents }
        val savedThisCycle = sinceStartIncomes.filter { cycle.contains(it.date) }.sumOf { it.lkrAmountCents } -
            sinceStartExpenses.filter { cycle.contains(it.date) }.sumOf { it.amountCents }

        return GoalCalculator().calculate(
            GoalInput(
                targetCents = goal.targetCents,
                startingSavingsCents = goal.startingSavingsCents,
                savedSinceStartCents = savedSinceStart,
                savedThisCycleCents = savedThisCycle,
                goalStartDate = goal.startDate,
                deadline = goal.deadline,
                today = today,
                cycleStartDay = cycle.start.dayOfMonth,
            )
        )
    }
}
