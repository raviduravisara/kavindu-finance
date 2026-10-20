package com.fintrack.kavindu.feature.auth.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.model.AuthError

// Building blocks shared by the login, register and forgot-password screens.

@Composable
fun AuthFormColumn(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.auth_email)) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    enabled: Boolean,
    imeAction: ImeAction,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = onVisibilityToggle) {
                Text(stringResource(if (isVisible) R.string.auth_hide else R.string.auth_show))
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

// The form's message. Shown under the fields so the user keeps what they typed; pressing the
// button again is the retry.
@Composable
fun AuthErrorText(error: AuthError?) {
    if (error != null) {
        Text(
            text = stringResource(error.messageRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
fun AuthButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(text)
        }
    }
}

@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.EMAIL_REQUIRED -> R.string.auth_error_email_required
    AuthError.INVALID_EMAIL -> R.string.auth_error_invalid_email
    AuthError.PASSWORD_REQUIRED -> R.string.auth_error_password_required
    AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthError.PASSWORDS_DO_NOT_MATCH -> R.string.auth_error_passwords_do_not_match
    AuthError.WRONG_CREDENTIALS -> R.string.auth_error_wrong_credentials
    AuthError.EMAIL_IN_USE -> R.string.auth_error_email_in_use
    AuthError.NO_NETWORK -> R.string.auth_error_no_network
    AuthError.TOO_MANY_ATTEMPTS -> R.string.auth_error_too_many_attempts
    AuthError.UNKNOWN -> R.string.auth_error_unknown
}
