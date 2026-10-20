package com.fintrack.kavindu.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.theme.KavinduFinanceTheme

/** Shown while a screen waits for its data. Every screen uses this instead of a blank page. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.state_loading)
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingStatePreview() {
    KavinduFinanceTheme {
        LoadingState()
    }
}
