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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.model.AuthError
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.theme.Dimens

// Building blocks shared by the login, register and forgot-password screens.
// They follow the UI spec's form pattern (section 5): full-width OutlinedTextFields 16.dp apart,
// one full-width filled Button, secondary actions as TextButtons.

/**
 * The frame of an auth form. With [onBackClicked], the title goes in a small top bar with a back arrow
 * (Register, Forgot password); without it, the title is shown at the top of the form (Login).
 */
@Composable
fun AuthFormColumn(
    title: String,
    subtitle: String,
    onBackClicked: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            if (onBackClicked != null) AppTopBar(title = title, onBackClicked = onBackClicked)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.FormFieldGap),
        ) {
            if (onBackClicked == null) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
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
            IconButton(onClick = onVisibilityToggle) {
                Icon(
                    imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(if (isVisible) R.string.auth_hide else R.string.auth_show),
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

// The form's message, under the fields so the user keeps what they typed; pressing the button again
// is the retry. Red only for validation errors (the spec's rule); other problems use a calm colour.
@Composable
fun AuthErrorText(error: AuthError?) {
    if (error != null) {
        Text(
            text = stringResource(error.messageRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = if (error.isValidationError()) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
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

// Problems with what the user typed. No internet, too many attempts and unknown errors are not.
private fun AuthError.isValidationError(): Boolean = when (this) {
    AuthError.NO_NETWORK, AuthError.TOO_MANY_ATTEMPTS, AuthError.UNKNOWN -> false
    else -> true
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
