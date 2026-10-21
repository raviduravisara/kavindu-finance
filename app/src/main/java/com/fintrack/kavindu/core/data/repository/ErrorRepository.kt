package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.AppError
import kotlinx.coroutines.flow.Flow

interface ErrorRepository {
    /**
     * Problems that happen in the background, after a screen has moved on: a save the server
     * rejected, or data that could not be loaded. The app shows each one as a short message.
     */
    val backgroundErrors: Flow<AppError>
}
