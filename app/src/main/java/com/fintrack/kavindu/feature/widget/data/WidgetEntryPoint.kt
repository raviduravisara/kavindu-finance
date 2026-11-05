package com.fintrack.kavindu.feature.widget.data

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// A Glance widget is not an Activity or ViewModel, so Hilt cannot inject it directly.
// The widget asks this entry point for its repository instead.
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetStateRepository(): WidgetStateRepository
}
