package com.budgeyet.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.budgeyet.core.model.Category
import com.budgeyet.core.model.Transaction
import com.budgeyet.core.model.TransactionType
import com.budgeyet.core.util.formatAmount
import com.budgeyet.theme.BrandTeal
import com.budgeyet.theme.LocalBudgeYetTypography

// One line of the "Family Activity" feed — a sentence ("Dad paid $45 for Groceries")
// rather than a standalone amount row, matching the Stitch dashboard design. `category`
// is the live category this transaction belongs to (looked up by the caller from
// DashboardData.categories) so the icon and its tint reflect the category's *current*
// spend status rather than a static per-transaction snapshot.
@Composable
fun ActivityFeedRow(
    transaction: Transaction,
    category: Category?,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val verb = if (isIncome) "added" else "paid"
    val preposition = if (isIncome) "to" else "for"
    val target = category?.name ?: transaction.categoryName ?: transaction.merchant

    val iconTint = category?.let { colorFor(it.status) }
        ?: if (isIncome) BrandTeal else MaterialTheme.colorScheme.onSurfaceVariant
    val iconVector = categoryIcon(category?.icon ?: if (isIncome) "savings" else "")
    val budgeYetType = LocalBudgeYetTypography.current

    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InitialsAvatar(name = transaction.paidBy.nickname)

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(transaction.paidBy.nickname) }
                    append(" $verb ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formatAmount(transaction.amount, currencySymbol)) }
                    append(" $preposition ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(target) }
                },
                style = budgeYetType.bodyMd,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = transaction.transactionDateText,
                style = budgeYetType.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = iconVector,
            contentDescription = target,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}
