package com.fintrack.kavindu.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.components.EmptyState
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.feature.auth.ui.ForgotPasswordScreen
import com.fintrack.kavindu.feature.balancecheck.ui.BalanceCheckScreen
import com.fintrack.kavindu.feature.auth.ui.LoginScreen
import com.fintrack.kavindu.feature.auth.ui.RegisterScreen
import com.fintrack.kavindu.feature.auth.viewmodel.SessionUiState
import com.fintrack.kavindu.feature.auth.viewmodel.SessionViewModel
import com.fintrack.kavindu.feature.settings.ui.SettingsScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val sessionState by sessionViewModel.uiState.collectAsState()

    if (sessionState == SessionUiState.Loading) {
        LoadingState(modifier = modifier)
        return
    }
    val isSignedIn = sessionState == SessionUiState.SignedIn

    // A fresh NavHost and back stack each time the user signs in or out, so Back never returns
    // to the login screens after login, or into the app after logout.
    key(isSignedIn) {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = if (isSignedIn) Dashboard else Login,
            modifier = modifier,
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
            // Placeholder until Member 3's dashboard exists; the real dashboard links to Settings the same way.
            composable<Dashboard> {
                EmptyState(
                    message = stringResource(R.string.placeholder_dashboard),
                    actionLabel = stringResource(R.string.action_settings),
                    onAction = { navController.navigate(Settings) },
                )
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
