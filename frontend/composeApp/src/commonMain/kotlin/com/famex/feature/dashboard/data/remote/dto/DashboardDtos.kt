package com.famex.feature.dashboard.data.remote.dto

import com.famex.core.network.dto.CategoryWithStatsDto
import com.famex.core.network.dto.TransactionTypeDto
import com.famex.core.network.dto.UserResponseDto
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

// Mirrors backend/app/schemas/dashboard.py ActivityFeedItem. Note `user` here is
// created_by_user, not paid_by_user (see dashboard_controller._to_activity_item) — the backend's
// feed reports who *logged* the transaction, not who *paid*. DashboardMappers.kt maps it into
// Transaction.paidBy anyway since that's the only "who" field ActivityFeedRow renders and the two
// are the same person for the common case (someone logging their own expense); it'll only be
// wrong for the edge case of one member logging a transaction paid by another. amount is a
// Decimal field, same String-not-Double gotcha as everywhere else. There is no category_id here
// (only category_name) — see DashboardMappers.kt for what that costs the UI.
@Serializable
data class ActivityFeedItemDto(
    val id: Long,
    val type: TransactionTypeDto,
    val amount: String,
    val merchant: String,
    @SerialName("category_name") val categoryName: String? = null,
    val user: UserResponseDto,
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
