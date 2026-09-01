package com.budgeyet.feature.category.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.budgeyet.core.ui.TextFieldCard
import com.budgeyet.core.ui.categoryIcon
import com.budgeyet.core.ui.categoryIconChoices
import com.budgeyet.core.ui.categoryIconGridPreviewCount
import com.budgeyet.core.ui.dismissKeyboardOptions
import com.budgeyet.core.ui.fieldColors
import com.budgeyet.core.util.currencySymbolFor
import com.budgeyet.core.util.formatAmount
import com.budgeyet.theme.LocalBudgeYetTypography

/**
 * Stitch "Add Category Form" screen (58da6754f9344e67881ef3920c1bc16f). Reached from the
 * Category Limits list's "Add Category" placeholder. The Stitch export's own TopAppBar and
 * BottomNavBar are dropped — App.kt's shared Scaffold already renders both.
 */
@Composable
fun AddCategoryScreen(
    uiState: AddCategoryUiState,
    onNameChange: (String) -> Unit,
    onMonthlyLimitChange: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onSeeAllIcons: () -> Unit,
    onDismissIconPicker: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgeYetType = LocalBudgeYetTypography.current
    val currencySymbol = currencySymbolFor(uiState.currency)

    if (uiState.isIconPickerOpen) {
        IconPickerSheet(
            selectedIcon = uiState.selectedIcon,
            onIconSelected = onIconSelected,
            onDismiss = onDismissIconPicker
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            TextFieldCard(
                label = "Category Name",
                value = uiState.name,
                onValueChange = onNameChange,
                placeholder = "e.g. Groceries, Entertainment"
            )
        }

        item {
            MonthlyLimitFieldCard(
                value = uiState.monthlyLimitText,
                currencySymbol = currencySymbol,
                onValueChange = onMonthlyLimitChange
            )
        }

        item {
            IconSelectionCard(
                selectedIcon = uiState.selectedIcon,
                onIconSelected = onIconSelected,
                onSeeAllIcons = onSeeAllIcons
            )
        }

        item {
            PreviewCard(
                iconKey = uiState.selectedIcon,
                name = uiState.previewName,
                limitText = "Budget: ${formatAmount(uiState.previewLimit, currencySymbol)}"
            )
        }

        item {
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
                Text(text = if (uiState.isSaving) "Adding…" else "Add Category", style = budgeYetType.headlineSm)
            }
        }

        uiState.saveError?.let { error ->
            item {
                Text(text = error, style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun MonthlyLimitFieldCard(value: String, currencySymbol: String, onValueChange: (String) -> Unit) {
    val budgeYetType = LocalBudgeYetTypography.current

    // Numeric keypad with an IME "Done" that hides the keyboard (see core/ui/KeyboardDismiss.kt).
    val (limitOptions, limitActions) = dismissKeyboardOptions()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Monthly Limit", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(text = "0.00", style = budgeYetType.bodyMd) },
                leadingIcon = { Text(text = currencySymbol, style = budgeYetType.headlineSm, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                keyboardOptions = limitOptions,
                keyboardActions = limitActions,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors()
            )
        }
    }
}

@Composable
private fun IconSelectionCard(selectedIcon: String, onIconSelected: (String) -> Unit, onSeeAllIcons: () -> Unit) {
    val budgeYetType = LocalBudgeYetTypography.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Select Icon", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Only the first categoryIconGridPreviewCount icons render inline (as plain rows
            // of 5, not a LazyVerticalGrid — a hardcoded grid height previously clipped icons
            // whenever the actual cell width came out taller than the guess; square
            // (aspectRatio 1f) row items size themselves correctly at any width instead).
            // The full set — which can grow freely — lives behind "See all icons" so this
            // card's height never scales with categoryIconChoices' size.
            categoryIconChoices.take(categoryIconGridPreviewCount).chunked(5).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { iconKey ->
                        IconGridItem(
                            iconKey = iconKey,
                            selected = iconKey == selectedIcon,
                            onClick = { onIconSelected(iconKey) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(5 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }

            if (categoryIconChoices.size > categoryIconGridPreviewCount) {
                TextButton(onClick = onSeeAllIcons, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "See all icons", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
internal fun IconGridItem(iconKey: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    val containerColor = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = categoryIcon(iconKey), contentDescription = iconKey, tint = contentColor)
    }
}

@Composable
private fun PreviewCard(iconKey: String, name: String, limitText: String) {
    val budgeYetType = LocalBudgeYetTypography.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = categoryIcon(iconKey), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
            }
            Column {
                Text(text = name, style = budgeYetType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                Text(text = limitText, style = budgeYetType.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
