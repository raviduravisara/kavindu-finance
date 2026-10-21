package com.fintrack.kavindu.feature.auth.ui

import androidx.compose.material3.MaterialTheme
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
import com.fintrack.kavindu.feature.auth.viewmodel.ForgotPasswordUiState
import com.fintrack.kavindu.feature.auth.viewmodel.ForgotPasswordViewModel

@Composable
fun ForgotPasswordScreen(
    onBackToLoginClicked: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ForgotPasswordContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onSendClicked = viewModel::onSendClicked,
        onBackToLoginClicked = onBackToLoginClicked,
    )
}

@Composable
private fun ForgotPasswordContent(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSendClicked: () -> Unit,
    onBackToLoginClicked: () -> Unit,
) {
    AuthFormColumn(
        title = stringResource(R.string.auth_reset_title),
        subtitle = stringResource(R.string.auth_reset_subtitle),
        onBackClicked = onBackToLoginClicked,
    ) {
        if (uiState.isEmailSent) {
            // Firebase does not reveal whether the email has an account, so neither do we.
            Text(
                text = stringResource(R.string.auth_reset_sent),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            EmailField(
                value = uiState.email,
                onValueChange = onEmailChange,
                enabled = !uiState.isLoading,
                imeAction = ImeAction.Done,
                onDone = onSendClicked,
            )
            AuthErrorText(error = uiState.error)
            AuthButton(
                text = stringResource(R.string.auth_send_reset_link),
                isLoading = uiState.isLoading,
                onClick = onSendClicked,
            )
        }
        TextButton(onClick = onBackToLoginClicked, enabled = !uiState.isLoading) {
            Text(stringResource(R.string.auth_back_to_login))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordPreview() {
    KavinduFinanceTheme {
        ForgotPasswordContent(
            uiState = ForgotPasswordUiState(),
            onEmailChange = {},
            onSendClicked = {},
            onBackToLoginClicked = {},
        )
    }
}
