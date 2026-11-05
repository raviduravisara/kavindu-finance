package com.fintrack.kavindu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.fintrack.kavindu.core.ui.theme.KavinduFinanceTheme
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
            KavinduFinanceTheme {
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
    }
}
