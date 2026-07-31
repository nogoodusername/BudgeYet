package com.famex.core.navigation

sealed interface Screen {
    data object Dashboard : Screen
    data object Categories : Screen
    data class CategoryDetail(val categoryId: Long) : Screen
    data object History : Screen
    data class TransactionDetail(val transactionId: Long) : Screen
    data object AddTransaction : Screen
    data object Profile : Screen
}
