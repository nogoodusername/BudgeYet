package com.famex.core.navigation

sealed interface Screen {
    data object Dashboard : Screen
    data object Categories : Screen
    data class CategoryDetail(val categoryId: Long) : Screen
    data object AddCategory : Screen
    data object BudgetSetup : Screen
    data object History : Screen
    data class TransactionDetail(val transactionId: Long) : Screen
    data class EditTransaction(val transactionId: Long) : Screen
    data object AddTransaction : Screen
    data object Profile : Screen
    data object HouseholdMembers : Screen
    data object InviteMember : Screen
}
