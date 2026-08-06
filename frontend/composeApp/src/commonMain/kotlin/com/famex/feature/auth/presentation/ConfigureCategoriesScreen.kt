package com.famex.feature.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.famex.core.ui.categoryIcon
import com.famex.core.ui.fieldColors
import com.famex.theme.LocalFamExTypography

/**
 * Stitch "Configure Categories" screen (dd8dc00469d04f438a1ccb320f1e1ae5). Reached after
 * Budget Goal's "Next" — "Automated Distribution" splits the goal amount evenly across
 * whichever categories are checked; "Finish Setup" completes onboarding.
 */
@Composable
fun ConfigureCategoriesScreen(
    uiState: ConfigureCategoriesUiState,
    onToggleCategory: (String) -> Unit,
    onLimitChange: (String, String) -> Unit,
    onCustomNameChange: (String, String) -> Unit,
    onAutoDistributeToggle: () -> Unit,
    onAddCustomCategory: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val famExType = LocalFamExTypography.current

    // imePadding on the root shrinks the space available to the weighted LazyColumn below
    // when the keyboard opens, pushing the fixed "Finish Setup" footer button above it — the
    // list already scrolls its own items, so it doesn't need verticalScroll like the other
    // onboarding screens.
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "STEP 2 OF 3", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Category Limits", style = famExType.labelMd, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(2f / 3f).clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Automated Distribution", style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "Divide total budget target evenly among selected categories",
                        style = famExType.bodyMd,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.autoDistribute,
                    onCheckedChange = { onAutoDistributeToggle() },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.secondary)
                )
            }
        }

        uiState.error?.let { error ->
            Text(
                text = error,
                style = famExType.labelSm,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.categories, key = { it.key }) { category ->
                CategoryConfigCard(
                    category = category,
                    onToggle = { onToggleCategory(category.key) },
                    onLimitChange = { onLimitChange(category.key, it) },
                    onNameChange = { onCustomNameChange(category.key, it) }
                )
            }
            item {
                TextButton(onClick = onAddCustomCategory) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Add custom category", style = famExType.labelMd, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
                onClick = onFinish,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = if (uiState.isSaving) "Finishing…" else "Finish Setup", style = famExType.headlineSm)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
            }
        }
    }
}

@Composable
private fun CategoryConfigCard(
    category: ConfigureCategoryItem,
    onToggle: () -> Unit,
    onLimitChange: (String) -> Unit,
    onNameChange: (String) -> Unit
) {
    val famExType = LocalFamExTypography.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (category.isSelected) 2.dp else 1.dp,
            color = if (category.isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = categoryIcon(category.icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column {
                        if (category.isCustom) {
                            OutlinedTextField(
                                value = category.name,
                                onValueChange = onNameChange,
                                placeholder = { Text(text = "Category name", style = famExType.bodyMd) },
                                singleLine = true,
                                modifier = Modifier.width(180.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = fieldColors()
                            )
                        } else {
                            Text(text = category.name, style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(text = category.description, style = famExType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Checkbox(
                    checked = category.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
            }

            AnimatedVisibility(visible = category.isSelected, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(text = "Monthly Limit", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = category.monthlyLimitText,
                        onValueChange = onLimitChange,
                        placeholder = { Text(text = "0.00", style = famExType.bodyMd) },
                        leadingIcon = { Text(text = "$", style = famExType.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors()
                    )
                }
            }
        }
    }
}
