package com.budgeyet.feature.dashboard.domain.model

import com.budgeyet.core.model.Budget
import com.budgeyet.core.model.Category
import com.budgeyet.core.model.Household
import com.budgeyet.core.model.Transaction
import kotlinx.serialization.Serializable

@Serializable
data class DashboardData(
    val household: Household,
    val budget: Budget?,
    val categories: List<Category>,
    val activityFeed: List<Transaction>
)
