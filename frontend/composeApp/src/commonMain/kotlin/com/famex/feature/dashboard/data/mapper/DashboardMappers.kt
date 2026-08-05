package com.famex.feature.dashboard.data.mapper

import com.famex.core.model.Budget
import com.famex.core.model.PaymentMode
import com.famex.core.model.Transaction
import com.famex.core.network.mapper.toDomain
import com.famex.core.util.parseIsoDateTimeToLocalDate
import com.famex.core.util.toDisplayText
import com.famex.feature.dashboard.data.remote.dto.ActivityFeedItemDto
import com.famex.feature.dashboard.data.remote.dto.DashboardBudgetDto

fun DashboardBudgetDto.toDomain(): Budget = Budget(
    id = id,
    name = name,
    monthlyGoalAmount = monthlyGoalAmount.toDouble(),
    spentAmount = spent.toDouble(),
    month = month,
    year = year
)

// See ActivityFeedItemDto's doc comment: `user` is created_by_user, not paid_by_user, and there's
// no category_id in this response at all — only category_name. categoryId is left null here,
// which costs ActivityFeedRow the category-specific icon/tint (it falls back to a default), but
// the merchant/amount/date/category-name text all still render correctly via categoryName.
fun ActivityFeedItemDto.toDomain(): Transaction {
    val date = parseIsoDateTimeToLocalDate(transactionDate)
    return Transaction(
        id = id,
        merchant = merchant,
        amount = amount.toDouble(),
        type = type.toDomain(),
        paymentMode = PaymentMode.CARD,
        categoryId = null,
        categoryName = categoryName,
        paidBy = user.toDomain(),
        notes = null,
        transactionDate = date,
        transactionDateText = date.toDisplayText(),
        createdAtText = parseIsoDateTimeToLocalDate(createdAt).toDisplayText()
    )
}
