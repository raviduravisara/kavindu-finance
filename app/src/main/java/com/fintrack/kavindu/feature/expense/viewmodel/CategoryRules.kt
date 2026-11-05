package com.fintrack.kavindu.feature.expense.viewmodel

import com.fintrack.kavindu.core.model.Category

/** Why a category name cannot be saved. Shown under the name field. */
enum class CategoryNameError {
    REQUIRED,
    TOO_LONG, // the security rules allow 1 to 100 characters
    DUPLICATE, // two chips with the same name would be impossible to tell apart on quick-add
}

data class CategoryValidation(val name: String, val error: CategoryNameError?) {
    val isValid: Boolean get() = error == null
}

/** The rules for adding and renaming categories (M4, FR-20). Plain Kotlin, unit tested. */
object CategoryRules {

    const val MAX_NAME_LENGTH = 100

    /** [editingId] is the category being renamed, so it is not compared with itself. Null when adding. */
    fun validateName(rawName: String, existing: List<Category>, editingId: String?): CategoryValidation {
        val name = rawName.trim()
        val error = when {
            name.isEmpty() -> CategoryNameError.REQUIRED
            name.length > MAX_NAME_LENGTH -> CategoryNameError.TOO_LONG
            existing.any { it.id != editingId && it.name.equals(name, ignoreCase = true) } -> CategoryNameError.DUPLICATE
            else -> null
        }
        return CategoryValidation(name, error)
    }

    /** A new category goes to the end of his list. */
    fun nextSortOrder(existing: List<Category>): Int = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
}
