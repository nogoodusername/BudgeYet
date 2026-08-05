package com.famex.core.cache

import com.famex.core.model.Category
import com.famex.core.model.Household
import com.famex.core.model.Transaction
import com.famex.core.model.User
import com.famex.feature.dashboard.domain.model.DashboardData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Typed, JSON-backed mirror of the last successful network fetch, keyed per data type. Written
// by the OfflineFirst*Repository wrappers after every successful read/write, read back when a
// network call fails (see core/offline/networkFirstRead). No TTL in v1 — the cache is exactly
// "the last state we know the server was in", which is what an offline view should show.
//
// encodeDefaults = true so a Transaction whose field happens to equal its default value still
// round-trips (e.g. paymentMode = CARD, the default, is a real value that must survive).
class LocalCacheStore(
    private val storage: LocalFileStorage,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    // ---- Categories ----
    suspend fun cacheCategories(categories: List<Category>) =
        storage.writeString(KEY_CATEGORIES, json.encodeToString(categories))

    suspend fun getCachedCategories(): List<Category>? =
        storage.readString(KEY_CATEGORIES)?.decodeList()

    // ---- Transactions ----
    suspend fun cacheTransactions(transactions: List<Transaction>) =
        storage.writeString(KEY_TRANSACTIONS, json.encodeToString(transactions))

    suspend fun getCachedTransactions(): List<Transaction>? =
        storage.readString(KEY_TRANSACTIONS)?.decodeList()

    // ---- Dashboard snapshot ----
    suspend fun cacheDashboardData(data: DashboardData) =
        storage.writeString(KEY_DASHBOARD, json.encodeToString(data))

    suspend fun getCachedDashboardData(): DashboardData? =
        storage.readString(KEY_DASHBOARD)?.let { raw ->
            runCatching { json.decodeFromString<DashboardData>(raw) }.getOrNull()
        }

    // ---- Household ----
    suspend fun cacheHousehold(household: Household) =
        storage.writeString(KEY_HOUSEHOLD, json.encodeToString(household))

    suspend fun getCachedHousehold(): Household? =
        storage.readString(KEY_HOUSEHOLD)?.let { raw ->
            runCatching { json.decodeFromString<Household>(raw) }.getOrNull()
        }

    // ---- Current user ----
    suspend fun cacheUser(user: User) =
        storage.writeString(KEY_USER, json.encodeToString(user))

    suspend fun getCachedUser(): User? =
        storage.readString(KEY_USER)?.let { raw ->
            runCatching { json.decodeFromString<User>(raw) }.getOrNull()
        }

    suspend fun clear() = storage.clear()

    // A stale/corrupt cache entry (schema drift, partial write) must not brick an offline screen —
    // treat it as "no cache" so callers fall through to the original network error instead.
    private inline fun <reified T> String.decodeList(): List<T>? =
        runCatching { json.decodeFromString<List<T>>(this) }.getOrNull()

    companion object {
        private const val KEY_CATEGORIES = "cache_categories_v1"
        private const val KEY_TRANSACTIONS = "cache_transactions_v1"
        private const val KEY_DASHBOARD = "cache_dashboard_v1"
        private const val KEY_HOUSEHOLD = "cache_household_v1"
        private const val KEY_USER = "cache_user_v1"
    }
}
