package com.budgeyet.core.network.mapper

import com.budgeyet.core.model.Category
import com.budgeyet.core.network.dto.CategoryWithStatsDto

fun CategoryWithStatsDto.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    monthlyLimit = monthlyLimit.toDouble(),
    amountSpent = spent.toDouble()
)
