package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>

    suspend fun saveCategory(category: Category): Result<Unit>
}
