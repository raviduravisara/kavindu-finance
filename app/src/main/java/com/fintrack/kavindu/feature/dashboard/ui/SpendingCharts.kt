package com.fintrack.kavindu.feature.dashboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.ui.components.StandardCard
import com.fintrack.kavindu.feature.dashboard.viewmodel.CategorySlice
import com.fintrack.kavindu.feature.dashboard.viewmodel.DashboardDetails
import com.fintrack.kavindu.feature.dashboard.viewmodel.SliceKind

// The two spending pictures on the dashboard. Both are drawn with Compose (Canvas and plain layouts), so there is no
// chart library to configure. Colours are theme roles only: committed = secondary, discretionary = tertiary (UI spec).

/** "Where it went": one bar split into committed and discretionary, with a legend (M7, FR-30, RC1). */
@Composable
internal fun WhereItWentCard(details: DashboardDetails) {
    val committedColor = MaterialTheme.colorScheme.secondary
    val discretionaryColor = MaterialTheme.colorScheme.tertiary
    StandardCard {
        Text(text = stringResource(R.string.dashboard_where_it_went), style = MaterialTheme.typography.titleMedium)
        SplitBar(
            committedFraction = details.committedFraction,
            discretionaryFraction = details.discretionaryFraction,
            committedColor = committedColor,
            discretionaryColor = discretionaryColor,
            emptyColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        LegendRow(
            color = committedColor,
            label = stringResource(R.string.dashboard_committed),
            value = stringResource(R.string.dashboard_legend_value, details.committed, details.committedPercent),
        )
        LegendRow(
            color = discretionaryColor,
            label = stringResource(R.string.dashboard_discretionary),
            value = stringResource(R.string.dashboard_legend_value, details.discretionary, details.discretionaryPercent),
        )
    }
}

// 12.dp tall, two rounded segments with a 3.dp gap, widths in proportion to the two amounts (UI spec).
@Composable
private fun SplitBar(
    committedFraction: Float,
    discretionaryFraction: Float,
    committedColor: Color,
    discretionaryColor: Color,
    emptyColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Row(modifier = modifier.fillMaxWidth().height(12.dp)) {
        when {
            committedFraction <= 0f && discretionaryFraction <= 0f ->
                Box(modifier = Modifier.weight(1f).height(12.dp).background(emptyColor, shape))
            else -> {
                if (committedFraction > 0f) {
                    Box(modifier = Modifier.weight(committedFraction).height(12.dp).background(committedColor, shape))
                }
                if (committedFraction > 0f && discretionaryFraction > 0f) Spacer(modifier = Modifier.width(3.dp))
                if (discretionaryFraction > 0f) {
                    Box(modifier = Modifier.weight(discretionaryFraction).height(12.dp).background(discretionaryColor, shape))
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
        }
        Text(text = value, style = MaterialTheme.typography.labelLarge)
    }
}

/** "By category": where the money went this cycle, biggest first (S7). Bars share the committed/discretionary colours. */
@Composable
internal fun CategoryChartCard(slices: List<CategorySlice>) {
    if (slices.isEmpty()) return
    StandardCard {
        Text(text = stringResource(R.string.dashboard_by_category), style = MaterialTheme.typography.titleMedium)
        slices.forEach { slice ->
            val name = when (slice.kind) {
                SliceKind.CATEGORY -> slice.name ?: stringResource(R.string.dashboard_slice_other)
                SliceKind.UNRECORDED -> stringResource(R.string.dashboard_slice_unrecorded)
                SliceKind.REST -> stringResource(R.string.dashboard_slice_rest)
            }
            val color = when (slice.type) {
                CategoryType.COMMITTED -> MaterialTheme.colorScheme.secondary
                CategoryType.DISCRETIONARY -> MaterialTheme.colorScheme.tertiary
                null -> MaterialTheme.colorScheme.outline
            }
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = stringResource(R.string.dashboard_legend_value, slice.amount, slice.percent),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                SliceBar(
                    fraction = slice.fraction,
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

// A rounded track with a rounded fill, drawn on a Canvas. A tiny share still shows a small stub, so it is never invisible.
@Composable
private fun SliceBar(fraction: Float, color: Color, trackColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(8.dp)) {
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(color = trackColor, cornerRadius = corner)
        val filled = (size.width * fraction.coerceIn(0f, 1f)).coerceAtLeast(size.height)
        drawRoundRect(color = color, size = Size(filled, size.height), cornerRadius = corner)
    }
}
