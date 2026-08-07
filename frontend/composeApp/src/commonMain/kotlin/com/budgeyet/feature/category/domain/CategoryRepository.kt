package com.budgeyet.feature.category.domain

import com.budgeyet.core.model.Category

interface CategoryRepository {
    suspend fun getCategories(): List<Category>
    suspend fun getCategory(categoryId: Long): Category?
    suspend fun updateCategoryLimits(limits: Map<Long, Double>)
    suspend fun createCategory(name: String, icon: String, monthlyLimit: Double): Category
    // Caller must reassign any existing transactions off this category first (PRD C1) —
    // this only removes the category itself.
    suspend fun deleteCategory(categoryId: Long)
}
