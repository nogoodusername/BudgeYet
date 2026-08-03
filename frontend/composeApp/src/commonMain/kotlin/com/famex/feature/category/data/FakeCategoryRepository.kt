package com.famex.feature.category.data

import com.famex.core.model.Category
import com.famex.feature.category.domain.CategoryRepository
import com.famex.fixtures.DummyScenario
import com.famex.fixtures.dummyCategories
import kotlinx.coroutines.delay

class FakeCategoryRepository(scenario: DummyScenario) : CategoryRepository {
    // In-memory only — mutated by updateCategoryLimits so Save Changes persists for the
    // rest of the app session (this repository instance lives as long as AppContainer).
    private var categories: List<Category> = dummyCategories(scenario)

    override suspend fun getCategories(): List<Category> {
        delay(400)
        return categories
    }

    override suspend fun getCategory(categoryId: Long): Category? {
        delay(300)
        return categories.find { it.id == categoryId }
    }

    override suspend fun updateCategoryLimits(limits: Map<Long, Double>) {
        delay(300)
        categories = categories.map { category ->
            limits[category.id]?.let { newLimit -> category.copy(monthlyLimit = newLimit) } ?: category
        }
    }

    override suspend fun createCategory(name: String, icon: String, monthlyLimit: Double): Category {
        delay(300)
        val newCategory = Category(
            id = (categories.maxOfOrNull { it.id } ?: 0L) + 1,
            name = name,
            icon = icon,
            monthlyLimit = monthlyLimit
        )
        categories = categories + newCategory
        return newCategory
    }
}
