package com.fintrack.kavindu.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.core.ui.theme.KavinduFinanceTheme

/**
 * Shown when a screen has loaded but has nothing to show yet, for example on first login.
 * As the UI spec says: one friendly line and one action, such as "Set a goal".
 */
@Composable
fun EmptyState(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAction) {
            Text(text = actionLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    KavinduFinanceTheme {
        EmptyState(
            message = "Set a savings goal to see what you need each cycle",
            actionLabel = "Set a goal",
            onAction = {},
        )
    }
}
