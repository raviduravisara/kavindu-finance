package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.AppError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

// The Firestore data sources report background problems here; the app shows them as messages.
@Singleton
class ErrorRepositoryImpl @Inject constructor() : ErrorRepository {

    private val _backgroundErrors = MutableSharedFlow<AppError>(extraBufferCapacity = 8)
    override val backgroundErrors: SharedFlow<AppError> = _backgroundErrors.asSharedFlow()

    fun report(error: AppError) {
        _backgroundErrors.tryEmit(error)
    }
}
