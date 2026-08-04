package com.famex.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.famex.core.model.Transaction
import com.famex.core.model.TransactionType
import com.famex.core.util.formatAmount
import com.famex.theme.BrandCoral
import com.famex.theme.BrandTeal

@Composable
fun TransactionRow(
    transaction: Transaction,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val cardModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier

    Card(
        modifier = cardModifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InitialsAvatar(name = transaction.paidBy.nickname)
                Column {
                    Text(
                        text = transaction.merchant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${transaction.paidBy.nickname} • ${transaction.categoryName ?: "Uncategorized"} • ${transaction.transactionDateText}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = "${if (isExpense) "-" else "+"}${formatAmount(transaction.amount, currencySymbol)}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isExpense) BrandCoral else BrandTeal
            )
        }
    }
}
