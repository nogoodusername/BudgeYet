package com.budgeyet.core.model

import com.budgeyet.core.util.todayLocalDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.serializers.LocalDateIso8601Serializer
import kotlinx.serialization.Serializable

enum class TransactionType { EXPENSE, INCOME }
enum class PaymentMode { CASH, CARD, BANK_TRANSFER, OTHER }

// @Serializable so Transaction round-trips through the offline cache + write queue
// (core/cache + core/offline). clientId is null for server-confirmed transactions and a
// non-null UUID for transactions created offline that are still waiting to sync — it's the
// only signal that distinguishes a pending row from a confirmed one (isPending).
@Serializable
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
    val clientId: String? = null,
    // Structured date backing real filtering/grouping (History search + filters); the *Text
    // fields stay as human-friendly relative strings ("2d ago") used by Dashboard/Category
    // screens and aren't derived from this to avoid touching their already-shipped display.
    @Serializable(with = LocalDateIso8601Serializer::class)
    val transactionDate: LocalDate = todayLocalDate(),
    val transactionDateText: String,
    val createdAtText: String
) {
    // True while this transaction exists only in the offline write queue (created offline,
    // not yet acknowledged by the server). Pending transactions carry a negative temp id and
    // a clientId; once sync confirms them they're replaced by the server's copy (positive id,
    // clientId = null).
    val isPending: Boolean get() = clientId != null
}
