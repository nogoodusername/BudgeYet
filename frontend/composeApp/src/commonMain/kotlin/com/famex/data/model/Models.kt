package com.famex.data.model

data class User(
    val id: Long,
    val email: String,
    val fullName: String,
    val nickname: String
)

data class Category(
    val id: Long,
    val name: String,
    val icon: String,
    val monthlyLimit: Double,
    val amountSpent: Double = 0.0
) {
    val remainingBalance: Double get() = monthlyLimit - amountSpent
    val percentageUtilized: Float get() = if (monthlyLimit > 0) (amountSpent / monthlyLimit).toFloat() else 0f
}

data class Transaction(
    val id: Long,
    val merchant: String,
    val amount: Double,
    val categoryName: String,
    val paidByNickname: String,
    val dateText: String,
    val isExpense: Boolean = true
)

data class HouseholdBudgetSummary(
    val budgetName: String,
    val totalGoalAmount: Double,
    val totalSpentAmount: Double,
    val currencySymbol: String = "$"
) {
    val remainingAmount: Double get() = totalGoalAmount - totalSpentAmount
    val percentageUtilized: Float get() = if (totalGoalAmount > 0) (totalSpentAmount / totalGoalAmount).toFloat() else 0f
}
