package com.famex.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.famex.core.ui.fieldColors
import com.famex.theme.LocalFamExTypography

/**
 * Stitch "Set Up Budget Goal" screen (b76e7d33a8824548a1877e7683199da9). Reached after Create
 * Household succeeds — "Next" carries the goal amount forward to Configure Categories, "Skip
 * for now" finishes onboarding without a budget (can be set up later from Settings).
 */
@Composable
fun BudgetGoalScreen(
    uiState: BudgetGoalUiState,
    onBudgetNameChange: (String) -> Unit,
    onBudgetPeriodChange: (String) -> Unit,
    onGoalAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val famExType = LocalFamExTypography.current

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Build Your Blueprint",
            style = famExType.headlineMd,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Set your monthly targets to keep the family finances in perfect harmony.",
            style = famExType.bodyMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "Budget Name", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.budgetName,
                    onValueChange = onBudgetNameChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Budget Period", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.budgetPeriod,
                    onValueChange = onBudgetPeriodChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Monthly Goal Amount", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.monthlyGoalAmountText,
                    onValueChange = onGoalAmountChange,
                    placeholder = { Text(text = "0.00", style = famExType.bodyLg) },
                    leadingIcon = { Text(text = "$", style = famExType.bodyLg, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "Most households find success by planning their budget at the start of every month.",
                        style = famExType.labelSm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, style = famExType.labelSm, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onSave,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = if (uiState.isSaving) "Saving…" else "Next: Configure Categories", style = famExType.headlineSm)
        }

        TextButton(onClick = onSkip, enabled = !uiState.isSaving) {
            Text(text = "Skip for now", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
