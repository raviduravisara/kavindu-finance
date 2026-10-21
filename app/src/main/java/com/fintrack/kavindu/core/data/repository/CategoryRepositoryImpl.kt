package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.data.local.dao.CategoryDao
import com.fintrack.kavindu.core.data.local.entity.toModel
import com.fintrack.kavindu.core.data.remote.FirestorePaths
import com.fintrack.kavindu.core.data.remote.UserFirestore
import com.fintrack.kavindu.core.data.remote.documentFor
import com.fintrack.kavindu.core.data.remote.toFirestoreMap
import com.fintrack.kavindu.core.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val userFirestore: UserFirestore,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { rows -> rows.map { it.toModel() } }

    override suspend fun saveCategory(category: Category): Result<Unit> = userFirestore.write {
        userFirestore.collection(FirestorePaths.CATEGORIES).documentFor(category.id).set(category.toFirestoreMap())
    }
}
