package com.fintrack.kavindu.feature.widget.viewmodel

import com.fintrack.kavindu.feature.dashboard.viewmodel.DashboardDetails

/** What the home-screen widget shows. All amounts arrive already formatted, e.g. "LKR 2,119". */
sealed interface WidgetState {
    /** Nobody is signed in: "Open Kavindu Finance to start". */
    data object SignedOut : WidgetState

    /** Signed in, but nothing is recorded in this pay cycle yet. */
    data object NoData : WidgetState

    /** What he can spend today and stay on track: the dashboard's per-day number. */
    data class Ready(val perDay: String) : WidgetState

    /** Over for this cycle: the plain negative amount, e.g. "-LKR 4,000". Never shown as an alarm. */
    data class Over(val amount: String) : WidgetState
}

/**
 * Turns the dashboard's numbers into the widget's state (N2). Plain Kotlin, unit tested.
 * It does no maths of its own: every number comes from [DashboardDetails], which the dashboard's
 * builder already worked out, so the widget and the home screen always show the same amount.
 */
object WidgetStateMapper {

    fun map(userId: String?, details: DashboardDetails?): WidgetState = when {
        userId == null -> WidgetState.SignedOut
        details == null || !details.hasEntries -> WidgetState.NoData
        details.isOver -> WidgetState.Over(details.safeToSpend)
        else -> WidgetState.Ready(details.perDay)
    }
}
