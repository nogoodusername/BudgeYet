package com.famex.core.model

import com.famex.core.util.todayLocalDate
import kotlinx.datetime.LocalDate

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
    // Structured date backing real filtering/grouping (History search + filters); the *Text
    // fields stay as human-friendly relative strings ("2d ago") used by Dashboard/Category
    // screens and aren't derived from this to avoid touching their already-shipped display.
    val transactionDate: LocalDate = todayLocalDate(),
    val transactionDateText: String,
    val createdAtText: String
)
