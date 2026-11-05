package com.fintrack.kavindu.feature.recurring.data

import com.fintrack.kavindu.core.data.repository.CategoryRepository
import com.fintrack.kavindu.core.data.repository.ExpenseRepository
import com.fintrack.kavindu.core.data.repository.ProfileRepository
import com.fintrack.kavindu.core.data.repository.RecurringRepository
import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.RecurringItem
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.recurring.viewmodel.RecurringSchedule
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Adds Kavindu's recurring expenses for him, so rent, gym and subscriptions never depend on him remembering
 * (M5). The dashboard starts it when the app opens, and it keeps running while the dashboard is
 * alive, so an item created a moment ago is picked up too.
 *
 * What is due is decided by [RecurringSchedule] (plain Kotlin, unit tested). This class only reads from the
 * repositories and saves through [ExpenseRepository], like every other feature: Firestore first, then the
 * listener copies it into Room.
 *
 * Safe to run on two phones at once: an entry has a fixed id, so both phones write the same document with the
 * same data and it exists only once (decision 4.7).
 */
class RecurringEntryGenerator @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    private val profileRepository: ProfileRepository,
) {
    // Ids this run has already tried to save. If the server ever refuses one, Firestore undoes it and the list of
    // expenses changes back; without this memory the generator would try the same entry again and again.
    private val attempted = mutableSetOf<String>()

    /** Never returns normally: it keeps collecting until the caller's scope (the ViewModel) is cancelled. */
    @OptIn(FlowPreview::class)
    suspend fun run() {
        val startDays = profileRepository.observeProfile()
            .map { (it?.cycleStartDay ?: PayCycle.DEFAULT_START_DAY).coerceIn(PayCycle.MIN_START_DAY, PayCycle.MAX_START_DAY) }
            .distinctUntilChanged()

        // The expenses of the last few months are enough: catching up never goes back further than one cycle.
        val since = LocalDate.now().minusMonths(CHECK_BACK_MONTHS)
        combine(
            recurringRepository.observeRecurringItems(),
            categoryRepository.observeCategories(),
            expenseRepository.observeExpensesSince(since),
            startDays,
        ) { items, categories, expenses, startDay -> Snapshot(items, categories, expenses.map { it.id }.toSet(), startDay) }
            // On a phone that has just logged in, Room fills up in pieces. Waiting for things to settle means the
            // existing entries are known before anything is added.
            .debounce(SETTLE_MILLIS)
            .collect { snapshot ->
                val due = RecurringSchedule.dueEntries(
                    items = snapshot.items,
                    categories = snapshot.categories,
                    existingIds = snapshot.existingExpenseIds,
                    today = LocalDate.now(),
                    cycleStartDay = snapshot.startDay,
                )
                due.filter { attempted.add(it.id) }.forEach { expenseRepository.saveExpense(it) }
            }
    }

    private class Snapshot(
        val items: List<RecurringItem>,
        val categories: List<Category>,
        val existingExpenseIds: Set<String>,
        val startDay: Int,
    )

    private companion object {
        const val SETTLE_MILLIS = 1_000L
        const val CHECK_BACK_MONTHS = 3L
    }
}
