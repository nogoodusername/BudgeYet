package com.famex.feature.dashboard.domain.model

import com.famex.core.model.Budget
import com.famex.core.model.Category
import com.famex.core.model.Household
import com.famex.core.model.Transaction
import kotlinx.serialization.Serializable

@Serializable
data class DashboardData(
    val household: Household,
    val budget: Budget?,
    val categories: List<Category>,
    val activityFeed: List<Transaction>
)
