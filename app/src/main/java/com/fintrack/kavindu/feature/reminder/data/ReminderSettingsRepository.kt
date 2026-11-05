package com.fintrack.kavindu.feature.reminder.data

import android.content.Context
import androidx.core.content.edit
import com.fintrack.kavindu.feature.reminder.viewmodel.ReminderTime
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** The daily reminder's setting: off by default, and the time of day it is shown. */
data class ReminderSettings(
    val isEnabled: Boolean = false,
    val time: LocalTime = ReminderTime.DEFAULT,
)

/**
 * Saves the reminder setting on this phone only (SharedPreferences), not in Firestore.
 * A reminder belongs to the device that shows the notification, not to the account: Firestore keeps
 * Kavindu's financial data, and a second phone or tablet should not buzz too.
 */
@Singleton
class ReminderSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<ReminderSettings> = _settings.asStateFlow()

    fun current(): ReminderSettings = _settings.value

    fun save(settings: ReminderSettings) {
        prefs.edit {
            putBoolean(KEY_ENABLED, settings.isEnabled)
            putInt(KEY_MINUTE_OF_DAY, settings.time.hour * 60 + settings.time.minute)
        }
        _settings.value = settings
    }

    private fun read(): ReminderSettings {
        val defaultMinute = ReminderTime.DEFAULT.hour * 60 + ReminderTime.DEFAULT.minute
        val minuteOfDay = prefs.getInt(KEY_MINUTE_OF_DAY, defaultMinute).coerceIn(0, 24 * 60 - 1)
        return ReminderSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60),
        )
    }

    private companion object {
        const val PREFS_NAME = "daily_reminder"
        const val KEY_ENABLED = "enabled"
        const val KEY_MINUTE_OF_DAY = "minute_of_day" // e.g. 1200 = 20:00
    }
}
