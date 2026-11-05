package com.fintrack.kavindu.feature.widget.data

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.fintrack.kavindu.feature.widget.ui.QuickAddWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the home-screen widget up to date while the app is running. A save goes Firestore -> listener -> Room
 * (straight away, offline too), Room's flow changes, and this redraws the widget. Logging out clears Room, so the
 * widget switches to "Open Kavindu Finance to start". When the app is closed, Android refreshes the widget itself
 * (every 60 minutes, set in quick_add_widget_info.xml).
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val widgetStateRepository: WidgetStateRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            widgetStateRepository.observeWidgetState()
                .distinctUntilChanged()
                .catch { error -> Log.w(TAG, "Widget data stopped", error) }
                .collect {
                    try {
                        QuickAddWidget().updateAll(context)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // A widget that cannot redraw must never crash the app.
                        Log.w(TAG, "Widget update failed", e)
                    }
                }
        }
    }

    private companion object {
        const val TAG = "WidgetUpdater"
    }
}
