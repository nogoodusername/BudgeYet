package com.budgeyet.feature.transaction.data.remote.dto

import com.budgeyet.core.network.dto.TransactionTypeDto
import com.budgeyet.core.network.dto.UserResponseDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PaymentModeDto {
    @SerialName("cash") CASH,
    @SerialName("card") CARD,
    @SerialName("bank_transfer") BANK_TRANSFER,
    @SerialName("other") OTHER
}

// Mirrors backend/app/schemas/transaction.py TransactionCreate. amount is sent as a JSON number
// even though the backend's Decimal field serializes back out as a string on responses (see
// TransactionResponseDto) — Pydantic's Decimal validator accepts a JSON number on the way in.
// transaction_date is always sent (backend defaults a missing one to "now" server-side, but the
// UI always has a selected date already — see AddTransactionUiState.dateText/selectedDate — so
// there's no case where omitting it is the more correct choice).
@Serializable
data class TransactionCreateRequestDto(
    val amount: Double,
    val merchant: String,
    val type: TransactionTypeDto,
    @SerialName("payment_mode") val paymentMode: PaymentModeDto,
    val notes: String? = null,
    @SerialName("transaction_date") val transactionDate: String,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("paid_by_id") val paidById: Long? = null
)

// Mirrors backend/app/schemas/transaction.py TransactionUpdate — all fields optional, only what
// changed needs to be set. RealTransactionRepository always sends the full transaction (there's
// no partial-edit UI), but reassignCategory sends only category_id.
@Serializable
data class TransactionUpdateRequestDto(
    val amount: Double? = null,
    val merchant: String? = null,
    val type: TransactionTypeDto? = null,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("paid_by_id") val paidById: Long? = null,
    @SerialName("payment_mode") val paymentMode: PaymentModeDto? = null,
    val notes: String? = null,
    @SerialName("transaction_date") val transactionDate: String? = null
)

// Nested category on TransactionResponse (backend/app/schemas/category.py CategoryResponse) —
// only decodes what Transaction.categoryName needs; monthly_limit is intentionally omitted (see
// CategoryDtos.kt for why it'd need to be a String, not a Double, if it were used here).
@Serializable
data class TransactionCategoryDto(val id: Long, val name: String)

// Mirrors backend/app/schemas/transaction.py TransactionResponse. amount is a Decimal field,
// which Pydantic serializes as a JSON string (see CategoryDtos.kt's CategoryResponseDto for the
// same gotcha, confirmed there against Pydantic v2's default encoder) — String, not Double;
// TransactionMappers.kt does the .toDouble() conversion.
@Serializable
data class TransactionResponseDto(
    val id: Long,
    val amount: String,
    val merchant: String,
    val type: TransactionTypeDto,
    @SerialName("payment_mode") val paymentMode: PaymentModeDto,
    val notes: String? = null,
    val category: TransactionCategoryDto? = null,
    @SerialName("paid_by_user") val paidByUser: UserResponseDto,
    @SerialName("transaction_date") val transactionDate: String,
    @SerialName("created_at") val createdAt: String
)

// Mirrors backend/app/schemas/common.py Page[TransactionResponse].
@Serializable
data class TransactionPageDto(
    val items: List<TransactionResponseDto>,
    val total: Int,
    val limit: Int,
    val offset: Int
)
