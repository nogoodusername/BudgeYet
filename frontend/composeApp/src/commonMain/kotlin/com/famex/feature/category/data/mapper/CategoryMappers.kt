package com.famex.feature.category.data.mapper

import com.famex.core.model.Category
import com.famex.feature.category.data.remote.dto.CategoryResponseDto

// A freshly created category has no spend yet — CategoryResponse (unlike the shared
// CategoryWithStatsDto, see core/network/dto/CategoryDto.kt + core/network/mapper/CategoryMapper.kt)
// doesn't return one either way.
fun CategoryResponseDto.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    monthlyLimit = monthlyLimit.toDouble(),
    amountSpent = 0.0
)
