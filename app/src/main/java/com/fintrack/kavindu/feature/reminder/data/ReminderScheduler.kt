package com.fintrack.kavindu.feature.reminder.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fintrack.kavindu.feature.reminder.viewmodel.ReminderTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plans the daily reminder with WorkManager. There is only ever one job, under one unique name, so the reminder can
 * never be scheduled twice. Each job runs once at the chosen time and then plans the next day's job, which keeps the
 * time exact (a 24-hour repeating job slowly drifts). WorkManager keeps the job after the phone restarts.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: ReminderSettingsRepository,
) {
    /** Saves the new setting and replaces any planned job with one for the new time, or cancels it when off. */
    fun update(settings: ReminderSettings) {
        settingsRepository.save(settings)
        if (settings.isEnabled) {
            enqueue(settings.time, ExistingWorkPolicy.REPLACE)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    fun turnOff() = update(settingsRepository.current().copy(isEnabled = false))

    /**
     * Called when the app starts. If the reminder is on but no job is planned (for example after the app was
     * force-stopped or restored on a new phone), plan one. A job that is already planned is kept as it is.
     */
    fun restore() {
        val settings = settingsRepository.current()
        if (settings.isEnabled) enqueue(settings.time, ExistingWorkPolicy.KEEP)
    }

    /**
     * Called by the reminder job itself when it finishes. The next job waits for this one to end (APPEND), and is at
     * least an hour away, so the same evening never gets two reminders.
     */
    fun scheduleAfterRun(time: LocalTime) =
        enqueue(time, ExistingWorkPolicy.APPEND_OR_REPLACE, minimumGap = Duration.ofHours(1))

    private fun enqueue(time: LocalTime, policy: ExistingWorkPolicy, minimumGap: Duration = Duration.ZERO) {
        val delay = ReminderTime.delayUntilNext(ZonedDateTime.now(), time, minimumGap)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private companion object {
        const val WORK_NAME = "daily_reminder"
    }
}
