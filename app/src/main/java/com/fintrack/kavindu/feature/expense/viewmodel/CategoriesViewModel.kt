package com.fintrack.kavindu.feature.expense.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.CategoryRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.asAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryRow(val id: String, val name: String)

/** The add / rename dialog. [id] is null while adding a new category. */
data class CategoryEditor(
    val id: String?,
    val nameInput: String,
    val type: CategoryType,
    val nameError: CategoryNameError? = null,
    val isSaving: Boolean = false,
    val saveError: AppError? = null,
)

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val committed: List<CategoryRow> = emptyList(),
    val discretionary: List<CategoryRow> = emptyList(),
    val editor: CategoryEditor? = null,
) {
    val isEmpty: Boolean get() = committed.isEmpty() && discretionary.isEmpty()
}

/**
 * Categories with their committed or discretionary flag (M4, FR-20). Kavindu can rename a category, add his own, and
 * change a flag on any of them, so he never has to guess whether coffee is a "must pay" (RC3: no rigid model).
 *
 * Changing a flag only affects expenses saved from then on: each expense copies the flag when it is saved, so
 * the totals of earlier cycles never change behind his back.
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private var categories: List<Category> = emptyList()
    private var loadJob: Job? = null

    init {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            categoryRepository.observeCategories()
                .catch { error -> _uiState.update { it.copy(isLoading = false, loadError = error.asAppError()) } }
                .collect { all ->
                    categories = all
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = null,
                            committed = all.filter { c -> c.type == CategoryType.COMMITTED }.map { c -> CategoryRow(c.id, c.name) },
                            discretionary = all.filter { c -> c.type == CategoryType.DISCRETIONARY }.map { c -> CategoryRow(c.id, c.name) },
                        )
                    }
                }
        }
    }

    fun onRetryClicked() = load()

    fun onAddClicked() =
        _uiState.update { it.copy(editor = CategoryEditor(id = null, nameInput = "", type = CategoryType.DISCRETIONARY)) }

    fun onCategoryClicked(id: String) {
        val category = categories.firstOrNull { it.id == id } ?: return
        _uiState.update { it.copy(editor = CategoryEditor(id = category.id, nameInput = category.name, type = category.type)) }
    }

    fun onNameChange(text: String) = updateEditor { it.copy(nameInput = text, nameError = null, saveError = null) }

    fun onTypeSelected(type: CategoryType) = updateEditor { it.copy(type = type) }

    fun onEditorDismissed() = _uiState.update { it.copy(editor = null) }

    fun onSaveClicked() {
        val editor = _uiState.value.editor ?: return
        if (editor.isSaving) return

        val check = CategoryRules.validateName(editor.nameInput, categories, editor.id)
        if (!check.isValid) {
            updateEditor { it.copy(nameError = check.error) }
            return
        }

        updateEditor { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val existing = categories.firstOrNull { it.id == editor.id }
            val category = Category(
                id = existing?.id ?: "", // blank: the repository creates the Firestore document
                name = check.name,
                type = editor.type,
                sortOrder = existing?.sortOrder ?: CategoryRules.nextSortOrder(categories),
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            val result = categoryRepository.saveCategory(category)
            if (result.isSuccess) {
                _uiState.update { it.copy(editor = null) }
            } else {
                updateEditor { it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError()) }
            }
        }
    }

    private fun updateEditor(change: (CategoryEditor) -> CategoryEditor) =
        _uiState.update { state -> state.editor?.let { state.copy(editor = change(it)) } ?: state }
}
