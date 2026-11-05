package com.fintrack.kavindu.feature.reminder.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fintrack.kavindu.feature.reminder.ui.ReminderNotification
import com.fintrack.kavindu.feature.reminder.viewmodel.ReminderTime
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Runs once at the chosen time. It shows the reminder only when someone is signed in and nothing was added by hand
 * today, so it never nags. Then it plans tomorrow's run. The expenses come from Room, so this works offline too.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)
        val settings = entryPoint.reminderSettingsRepository().current()
        if (!settings.isEnabled) return Result.success() // switched off since this run was planned

        try {
            val isSignedIn = entryPoint.authRepository().currentUserId.first() != null
            if (isSignedIn) {
                val today = LocalDate.now()
                val todaysExpenses = entryPoint.expenseRepository().observeExpensesSince(today).first()
                if (ReminderTime.shouldRemind(todaysExpenses, today)) {
                    ReminderNotification.show(applicationContext)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A reminder that cannot be shown must never crash the app; tomorrow's is still planned below.
            Log.w(TAG, "Daily reminder could not be shown", e)
        }

        entryPoint.reminderScheduler().scheduleAfterRun(settings.time)
        return Result.success()
    }

    private companion object {
        const val TAG = "ReminderWorker"
    }
}
