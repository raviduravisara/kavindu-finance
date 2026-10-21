package com.fintrack.kavindu.feature.auth.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.theme.KavinduFinanceTheme
import com.fintrack.kavindu.feature.auth.viewmodel.RegisterUiState
import com.fintrack.kavindu.feature.auth.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    onLogInClicked: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    RegisterContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
        onRegisterClicked = viewModel::onRegisterClicked,
        onLogInClicked = onLogInClicked,
    )
}

@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onRegisterClicked: () -> Unit,
    onLogInClicked: () -> Unit,
) {
    AuthFormColumn(
        title = stringResource(R.string.auth_register_title),
        subtitle = stringResource(R.string.auth_register_subtitle),
        onBackClicked = onLogInClicked,
    ) {
        EmailField(value = uiState.email, onValueChange = onEmailChange, enabled = !uiState.isLoading)
        PasswordField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_password),
            isVisible = uiState.isPasswordVisible,
            onVisibilityToggle = onPasswordVisibilityToggle,
            enabled = !uiState.isLoading,
            imeAction = ImeAction.Next,
        )
        PasswordField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = stringResource(R.string.auth_confirm_password),
            isVisible = uiState.isPasswordVisible,
            onVisibilityToggle = onPasswordVisibilityToggle,
            enabled = !uiState.isLoading,
            imeAction = ImeAction.Done,
            onDone = onRegisterClicked,
        )
        AuthErrorText(error = uiState.error)
        AuthButton(
            text = stringResource(R.string.auth_create_account),
            isLoading = uiState.isLoading,
            onClick = onRegisterClicked,
        )
        TextButton(onClick = onLogInClicked, enabled = !uiState.isLoading) {
            Text(stringResource(R.string.auth_have_account))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterPreview() {
    KavinduFinanceTheme {
        RegisterContent(
            uiState = RegisterUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onPasswordVisibilityToggle = {},
            onRegisterClicked = {},
            onLogInClicked = {},
        )
    }
}
