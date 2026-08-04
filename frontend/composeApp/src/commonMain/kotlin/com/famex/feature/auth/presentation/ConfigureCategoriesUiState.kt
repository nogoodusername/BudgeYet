package com.famex.feature.auth.presentation

data class ConfigureCategoryItem(
    val key: String,
    val name: String,
    val description: String,
    val icon: String,
    val isSelected: Boolean = false,
    val monthlyLimitText: String = "",
    val isCustom: Boolean = false
)

// The 6 starter categories from the Stitch "Configure Categories" mockup — a sensible default
// household budget breakdown. Users can uncheck any of these and/or add their own via "Add
// custom category"; only checked rows are sent to AuthRepository.setupCategories on Finish.
val starterConfigureCategories: List<ConfigureCategoryItem> = listOf(
    ConfigureCategoryItem(key = "groceries", name = "Groceries", description = "Food, household supplies", icon = "cart"),
    ConfigureCategoryItem(key = "housing", name = "Housing", description = "Rent, mortgage, insurance", icon = "home"),
    ConfigureCategoryItem(key = "transportation", name = "Transportation", description = "Fuel, transit, maintenance", icon = "car"),
    ConfigureCategoryItem(key = "utilities", name = "Utilities", description = "Electricity, water, internet", icon = "flash"),
    ConfigureCategoryItem(key = "dining", name = "Dining Out", description = "Restaurants, cafes, delivery", icon = "restaurant"),
    ConfigureCategoryItem(key = "entertainment", name = "Entertainment", description = "Movies, hobbies, events", icon = "movie")
)

data class ConfigureCategoriesUiState(
    val monthlyGoalAmount: Double,
    val categories: List<ConfigureCategoryItem> = starterConfigureCategories,
    val autoDistribute: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
