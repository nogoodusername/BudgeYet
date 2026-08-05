package com.famex.feature.category.data

import com.famex.core.model.Category
import com.famex.core.network.AppException
import com.famex.core.network.HouseholdRequestContextProvider
import com.famex.core.network.mapper.toDomain
import com.famex.feature.category.data.mapper.toDomain
import com.famex.feature.category.data.remote.CategoryApiService
import com.famex.feature.category.domain.CategoryRepository

// Real, network-backed CategoryRepository. CategoryRepository's interface takes no household id
// (it predates real networking and is shared across many call sites — see
// core/session/CurrentHouseholdHolder.kt for why), so every call resolves it via
// HouseholdRequestContextProvider instead.
class RealCategoryRepository(
    private val api: CategoryApiService,
    private val contextProvider: HouseholdRequestContextProvider
) : CategoryRepository {

    override suspend fun getCategories(): List<Category> {
        val (config, token, householdId) = contextProvider.get()
        return api.listCategories(config, token, householdId).map { it.toDomain() }
    }

    override suspend fun getCategory(categoryId: Long): Category? {
        val (config, token, householdId) = contextProvider.get()
        return try {
            api.getCategory(config, token, householdId, categoryId).toDomain()
        } catch (e: AppException.NotFoundException) {
            null
        }
    }

    override suspend fun updateCategoryLimits(limits: Map<Long, Double>) {
        val (config, token, householdId) = contextProvider.get()
        // No batch-update endpoint — one PATCH per changed category, same as
        // AuthRepository.setupCategories has to do one POST per category on create.
        for ((categoryId, newLimit) in limits) {
            api.updateCategoryLimit(config, token, householdId, categoryId, newLimit)
        }
    }

    override suspend fun createCategory(name: String, icon: String, monthlyLimit: Double): Category {
        val (config, token, householdId) = contextProvider.get()
        return api.createCategory(config, token, householdId, name, icon, monthlyLimit).toDomain()
    }

    override suspend fun deleteCategory(categoryId: Long) {
        val (config, token, householdId) = contextProvider.get()
        api.deleteCategory(config, token, householdId, categoryId)
    }
}
