package com.fintrack.kavindu.feature.dashboard.data

import com.fintrack.kavindu.core.data.repository.BalanceCheckRepository
import com.fintrack.kavindu.core.data.repository.CategoryRepository
import com.fintrack.kavindu.core.data.repository.CryptoRepository
import com.fintrack.kavindu.core.data.repository.ExpenseRepository
import com.fintrack.kavindu.core.data.repository.FreelanceRepository
import com.fintrack.kavindu.core.data.repository.GoalRepository
import com.fintrack.kavindu.core.data.repository.IncomeRepository
import com.fintrack.kavindu.core.data.repository.ProfileRepository
import com.fintrack.kavindu.core.data.repository.RecurringRepository
import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CryptoTransfer
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.Milestone
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.dashboard.viewmodel.DashboardInput
import com.fintrack.kavindu.feature.freelance.viewmodel.observeProjectsWithMilestones
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Brings together everything the dashboard shows into one live stream (M7, S4).
 *
 * Every source here is a Room `Flow` behind its own repository, so the dashboard updates the moment the Firestore
 * listener copies a change into Room: an expense added on another phone, a recurring entry, a freelance invoice
 * marked received. Nothing is read from Firestore directly and nothing waits for the network, so the screen is
 * never blank offline.
 *
 * The ViewModel never talks to nine repositories itself: it asks this class for one [DashboardInput] flow.
 */
class DashboardRepository @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val goalRepository: GoalRepository,
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val cryptoRepository: CryptoRepository,
    private val freelanceRepository: FreelanceRepository,
    private val balanceCheckRepository: BalanceCheckRepository,
    private val recurringRepository: RecurringRepository,
) {
    private class Records(
        val incomes: List<Income>,
        val expenses: List<Expense>,
        val categories: List<Category>,
        val transfers: List<CryptoTransfer>,
        val milestones: List<Milestone>,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDashboard(): Flow<DashboardInput> {
        val startDays = profileRepository.observeProfile()
            .map { (it?.cycleStartDay ?: PayCycle.DEFAULT_START_DAY).coerceIn(PayCycle.MIN_START_DAY, PayCycle.MAX_START_DAY) }
            .distinctUntilChanged()

        return combine(startDays, goalRepository.observeGoal()) { startDay, goal -> startDay to goal }
            .flatMapLatest { (startDay, goal) ->
                val today = LocalDate.now()
                val cycle = PayCycle.containing(today, startDay)
                // The goal counts what he saved since it was set, which can be before this cycle began.
                val since = goal?.startDate?.let { minOf(it, cycle.start) } ?: cycle.start

                val records = combine(
                    incomeRepository.observeIncomesSince(since),
                    expenseRepository.observeExpensesSince(since),
                    categoryRepository.observeCategories(),
                    cryptoRepository.observeTransfers(),
                    freelanceRepository.observeProjectsWithMilestones(),
                ) { incomes, expenses, categories, transfers, projects ->
                    Records(incomes, expenses, categories, transfers, projects.flatMap { it.second })
                }
                val extras = combine(
                    balanceCheckRepository.observeLatestCheck(),
                    recurringRepository.observeRecurringItems(),
                ) { latestCheck, items -> latestCheck?.date to items }

                combine(records, extras) { r, (lastCheckDate, items) ->
                    DashboardInput(
                        today = today,
                        cycle = cycle,
                        goal = goal,
                        incomes = r.incomes,
                        expenses = r.expenses,
                        categories = r.categories,
                        cryptoTransfers = r.transfers,
                        milestones = r.milestones,
                        lastCheckDate = lastCheckDate,
                        recurringItems = items,
                    )
                }
            }
    }
}
