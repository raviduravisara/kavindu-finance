package com.fintrack.kavindu.feature.widget.data

import com.fintrack.kavindu.core.data.repository.AuthRepository
import com.fintrack.kavindu.feature.dashboard.data.DashboardRepository
import com.fintrack.kavindu.feature.dashboard.viewmodel.DashboardBuilder
import com.fintrack.kavindu.feature.widget.viewmodel.WidgetState
import com.fintrack.kavindu.feature.widget.viewmodel.WidgetStateMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The widget's data, live from Room. It reuses the dashboard's own stream and builder, so the widget's
 * "safe to spend today" is exactly the per-day number on the home screen: there is no second copy of the maths.
 */
class WidgetStateRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val dashboardRepository: DashboardRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeWidgetState(): Flow<WidgetState> =
        authRepository.currentUserId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(WidgetState.SignedOut)
            } else {
                dashboardRepository.observeDashboard()
                    .map { input -> WidgetStateMapper.map(userId, DashboardBuilder.build(input)) }
            }
        }
}
