package com.fintrack.kavindu.feature.auth.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.theme.KavinduFinanceTheme
import com.fintrack.kavindu.feature.auth.viewmodel.LoginUiState
import com.fintrack.kavindu.feature.auth.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onCreateAccountClicked: () -> Unit,
    onForgotPasswordClicked: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current // the Activity: Google's account picker opens over it
    LoginContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
        onLoginClicked = viewModel::onLoginClicked,
        onGoogleClicked = { viewModel.onGoogleClicked(context) },
        onForgotPasswordClicked = onForgotPasswordClicked,
        onCreateAccountClicked = onCreateAccountClicked,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onLoginClicked: () -> Unit,
    onGoogleClicked: () -> Unit,
    onForgotPasswordClicked: () -> Unit,
    onCreateAccountClicked: () -> Unit,
) {
    AuthFormColumn(
        title = stringResource(R.string.auth_login_title),
        subtitle = stringResource(R.string.auth_login_subtitle),
    ) {
        EmailField(value = uiState.email, onValueChange = onEmailChange, enabled = !uiState.isBusy)
        PasswordField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_password),
            isVisible = uiState.isPasswordVisible,
            onVisibilityToggle = onPasswordVisibilityToggle,
            enabled = !uiState.isBusy,
            imeAction = ImeAction.Done,
            onDone = onLoginClicked,
        )
        AuthErrorText(error = uiState.error)
        AuthButton(
            text = stringResource(R.string.auth_log_in),
            isLoading = uiState.isLoading,
            onClick = onLoginClicked,
            enabled = !uiState.isGoogleLoading,
        )
        OrDivider()
        GoogleButton(isLoading = uiState.isGoogleLoading, enabled = !uiState.isLoading, onClick = onGoogleClicked)
        TextButton(onClick = onForgotPasswordClicked, enabled = !uiState.isBusy) {
            Text(stringResource(R.string.auth_forgot_password))
        }
        TextButton(onClick = onCreateAccountClicked, enabled = !uiState.isBusy) {
            Text(stringResource(R.string.auth_no_account))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    KavinduFinanceTheme {
        LoginContent(
            uiState = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordVisibilityToggle = {},
            onLoginClicked = {},
            onGoogleClicked = {},
            onForgotPasswordClicked = {},
            onCreateAccountClicked = {},
        )
    }
}
