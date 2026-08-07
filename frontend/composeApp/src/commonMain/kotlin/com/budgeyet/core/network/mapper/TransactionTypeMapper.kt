package com.budgeyet.core.network.mapper

import com.budgeyet.core.model.TransactionType
import com.budgeyet.core.network.dto.TransactionTypeDto

fun TransactionTypeDto.toDomain(): TransactionType = when (this) {
    TransactionTypeDto.EXPENSE -> TransactionType.EXPENSE
    TransactionTypeDto.INCOME -> TransactionType.INCOME
}

fun TransactionType.toDto(): TransactionTypeDto = when (this) {
    TransactionType.EXPENSE -> TransactionTypeDto.EXPENSE
    TransactionType.INCOME -> TransactionTypeDto.INCOME
}
