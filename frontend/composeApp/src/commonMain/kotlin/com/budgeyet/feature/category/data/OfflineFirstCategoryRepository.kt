package com.budgeyet.feature.category.data

import com.budgeyet.core.cache.LocalCacheStore
import com.budgeyet.core.model.Category
import com.budgeyet.core.offline.networkFirstRead
import com.budgeyet.feature.category.domain.CategoryRepository

// Read-through-cache wrapper around RealCategoryRepository. Reads fall back to the local cache on
// connectivity failure (so Category Limits / Add Transaction / History still work offline); writes
// are deliberately NOT queued (per the AGENTS.md offline note, only transactions sync offline) and
// pass straight through to the real repo — an offline write throws AppException.NetworkException,
// which the controller surfaces inline. After a successful write the cache is updated so a later
// offline read still shows the new state.
class OfflineFirstCategoryRepository(
    private val delegate: CategoryRepository,
    private val cacheStore: LocalCacheStore
) : CategoryRepository {

    override suspend fun getCategories(): List<Category> = networkFirstRead(
        networkCall = { delegate.getCategories() },
        cached = { cacheStore.getCachedCategories() },
        onSuccess = { cacheStore.cacheCategories(it) }
    )

    override suspend fun getCategory(categoryId: Long): Category? = networkFirstRead(
        networkCall = { delegate.getCategory(categoryId) },
        cached = { cacheStore.getCachedCategories()?.find { it.id == categoryId } },
        onSuccess = { found ->
            if (found != null) {
                val current = cacheStore.getCachedCategories()
                if (current != null) {
                    cacheStore.cacheCategories(current.map { if (it.id == found.id) found else it })
                }
            }
        }
    )

    override suspend fun updateCategoryLimits(limits: Map<Long, Double>) {
        delegate.updateCategoryLimits(limits)
        val cached = cacheStore.getCachedCategories() ?: return
        cacheStore.cacheCategories(
            cached.map { category ->
                if (limits.containsKey(category.id)) category.copy(monthlyLimit = limits.getValue(category.id)) else category
            }
        )
    }

    override suspend fun createCategory(name: String, icon: String, monthlyLimit: Double): Category {
        val created = delegate.createCategory(name, icon, monthlyLimit)
        cacheStore.cacheCategories((cacheStore.getCachedCategories() ?: emptyList()) + created)
        return created
    }

    override suspend fun deleteCategory(categoryId: Long) {
        delegate.deleteCategory(categoryId)
        val cached = cacheStore.getCachedCategories() ?: return
        cacheStore.cacheCategories(cached.filterNot { it.id == categoryId })
    }
}
