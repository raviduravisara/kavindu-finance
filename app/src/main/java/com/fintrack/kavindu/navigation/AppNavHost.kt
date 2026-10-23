package com.fintrack.kavindu.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fintrack.kavindu.AppMessages
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.EmptyState
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.feature.auth.ui.ForgotPasswordScreen
import com.fintrack.kavindu.feature.auth.ui.LoginScreen
import com.fintrack.kavindu.feature.auth.ui.RegisterScreen
import com.fintrack.kavindu.feature.auth.viewmodel.SessionUiState
import com.fintrack.kavindu.feature.auth.viewmodel.SessionViewModel
import com.fintrack.kavindu.feature.balancecheck.ui.BalanceCheckScreen
import com.fintrack.kavindu.feature.income.ui.EditIncomeScreen
import com.fintrack.kavindu.feature.income.ui.IncomeListScreen
import com.fintrack.kavindu.feature.settings.ui.SettingsScreen

// The four main screens in the bottom NavigationBar (UI spec, "App frame"): icon above label.
private enum class TopLevelTab(val route: Any, @StringRes val label: Int, val icon: ImageVector) {
    HOME(Dashboard, R.string.nav_home, Icons.Filled.Home),
    INCOME(IncomeList, R.string.nav_income, Icons.Filled.AccountBalanceWallet),
    HISTORY(TransactionHistory, R.string.nav_history, Icons.Filled.History),
    GOAL(GoalOverview, R.string.nav_goal, Icons.Filled.Flag),
}

@Composable
fun AppNavHost(
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val sessionState by sessionViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    if (sessionState == SessionUiState.Loading) {
        LoadingState()
        return
    }
    val isSignedIn = sessionState == SessionUiState.SignedIn

    // A fresh NavHost and back stack each time the user signs in or out, so Back never returns
    // to the login screens after login, or into the app after logout.
    key(isSignedIn) {
        val navController = rememberNavController()
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
        val currentTab = TopLevelTab.entries.firstOrNull { currentDestination?.hasRoute(it.route::class) == true }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }, // above the NavigationBar
            bottomBar = {
                // Only on the four main screens; not on login, Settings or the weekly check-in.
                if (isSignedIn && currentTab != null) {
                    AppNavigationBar(currentTab = currentTab, onTabSelected = { navController.openTab(it) })
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (isSignedIn) Dashboard else Login,
                // consumeWindowInsets: each screen's own top bar does not add the status bar space again.
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                composable<Login> {
                    LoginScreen(
                        onCreateAccountClicked = { navController.navigate(Register) },
                        onForgotPasswordClicked = { navController.navigate(ForgotPassword) },
                    )
                }
                composable<Register> {
                    RegisterScreen(onLogInClicked = { navController.popBackStack() })
                }
                composable<ForgotPassword> {
                    ForgotPasswordScreen(onBackToLoginClicked = { navController.popBackStack() })
                }

                // Placeholders until Member 3's dashboard and Member 2's and 3's tab screens exist.
                composable<Dashboard> {
                    DashboardPlaceholder(
                        onSettingsClicked = { navController.navigate(Settings) },
                        onCheckInClicked = { navController.navigate(WeeklyBalanceCheck) },
                    )
                }
                // Member 2: income
                composable<IncomeList> {
                    IncomeListScreen(
                        onAddIncomeClicked = { navController.navigate(EditIncome()) },
                        onIncomeClicked = { incomeId -> navController.navigate(EditIncome(incomeId)) },
                    )
                }
                composable<EditIncome> {
                    EditIncomeScreen(
                        onBackClicked = { navController.popBackStack() },
                        onDone = { navController.popBackStack() },
                    )
                }
                composable<TransactionHistory> {
                    TabPlaceholder(R.string.nav_history, R.string.placeholder_history) { navController.openTab(TopLevelTab.HOME) }
                }
                composable<GoalOverview> {
                    TabPlaceholder(R.string.nav_goal, R.string.placeholder_goal) { navController.openTab(TopLevelTab.HOME) }
                }

                composable<Settings> {
                    SettingsScreen(
                        onBackClicked = { navController.popBackStack() },
                        onBalanceCheckClicked = { navController.navigate(WeeklyBalanceCheck) },
                    )
                }
                composable<WeeklyBalanceCheck> {
                    BalanceCheckScreen(onBackClicked = { navController.popBackStack() })
                }
            }
        }
    }

    AppMessages(snackbarHostState)
}

// Switching tabs keeps each tab's own back stack and never stacks the same tab twice.
private fun NavHostController.openTab(tab: TopLevelTab) {
    navigate(tab.route) {
        popUpTo<Dashboard> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppNavigationBar(currentTab: TopLevelTab, onTabSelected: (TopLevelTab) -> Unit) {
    NavigationBar {
        TopLevelTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == currentTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

// Temporary. The real dashboard (Member 3) keeps the same top bar: wallet mark, "This cycle", ⋮ → Settings.
@Composable
private fun DashboardPlaceholder(onSettingsClicked: () -> Unit, onCheckInClicked: () -> Unit) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.dashboard_title),
                appIcon = Icons.Filled.AccountBalanceWallet,
                actions = {
                    IconButton(onClick = onSettingsClicked) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_settings))
                    }
                },
            )
        },
    ) { innerPadding ->
        EmptyState(
            message = stringResource(R.string.placeholder_dashboard),
            actionLabel = stringResource(R.string.placeholder_dashboard_action),
            onAction = onCheckInClicked,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

// Temporary screen for a tab whose real screen is not built yet.
@Composable
private fun TabPlaceholder(@StringRes title: Int, @StringRes message: Int, onBackToHomeClicked: () -> Unit) {
    Scaffold(topBar = { AppTopBar(title = stringResource(title)) }) { innerPadding ->
        EmptyState(
            message = stringResource(message),
            actionLabel = stringResource(R.string.placeholder_back_home),
            onAction = onBackToHomeClicked,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
