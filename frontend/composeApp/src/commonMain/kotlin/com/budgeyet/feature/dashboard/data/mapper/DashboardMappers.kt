package com.budgeyet.feature.dashboard.data.mapper

import com.budgeyet.core.model.Budget
import com.budgeyet.core.model.PaymentMode
import com.budgeyet.core.model.Transaction
import com.budgeyet.core.network.mapper.toDomain
import com.budgeyet.core.util.parseIsoDateTimeToLocalDate
import com.budgeyet.core.util.toDisplayText
import com.budgeyet.feature.dashboard.data.remote.dto.ActivityFeedItemDto
import com.budgeyet.feature.dashboard.data.remote.dto.DashboardBudgetDto

fun DashboardBudgetDto.toDomain(): Budget = Budget(
    id = id,
    name = name,
    monthlyGoalAmount = monthlyGoalAmount.toDouble(),
    spentAmount = spent.toDouble(),
    month = month,
    year = year
)

fun ActivityFeedItemDto.toDomain(): Transaction {
    val date = parseIsoDateTimeToLocalDate(transactionDate)
    return Transaction(
        id = id,
        merchant = merchant,
        amount = amount.toDouble(),
        type = type.toDomain(),
        paymentMode = PaymentMode.CARD,
        categoryId = categoryId,
        categoryName = categoryName,
        paidBy = paidByUser.toDomain(),
        notes = null,
        transactionDate = date,
        transactionDateText = date.toDisplayText(),
        createdAtText = parseIsoDateTimeToLocalDate(createdAt).toDisplayText()
    )
}
