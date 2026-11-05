package com.fintrack.kavindu.feature.widget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fintrack.kavindu.MainActivity
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.theme.DarkColorScheme
import com.fintrack.kavindu.core.ui.theme.LightColorScheme
import com.fintrack.kavindu.feature.widget.data.WidgetEntryPoint
import com.fintrack.kavindu.feature.widget.viewmodel.WidgetState
import com.fintrack.kavindu.navigation.AppLaunch
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

// Our own light and dark schemes, the same as the app (no dynamic colour). The widget follows the phone's dark mode.
private val WidgetColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

private val OpenQuickAddKey = ActionParameters.Key<Boolean>(AppLaunch.EXTRA_OPEN_QUICK_ADD)

/**
 * The 4x1 home-screen widget (N2): "Safe to spend today" and an "Add expense" button that opens the app with the
 * quick-add sheet ready, so recording an expense is still amount -> category -> Save.
 * It only shows [WidgetState]; the numbers come from Room through [WidgetEntryPoint].
 */
class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).widgetStateRepository()
        val state = try {
            repository.observeWidgetState().first()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WidgetState.SignedOut // never a broken widget: it simply invites him to open the app
        }
        provideContent {
            GlanceTheme(colors = WidgetColors) {
                WidgetContent(state)
            }
        }
    }
}

@Composable
private fun WidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val openApp = actionStartActivity<MainActivity>()
    val openQuickAdd = actionStartActivity<MainActivity>(actionParametersOf(OpenQuickAddKey to true))

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(20.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight().clickable(openApp)) {
            when (state) {
                is WidgetState.Ready -> {
                    Label(context.getString(R.string.widget_safe_today))
                    Amount(state.perDay)
                }
                is WidgetState.Over -> {
                    // A plain negative number in the same colours: no red, no warning.
                    Label(context.getString(R.string.widget_safe_cycle))
                    Amount(state.amount)
                }
                WidgetState.NoData, WidgetState.SignedOut -> Message(context.getString(R.string.widget_open_app))
            }
        }
        if (state !is WidgetState.SignedOut) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            Button(text = context.getString(R.string.widget_add_expense), onClick = openQuickAdd)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text = text, style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 13.sp))
}

@Composable
private fun Amount(text: String) {
    Text(
        text = text,
        style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 22.sp, fontWeight = FontWeight.Medium),
    )
}

@Composable
private fun Message(text: String) {
    Text(text = text, style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 14.sp))
}
