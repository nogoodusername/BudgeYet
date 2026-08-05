package com.famex.feature.category.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors backend/app/schemas/category.py CategoryCreate. Sent as a JSON number even though the
// backend's monthly_limit is a Decimal — Pydantic's Decimal validator accepts a JSON number on
// the way in, it's only Decimal *responses* that serialize as strings (see CategoryResponseDto).
@Serializable
data class CategoryCreateRequestDto(
    val name: String,
    val icon: String,
    @SerialName("monthly_limit") val monthlyLimit: Double
)

// Mirrors backend/app/schemas/category.py CategoryUpdate — only the limit is ever edited via
// updateCategoryLimits (Category Limits screen's Save Changes).
@Serializable
data class CategoryUpdateRequestDto(@SerialName("monthly_limit") val monthlyLimit: Double)

// Mirrors backend/app/schemas/category.py CategoryResponse. monthly_limit is a Decimal field,
// which Pydantic serializes as a JSON *string* (e.g. "12.50", not 12.5) to preserve precision —
// confirmed against Pydantic v2's default Decimal encoder — so this is String, not Double;
// CategoryMappers.kt does the .toDouble() conversion.
@Serializable
data class CategoryResponseDto(
    val id: Long,
    val name: String,
    val icon: String,
    @SerialName("monthly_limit") val monthlyLimit: String
)
