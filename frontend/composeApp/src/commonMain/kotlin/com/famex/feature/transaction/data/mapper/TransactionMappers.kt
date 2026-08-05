package com.famex.feature.transaction.data.mapper

import com.famex.core.model.PaymentMode
import com.famex.core.model.Transaction
import com.famex.core.network.mapper.toDomain
import com.famex.core.util.parseIsoDateTimeToLocalDate
import com.famex.core.util.toDisplayText
import com.famex.feature.transaction.data.remote.dto.PaymentModeDto
import com.famex.feature.transaction.data.remote.dto.TransactionResponseDto

fun PaymentModeDto.toDomain(): PaymentMode = when (this) {
    PaymentModeDto.CASH -> PaymentMode.CASH
    PaymentModeDto.CARD -> PaymentMode.CARD
    PaymentModeDto.BANK_TRANSFER -> PaymentMode.BANK_TRANSFER
    PaymentModeDto.OTHER -> PaymentMode.OTHER
}

fun PaymentMode.toDto(): PaymentModeDto = when (this) {
    PaymentMode.CASH -> PaymentModeDto.CASH
    PaymentMode.CARD -> PaymentModeDto.CARD
    PaymentMode.BANK_TRANSFER -> PaymentModeDto.BANK_TRANSFER
    PaymentMode.OTHER -> PaymentModeDto.OTHER
}

fun TransactionResponseDto.toDomain(): Transaction {
    val date = parseIsoDateTimeToLocalDate(transactionDate)
    return Transaction(
        id = id,
        merchant = merchant,
        amount = amount.toDouble(),
        type = type.toDomain(),
        paymentMode = paymentMode.toDomain(),
        categoryId = category?.id,
        categoryName = category?.name,
        paidBy = paidByUser.toDomain(),
        notes = notes,
        transactionDate = date,
        transactionDateText = date.toDisplayText(),
        createdAtText = parseIsoDateTimeToLocalDate(createdAt).toDisplayText()
    )
}
