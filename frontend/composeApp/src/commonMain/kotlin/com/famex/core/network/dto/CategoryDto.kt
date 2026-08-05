package com.famex.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors backend/app/schemas/category.py CategoryWithStats. Shared across features (Category's
// own list/detail, Dashboard's category snapshots) rather than duplicated — both need the exact
// same shape. Only decodes the fields the frontend's Category model actually needs —
// remaining/percent_used/status are all derived client-side from monthlyLimit/amountSpent (see
// core/model/Category.kt), so they're omitted here rather than decoded and discarded.
@Serializable
data class CategoryWithStatsDto(
    val id: Long,
    val name: String,
    val icon: String,
    @SerialName("monthly_limit") val monthlyLimit: String,
    val spent: String
)
