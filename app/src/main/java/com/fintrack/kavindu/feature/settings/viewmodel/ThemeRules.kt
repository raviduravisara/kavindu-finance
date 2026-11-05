package com.fintrack.kavindu.feature.settings.viewmodel

/** The Appearance choice in Settings. SYSTEM follows the phone's own light or dark setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Theme setting rules (E3). Plain Kotlin with no Android code, so it is unit tested. */
object ThemeRules {

    /** Whether the app draws with the dark colour scheme. */
    fun isDark(mode: ThemeMode, systemIsDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemIsDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    /** Reads the saved choice; anything missing or unknown means SYSTEM, so the app never breaks on an old value. */
    fun fromStored(value: String?): ThemeMode = ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.SYSTEM
}
