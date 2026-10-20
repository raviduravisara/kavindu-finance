package com.fintrack.kavindu.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fintrack.kavindu.Greeting

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Dashboard,
        modifier = modifier,
    ) {
        // Placeholder until the real dashboard screen exists. The login check comes with M1.
        composable<Dashboard> {
            Greeting(name = "Android")
        }
    }
}
