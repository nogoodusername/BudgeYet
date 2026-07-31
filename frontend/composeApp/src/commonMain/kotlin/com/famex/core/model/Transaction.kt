package com.famex.core.model

enum class TransactionType { EXPENSE, INCOME }
enum class PaymentMode { CASH, CARD, BANK_TRANSFER, OTHER }

data class Transaction(
    val id: Long,
    val merchant: String,
    val amount: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val paymentMode: PaymentMode = PaymentMode.CARD,
    val categoryId: Long?,
    val categoryName: String?,
    val paidBy: User,
    val notes: String? = null,
    val transactionDateText: String,
    val createdAtText: String
)
