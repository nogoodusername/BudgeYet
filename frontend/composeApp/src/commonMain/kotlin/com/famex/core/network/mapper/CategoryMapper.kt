package com.famex.core.network.mapper

import com.famex.core.model.Category
import com.famex.core.network.dto.CategoryWithStatsDto

fun CategoryWithStatsDto.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    monthlyLimit = monthlyLimit.toDouble(),
    amountSpent = spent.toDouble()
)
