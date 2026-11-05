package com.fintrack.kavindu.feature.expense.viewmodel

import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRulesTest {

    private fun category(id: String, name: String, order: Int) =
        Category(id = id, name = name, type = CategoryType.DISCRETIONARY, sortOrder = order, createdAt = 1L)

    private val existing = listOf(category("a", "Coffee & dining", 0), category("b", "Transport", 1))

    @Test
    fun `a new name is valid and trimmed`() {
        val result = CategoryRules.validateName("  Books  ", existing, editingId = null)
        assertTrue(result.isValid)
        assertEquals("Books", result.name)
    }

    @Test
    fun `an empty or blank name is required`() {
        assertEquals(CategoryNameError.REQUIRED, CategoryRules.validateName("", existing, null).error)
        assertEquals(CategoryNameError.REQUIRED, CategoryRules.validateName("   ", existing, null).error)
    }

    @Test
    fun `a name over 100 characters is too long`() {
        assertNull(CategoryRules.validateName("x".repeat(100), existing, null).error)
        assertEquals(CategoryNameError.TOO_LONG, CategoryRules.validateName("x".repeat(101), existing, null).error)
    }

    @Test
    fun `a name that already exists is a duplicate, ignoring case`() {
        assertEquals(CategoryNameError.DUPLICATE, CategoryRules.validateName("transport", existing, null).error)
    }

    @Test
    fun `keeping the same name while editing is not a duplicate`() {
        assertTrue(CategoryRules.validateName("Transport", existing, editingId = "b").isValid)
        assertFalse(CategoryRules.validateName("Transport", existing, editingId = "a").isValid)
    }

    @Test
    fun `next sort order goes after the last one, and starts at 0 for an empty list`() {
        assertEquals(2, CategoryRules.nextSortOrder(existing))
        assertEquals(0, CategoryRules.nextSortOrder(emptyList()))
    }
}
