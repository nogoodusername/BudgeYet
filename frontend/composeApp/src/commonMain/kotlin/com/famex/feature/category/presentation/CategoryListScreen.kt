package com.famex.feature.category.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.famex.core.model.Category
import com.famex.core.ui.categoryIcon
import com.famex.core.ui.colorFor
import com.famex.core.ui.dashedBorder
import com.famex.core.util.currencySymbolFor
import com.famex.core.util.formatAmount
import com.famex.theme.LocalFamExTypography

@Composable
fun CategoryListScreen(
    uiState: CategoryListUiState,
    onLimitChange: (Long, String) -> Unit,
    onSplitEvenly: () -> Unit,
    onSaveChanges: () -> Unit,
    onRetry: () -> Unit,
    onCategoryClick: (Long) -> Unit = {},
    onAddCategory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading && uiState.categories.isEmpty() && uiState.errorMessage == null ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        uiState.errorMessage != null && uiState.categories.isEmpty() ->
            Column(
                modifier = modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(uiState.errorMessage)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }

        uiState.categories.isEmpty() ->
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No categories yet. Categories are set up when you create your household budget.")
            }

        else -> CategoryLimitsContent(
            uiState = uiState,
            onLimitChange = onLimitChange,
            onSplitEvenly = onSplitEvenly,
            onSaveChanges = onSaveChanges,
            onCategoryClick = onCategoryClick,
            onAddCategory = onAddCategory,
            modifier = modifier
        )
    }
}

@Composable
private fun CategoryLimitsContent(
    uiState: CategoryListUiState,
    onLimitChange: (Long, String) -> Unit,
    onSplitEvenly: () -> Unit,
    onSaveChanges: () -> Unit,
    onCategoryClick: (Long) -> Unit,
    onAddCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencySymbol = currencySymbolFor(uiState.currency)
    val famExType = LocalFamExTypography.current

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            BudgetSummaryCard(uiState = uiState, currencySymbol = currencySymbol, onSplitEvenly = onSplitEvenly)
        }

        item {
            Text(text = "Active Categories", style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
        }

        items(uiState.categories, key = { it.id }) { category ->
            CategoryLimitRow(
                category = category,
                currencySymbol = currencySymbol,
                draftValue = uiState.limitDrafts[category.id] ?: "",
                percentOfTotal = uiState.draftPercent(category),
                onValueChange = { onLimitChange(category.id, it) },
                onClick = { onCategoryClick(category.id) }
            )
        }

        item { AddCategoryButton(onClick = onAddCategory) }

        item {
            Button(
                onClick = onSaveChanges,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = if (uiState.isSaving) "Saving…" else "Save Changes", style = famExType.labelMd)
            }
        }

        uiState.saveError?.let { error ->
            item {
                Text(text = error, style = famExType.labelSm, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    uiState: CategoryListUiState,
    currencySymbol: String,
    onSplitEvenly: () -> Unit
) {
    val famExType = LocalFamExTypography.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Total Monthly Budget",
                        style = famExType.labelSm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatAmount(uiState.totalMonthlyBudget, currencySymbol),
                        style = famExType.headlineLg,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedButton(
                    onClick = onSplitEvenly,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Split evenly", style = famExType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            AllocationBar(categories = uiState.categories, fractionFor = { uiState.draftFraction(it) })

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${formatAmount(uiState.allocatedAmount, currencySymbol)} Allocated",
                    style = famExType.labelSm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (uiState.remainingAmount >= 0) "${formatAmount(uiState.remainingAmount, currencySymbol)} Remaining"
                    else "${formatAmount(-uiState.remainingAmount, currencySymbol)} Over",
                    style = famExType.labelSm,
                    color = if (uiState.remainingAmount >= 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AllocationBar(categories: List<Category>, fractionFor: (Category) -> Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        categories.forEach { category ->
            val fraction = fractionFor(category).coerceIn(0f, 1f)
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(fraction)
                        .fillMaxHeight()
                        .background(colorFor(category.status))
                )
            }
        }
    }
}

@Composable
private fun CategoryLimitRow(
    category: Category,
    currencySymbol: String,
    draftValue: String,
    percentOfTotal: Int,
    onValueChange: (String) -> Unit,
    onClick: () -> Unit
) {
    val famExType = LocalFamExTypography.current
    val statusColor = colorFor(category.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).clickable(onClick = onClick),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = categoryIcon(category.icon), contentDescription = category.name, tint = statusColor)
                }

                Column {
                    Text(text = category.name, style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "$percentOfTotal% of total budget",
                        style = famExType.bodyMd,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = draftValue,
                onValueChange = onValueChange,
                modifier = Modifier.width(112.dp),
                textStyle = famExType.headlineSm.copy(textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface),
                leadingIcon = { Text(text = currencySymbol, style = famExType.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}

@Composable
private fun AddCategoryButton(onClick: () -> Unit) {
    val famExType = LocalFamExTypography.current
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .dashedBorder(color = outlineVariant, strokeWidth = 1.dp, cornerRadius = 12.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Category",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Add Category", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
