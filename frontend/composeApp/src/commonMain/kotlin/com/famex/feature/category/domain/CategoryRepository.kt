package com.famex.feature.category.domain

import com.famex.core.model.Category

interface CategoryRepository {
    suspend fun getCategories(): List<Category>
    suspend fun getCategory(categoryId: Long): Category?
    suspend fun updateCategoryLimits(limits: Map<Long, Double>)
    suspend fun createCategory(name: String, icon: String, monthlyLimit: Double): Category
}
