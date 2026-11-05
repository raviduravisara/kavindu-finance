package com.fintrack.kavindu

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.core.ui.theme.KavinduFinanceTheme
import com.fintrack.kavindu.feature.settings.viewmodel.AppThemeViewModel
import com.fintrack.kavindu.feature.settings.viewmodel.ThemeRules
import com.fintrack.kavindu.navigation.AppLaunch
import com.fintrack.kavindu.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

// The app's single activity. The app frame (Scaffold, NavigationBar, snackbars) lives in AppNavHost.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Counts requests to open quick-add from outside the app (the home-screen widget's "Add expense").
    // AppNavHost reacts to each new request once.
    private var quickAddRequests by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // After a rotation, keep the count (AppNavHost remembers how many it handled); only a fresh start
        // reads the widget's request from the intent.
        if (savedInstanceState != null) {
            quickAddRequests = savedInstanceState.getInt(KEY_QUICK_ADD_REQUESTS, 0)
        } else if (intent.takeQuickAddRequest()) {
            quickAddRequests++
        }
        setContent {
            // Appearance setting (System / Light / Dark), chosen in Settings and saved on this phone.
            val themeViewModel: AppThemeViewModel = hiltViewModel()
            val themeState by themeViewModel.uiState.collectAsState()
            val darkTheme = ThemeRules.isDark(themeState.mode, systemIsDark = isSystemInDarkTheme())

            // The status bar and navigation bar icons follow the app's theme, not only the phone's,
            // so the clock and battery stay readable when the app is set to Dark on a light phone (and the reverse).
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            KavinduFinanceTheme(darkTheme = darkTheme) {
                AppNavHost(quickAddRequests = quickAddRequests)
            }
        }
    }

    // The app was already open (launchMode singleTop) and the widget was tapped again.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.takeQuickAddRequest()) quickAddRequests++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_QUICK_ADD_REQUESTS, quickAddRequests)
    }

    // Reads the extra once and removes it, so it is never handled twice.
    private fun Intent.takeQuickAddRequest(): Boolean {
        val requested = getBooleanExtra(AppLaunch.EXTRA_OPEN_QUICK_ADD, false)
        removeExtra(AppLaunch.EXTRA_OPEN_QUICK_ADD)
        return requested
    }

    private companion object {
        const val KEY_QUICK_ADD_REQUESTS = "quick_add_requests"

        // The see-through bar behind 3-button navigation on older phones: the same values enableEdgeToEdge() uses
        // by default. System bar styling, not app UI colours (those all come from the theme).
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
