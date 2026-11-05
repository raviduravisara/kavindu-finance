package com.fintrack.kavindu.feature.reminder.data

import com.fintrack.kavindu.core.data.repository.AuthRepository
import com.fintrack.kavindu.core.data.repository.ExpenseRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// WorkManager creates the reminder job itself, so Hilt cannot inject it directly.
// The job asks this entry point for what it needs instead (the same approach as the home-screen widget).
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun authRepository(): AuthRepository
    fun expenseRepository(): ExpenseRepository
    fun reminderSettingsRepository(): ReminderSettingsRepository
    fun reminderScheduler(): ReminderScheduler
}
