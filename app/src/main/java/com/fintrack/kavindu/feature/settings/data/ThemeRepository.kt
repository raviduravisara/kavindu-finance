package com.fintrack.kavindu.feature.settings.data

import android.content.Context
import androidx.core.content.edit
import com.fintrack.kavindu.feature.settings.viewmodel.ThemeMode
import com.fintrack.kavindu.feature.settings.viewmodel.ThemeRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Appearance setting, saved on this phone only (SharedPreferences), not in Firestore: how the app looks is a
 * device preference, not financial data. It stays after logout.
 */
@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(ThemeRules.fromStored(prefs.getString(KEY_MODE, null)))
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_MODE, mode.name) }
        _mode.value = mode
    }

    private companion object {
        const val PREFS_NAME = "appearance"
        const val KEY_MODE = "theme_mode"
    }
}
