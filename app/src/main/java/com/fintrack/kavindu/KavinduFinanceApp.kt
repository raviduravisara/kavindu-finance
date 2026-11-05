package com.fintrack.kavindu

import android.app.Application
import com.fintrack.kavindu.feature.widget.data.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KavinduFinanceApp : Application() {

    @Inject lateinit var widgetUpdater: WidgetUpdater

    override fun onCreate() {
        super.onCreate()
        // Redraws the home-screen widget whenever the data in Room changes.
        widgetUpdater.start()
    }
}
