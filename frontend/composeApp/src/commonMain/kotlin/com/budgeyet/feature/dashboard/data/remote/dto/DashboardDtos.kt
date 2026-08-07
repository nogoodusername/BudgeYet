package com.budgeyet.feature.dashboard.data.remote.dto

import com.budgeyet.core.network.dto.CategoryWithStatsDto
import com.budgeyet.core.network.dto.TransactionTypeDto
import com.budgeyet.core.network.dto.UserResponseDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors backend/app/schemas/budget.py BudgetWithStats, as embedded in DashboardResponse. Only
// decodes what the frontend's Budget model needs — remaining/percent_used/status are all derived
// client-side (see core/model/Budget.kt), same reasoning as CategoryWithStatsDto.
// monthly_goal_amount/spent are Decimal fields, which serialize as JSON strings (see
// core/network/dto/CategoryDto.kt for the same gotcha) — String, not Double.
@Serializable
data class DashboardBudgetDto(
    val id: Long,
    val name: String,
    @SerialName("monthly_goal_amount") val monthlyGoalAmount: String,
    val spent: String,
    val month: Int,
    val year: Int
)

// Mirrors backend/app/schemas/dashboard.py DashboardResponse.
@Serializable
data class DashboardResponseDto(
    @SerialName("has_budget") val hasBudget: Boolean,
    @SerialName("has_transactions") val hasTransactions: Boolean,
    val budget: DashboardBudgetDto? = null,
    val categories: List<CategoryWithStatsDto> = emptyList()
)

// Mirrors backend/app/schemas/dashboard.py ActivityFeedItem. created_by_user is the member who
// logged the transaction; paid_by_user is the member who actually paid. category_id is now also
// present so the UI can look up the live category's icon/color.
@Serializable
data class ActivityFeedItemDto(
    val id: Long,
    val type: TransactionTypeDto,
    val amount: String,
    val merchant: String,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("created_by_user") val createdByUser: UserResponseDto,
    @SerialName("paid_by_user") val paidByUser: UserResponseDto,
    @SerialName("transaction_date") val transactionDate: String,
    @SerialName("created_at") val createdAt: String
)

// Mirrors backend/app/schemas/common.py Page[ActivityFeedItem].
@Serializable
data class ActivityFeedPageDto(
    val items: List<ActivityFeedItemDto>,
    val total: Int,
    val limit: Int,
    val offset: Int
)
