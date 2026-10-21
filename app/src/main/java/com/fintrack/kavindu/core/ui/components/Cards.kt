package com.fintrack.kavindu.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.core.ui.theme.KavinduFinanceTheme

// The three card styles from the UI spec (section 1). Every member uses these for cards and tiles.
// tile = true gives the smaller tile padding (for two tiles side by side).

/** Hero card: the screen's main number (safe-to-spend, goal saved so far). */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = SpecCard(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    tile = false,
    onClick = onClick,
    modifier = modifier,
    content = content,
)

/** Standard card or tile. Use onSurfaceVariant for labels inside it. */
@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    tile: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = SpecCard(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    tile = tile,
    onClick = onClick,
    modifier = modifier,
    content = content,
)

/** Prompt card: a gentle suggestion, such as the weekly check-in. */
@Composable
fun PromptCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = SpecCard(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    tile = false,
    onClick = onClick,
    modifier = modifier,
    content = content,
)

@Composable
private fun SpecCard(
    containerColor: Color,
    contentColor: Color,
    tile: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.CardCorner)
    val colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(
                horizontal = if (tile) Dimens.TilePaddingHorizontal else Dimens.CardPaddingHorizontal,
                vertical = if (tile) Dimens.TilePaddingVertical else Dimens.CardPaddingVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation, content = inner)
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation, content = inner)
    }
}

@Preview(showBackground = true)
@Composable
private fun CardsPreview() {
    KavinduFinanceTheme {
        Column(modifier = Modifier.padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
            HeroCard { Text("Hero card", style = MaterialTheme.typography.labelLarge) }
            StandardCard { Text("Standard card", style = MaterialTheme.typography.titleMedium) }
            PromptCard { Text("Prompt card", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
