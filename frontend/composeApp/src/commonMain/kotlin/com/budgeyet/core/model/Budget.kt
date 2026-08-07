package com.budgeyet.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: Long,
    val name: String,
    val monthlyGoalAmount: Double,
    val spentAmount: Double,
    val month: Int,
    val year: Int
) {
    val remainingAmount: Double get() = monthlyGoalAmount - spentAmount
    val percentUsed: Float get() = if (monthlyGoalAmount > 0) (spentAmount / monthlyGoalAmount).toFloat() else 0f
    val status: SpendStatus get() = spendStatusFor(percentUsed)
}
