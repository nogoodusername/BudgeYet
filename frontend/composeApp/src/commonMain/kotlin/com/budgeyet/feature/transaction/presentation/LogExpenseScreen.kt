package com.budgeyet.feature.transaction.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgeyet.core.model.PaymentMode
import com.budgeyet.core.model.TransactionType
import com.budgeyet.core.ui.AmountEntryCard
import com.budgeyet.core.ui.CategoryFieldCard
import com.budgeyet.core.ui.DateFieldCard
import com.budgeyet.core.ui.NotesFieldCard
import com.budgeyet.core.ui.PaidByFieldCard
import com.budgeyet.core.ui.PaymentModeFieldCard
import com.budgeyet.core.ui.TextFieldCard
import com.budgeyet.core.ui.TransactionTypeToggle
import com.budgeyet.core.util.currencySymbolFor
import com.budgeyet.theme.LocalBudgeYetTypography

// This one screen doubles as both the "Log Expense" and "Log Income" Stitch designs — they're
// ~95% identical (segmented type toggle drives which fields show), so a single AddTransaction
// flow with a type toggle covers both rather than duplicating a near-identical screen/controller.
@Composable
fun LogExpenseScreen(
    uiState: AddTransactionUiState,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onOpenCategoryPicker: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onPaidByChange: (Long) -> Unit,
    onPaymentModeChange: (PaymentMode) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.categories.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val budgeYetType = LocalBudgeYetTypography.current
    val isExpense = uiState.isExpense

    LazyColumn(
        modifier = modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { TransactionTypeToggle(selected = uiState.type, onSelect = onTypeChange) }

        item {
            AmountEntryCard(
                amountText = uiState.amountText,
                onAmountChange = onAmountChange,
                currencySymbol = currencySymbolFor(uiState.currency)
            )
        }

        item {
            TextFieldCard(
                label = if (isExpense) "Merchant / Description" else "Source / Description",
                value = uiState.merchant,
                onValueChange = onMerchantChange,
                placeholder = if (isExpense) "e.g. Whole Foods" else "e.g. Salary, Freelance"
            )
        }

        if (isExpense) {
            item {
                CategoryFieldCard(categoryName = uiState.selectedCategory?.name, onClick = onOpenCategoryPicker)
            }
        }

        item { DateFieldCard(dateText = uiState.dateText, onClick = onOpenDatePicker) }

        item {
            PaidByFieldCard(
                label = if (isExpense) "Who Paid" else "Paid By",
                members = uiState.householdMembers,
                selectedMemberId = uiState.paidByMemberId,
                onSelect = onPaidByChange
            )
        }

        item { PaymentModeFieldCard(selected = uiState.paymentMode, onSelect = onPaymentModeChange) }

        item { NotesFieldCard(value = uiState.notes, onValueChange = onNotesChange) }

        item {
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        uiState.isSaving -> "Saving…"
                        isExpense -> "Save Transaction"
                        else -> "Save Income"
                    },
                    style = budgeYetType.headlineSm
                )
            }
        }

        uiState.saveError?.let { error ->
            item {
                Text(text = error, style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
