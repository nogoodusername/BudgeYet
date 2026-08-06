package com.famex.feature.transaction.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.famex.core.model.PaymentMode
import com.famex.core.ui.AmountEntryCard
import com.famex.core.ui.CategoryFieldCard
import com.famex.core.ui.DateFieldCard
import com.famex.core.ui.NotesFieldCard
import com.famex.core.ui.PaidByFieldCard
import com.famex.core.ui.PaymentModeFieldCard
import com.famex.core.ui.TextFieldCard
import com.famex.core.util.currencySymbolFor
import com.famex.theme.LocalFamExTypography

@Composable
fun EditTransactionScreen(
    uiState: EditTransactionUiState,
    onAmountChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onOpenCategoryPicker: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onPaidByChange: (Long) -> Unit,
    onPaymentModeChange: (PaymentMode) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val famExType = LocalFamExTypography.current

    when {
        uiState.isLoading && uiState.categories.isEmpty() ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        uiState.notFound ->
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Transaction not found", style = famExType.bodyLg)
            }

        else -> {
            val isExpense = uiState.isExpense

            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    AmountEntryCard(
                        amountText = uiState.amountText,
                        onAmountChange = onAmountChange,
                        currencySymbol = currencySymbolFor(uiState.currency),
                        label = "Amount"
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
                        label = "Who Paid",
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
                        enabled = !uiState.isSaving && !uiState.isDeleting,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (uiState.isSaving) "Saving…" else "Save Changes", style = famExType.headlineSm)
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = onDelete, enabled = !uiState.isSaving && !uiState.isDeleting) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isDeleting) "Deleting…" else "Delete Transaction",
                                style = famExType.labelMd,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                uiState.saveError?.let { error ->
                    item {
                        Text(text = error, style = famExType.labelSm, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
