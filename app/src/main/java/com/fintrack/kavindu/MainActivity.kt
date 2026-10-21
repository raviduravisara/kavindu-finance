package com.fintrack.kavindu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fintrack.kavindu.core.ui.theme.KavinduFinanceTheme
import com.fintrack.kavindu.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

// The app's single activity. The app frame (Scaffold, NavigationBar, snackbars) lives in AppNavHost.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KavinduFinanceTheme {
                AppNavHost()
            }
        }
    }
}
