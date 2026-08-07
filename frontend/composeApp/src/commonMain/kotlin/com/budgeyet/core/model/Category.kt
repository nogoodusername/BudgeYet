package com.budgeyet.core.model

import kotlinx.serialization.Serializable

// Category limits reset every cycle with no rollover — historical spend for prior cycles
// must still be queryable by date range once real networking lands.
@Serializable
data class Category(
    val id: Long,
    val name: String,
    val icon: String,
    val monthlyLimit: Double,
    val amountSpent: Double = 0.0
) {
    val remainingAmount: Double get() = monthlyLimit - amountSpent
    val percentUsed: Float get() = if (monthlyLimit > 0) (amountSpent / monthlyLimit).toFloat() else 0f
    val status: SpendStatus get() = spendStatusFor(percentUsed)
}
