package com.budgeyet.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.budgeyet.core.model.Category
import com.budgeyet.theme.LocalBudgeYetTypography

/**
 * Stitch "Delete Category Confirmation" screen (6d8acb47ddd24dd68be312b560932f2e). When the
 * category has existing transactions, PRD C1 requires reassigning them before deleting —
 * blocking delete otherwise — so this dialog only enables the confirm action once a reassign
 * target is picked. A category with no transactions skips the reassign step entirely.
 */
@Composable
fun DeleteCategoryDialog(
    categoryName: String,
    transactionCount: Int,
    reassignOptions: List<Category>,
    selectedReassignTargetId: Long?,
    isDeleting: Boolean,
    errorMessage: String?,
    onReassignTargetSelected: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val budgeYetType = LocalBudgeYetTypography.current
    val hasTransactions = transactionCount > 0
    val confirmEnabled = !isDeleting && (!hasTransactions || selectedReassignTargetId != null)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Delete $categoryName?",
                            style = budgeYetType.headlineSm,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasTransactions) {
                                buildAnnotatedString {
                                    append("This category currently has ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) {
                                        append("$transactionCount transaction${if (transactionCount == 1) "" else "s"}")
                                    }
                                    append(" assigned to it.")
                                }
                            } else {
                                buildAnnotatedString { append("This will permanently remove it from your budget.") }
                            },
                            style = budgeYetType.bodyMd,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (hasTransactions) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Reassign transactions to...",
                        style = budgeYetType.labelMd,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ReassignCategoryDropdown(
                        options = reassignOptions,
                        selectedId = selectedReassignTargetId,
                        onSelect = onReassignTargetSelected
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Transactions must be reassigned before deleting to maintain your budget history.",
                            style = budgeYetType.labelSm,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = it, style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isDeleting -> "Deleting…"
                                hasTransactions -> "Delete and Reassign"
                                else -> "Delete"
                            },
                            style = budgeYetType.headlineSm
                        )
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isDeleting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Cancel", style = budgeYetType.headlineSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReassignCategoryDropdown(
    options: List<Category>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    val budgeYetType = LocalBudgeYetTypography.current
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.id == selectedId }?.name ?: "Select a category"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { Icon(imageVector = Icons.Default.ExpandMore, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            textStyle = budgeYetType.bodyMd,
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = fieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
