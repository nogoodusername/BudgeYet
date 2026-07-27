package com.famex

import androidx.compose.runtime.*
import com.famex.data.model.Category
import com.famex.data.model.HouseholdBudgetSummary
import com.famex.data.model.Transaction
import com.famex.theme.FamExTheme
import com.famex.ui.DashboardScreen

@Composable
fun App() {
    FamExTheme {
        // Sample data aligned with PRD specifications for initial scaffolding demo
        val sampleSummary = remember {
            HouseholdBudgetSummary(
                budgetName = "July 2026 Household Budget",
                totalGoalAmount = 3000.0,
                totalSpentAmount = 1840.0,
                currencySymbol = "$"
            )
        }

        val sampleCategories = remember {
            listOf(
                Category(id = 1, name = "Groceries", icon = "cart", monthlyLimit = 800.0, amountSpent = 620.0),
                Category(id = 2, name = "Dining Out", icon = "restaurant", monthlyLimit = 400.0, amountSpent = 380.0),
                Category(id = 3, name = "Utilities", icon = "flash", monthlyLimit = 350.0, amountSpent = 210.0),
                Category(id = 4, name = "Transportation", icon = "car", monthlyLimit = 300.0, amountSpent = 150.0),
            )
        }

        val sampleActivityFeed = remember {
            listOf(
                Transaction(id = 101, merchant = "Whole Foods Market", amount = 142.50, categoryName = "Groceries", paidByNickname = "Alex", dateText = "10m ago"),
                Transaction(id = 102, merchant = "Electricity Bill", amount = 110.00, categoryName = "Utilities", paidByNickname = "Sam", dateText = "2h ago"),
                Transaction(id = 103, merchant = "Starbucks", amount = 18.25, categoryName = "Dining Out", paidByNickname = "Alex", dateText = "5h ago"),
            )
        }

        DashboardScreen(
            summary = sampleSummary,
            categories = sampleCategories,
            activityFeed = sampleActivityFeed,
            onAddTransactionClick = {
                // FAB Click Handler
            }
        )
    }
}
