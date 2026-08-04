package com.famex.feature.category.data

import com.famex.core.model.Category
import com.famex.feature.category.domain.CategoryRepository
import com.famex.fixtures.DummyScenario
import com.famex.fixtures.dummyCategories
import kotlinx.coroutines.delay

class FakeCategoryRepository(private val scenario: DummyScenario) : CategoryRepository {
    override suspend fun getCategories(): List<Category> {
        delay(400)
        return dummyCategories(scenario)
    }

    override suspend fun getCategory(categoryId: Long): Category? {
        delay(300)
        return dummyCategories(scenario).find { it.id == categoryId }
    }
}
