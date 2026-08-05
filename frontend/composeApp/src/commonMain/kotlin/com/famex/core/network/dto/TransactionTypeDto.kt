package com.famex.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors backend/app/models/transaction.py TransactionType. Shared across features (Transaction's
// create/update/response, Dashboard's activity feed) rather than duplicated.
@Serializable
enum class TransactionTypeDto {
    @SerialName("expense") EXPENSE,
    @SerialName("income") INCOME
}
