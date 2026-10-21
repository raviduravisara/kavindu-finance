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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.core.ui.theme.KavinduFinanceTheme

/**
 * Shown when loading or saving failed, for example with no network.
 * [message] says what went wrong in plain words; [onRetry] is the screen's retry event.
 * The text uses a calm neutral colour, not red, to match the app's tone.
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
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
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.action_try_again))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    KavinduFinanceTheme {
        ErrorState(
            message = "Could not load your data. Check your connection.",
            onRetry = {},
        )
    }
}
