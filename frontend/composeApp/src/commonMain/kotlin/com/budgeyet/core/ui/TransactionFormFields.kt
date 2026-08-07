package com.budgeyet.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.pow
import com.budgeyet.core.model.HouseholdMember
import com.budgeyet.core.model.PaymentMode
import com.budgeyet.core.model.TransactionType
import com.budgeyet.core.util.epochMillisToLocalDate
import com.budgeyet.core.util.toUtcEpochMillis
import com.budgeyet.theme.BrandTealLight
import com.budgeyet.theme.LocalBudgeYetTypography
import kotlinx.datetime.LocalDate

private val Color.isDark: Boolean
    get() = {
        val r = red
        val g = green
        val b = blue
        val linearize = { c: Float ->
            if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
        }
        (0.2126f * linearize(r) + 0.7152f * linearize(g) + 0.0722f * linearize(b)) < 0.5f
    }()

// Shared building blocks for the Log Expense / Log Income / Edit Transaction screens — all
// three are visual "variations" of the same aligned form from the Stitch export (segmented
// type toggle, light amount card, bordered field cards, Payment Mode segmented row, Notes).

@Composable
fun TransactionTypeToggle(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        TypeToggleOption(
            modifier = Modifier.weight(1f),
            label = "Expense",
            selected = selected == TransactionType.EXPENSE,
            onClick = { onSelect(TransactionType.EXPENSE) }
        )
        TypeToggleOption(
            modifier = Modifier.weight(1f),
            label = "Income",
            selected = selected == TransactionType.INCOME,
            onClick = { onSelect(TransactionType.INCOME) }
        )
    }
}

@Composable
private fun TypeToggleOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val budgeYetType = LocalBudgeYetTypography.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (selected) Modifier.background(MaterialTheme.colorScheme.surface) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = budgeYetType.labelMd,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun AmountEntryCard(
    amountText: String,
    onAmountChange: (String) -> Unit,
    currencySymbol: String,
    label: String = "Enter Amount"
) {
    val budgeYetType = LocalBudgeYetTypography.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                style = budgeYetType.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = currencySymbol, style = budgeYetType.headlineMd, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(4.dp))
                BasicTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    textStyle = budgeYetType.displayAmount.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(160.dp)
                )
            }
        }
    }
}

// Every field below renders as its own bordered "card" — the aligned Stitch design gives each
// form row its own surface rather than grouping them inside one big card.
@Composable
fun FormFieldCard(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val budgeYetType = LocalBudgeYetTypography.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = label, style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.secondary,
    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.background,
    unfocusedContainerColor = MaterialTheme.colorScheme.background
)

@Composable
fun TextFieldCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val budgeYetType = LocalBudgeYetTypography.current
    FormFieldCard(label = label, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, style = budgeYetType.bodyMd) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = fieldColors()
        )
    }
}

@Composable
fun NotesFieldCard(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Add any additional details...",
    modifier: Modifier = Modifier
) {
    val budgeYetType = LocalBudgeYetTypography.current
    FormFieldCard(label = "Notes", modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, style = budgeYetType.bodyMd) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = fieldColors()
        )
    }
}

// A clickable trigger row styled like the surrounding OutlinedTextFields — tapping opens the
// full-screen SelectCategoryScreen dialog rather than an inline dropdown.
@Composable
fun CategoryFieldCard(categoryName: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FormFieldCard(label = "Category", modifier = modifier) {
        TriggerField(
            text = categoryName ?: "Select Category",
            placeholder = categoryName == null,
            trailingIcon = Icons.Default.ExpandMore,
            onClick = onClick
        )
    }
}

// A read-only, clickable field styled like the surrounding OutlinedTextFields — plain text
// input doesn't make sense here, tapping opens the DatePicker dialog instead.
@Composable
fun DateFieldCard(dateText: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FormFieldCard(label = "Date", modifier = modifier) {
        TriggerField(
            text = dateText,
            placeholder = false,
            leadingIcon = Icons.Default.CalendarToday,
            onClick = onClick
        )
    }
}

@Composable
fun TriggerField(
    text: String,
    placeholder: Boolean,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    val budgeYetType = LocalBudgeYetTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = text,
            style = budgeYetType.bodyMd,
            color = if (placeholder) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailingIcon != null) {
            Icon(imageVector = trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaidByFieldCard(
    label: String,
    members: List<HouseholdMember>,
    selectedMemberId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = members.find { it.id == selectedMemberId }?.user?.nickname ?: "Select"

    FormFieldCard(label = label, modifier = modifier) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                members.forEach { member ->
                    DropdownMenuItem(
                        text = { Text(member.user.nickname) },
                        onClick = {
                            onSelect(member.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentModeFieldCard(selected: PaymentMode, onSelect: (PaymentMode) -> Unit, modifier: Modifier = Modifier) {
    FormFieldCard(label = "Payment Mode", modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentModeOption(
                modifier = Modifier.weight(1f),
                label = "Card",
                icon = Icons.Default.CreditCard,
                selected = selected == PaymentMode.CARD,
                onClick = { onSelect(PaymentMode.CARD) }
            )
            PaymentModeOption(
                modifier = Modifier.weight(1f),
                label = "Cash",
                icon = Icons.Default.Payments,
                selected = selected == PaymentMode.CASH,
                onClick = { onSelect(PaymentMode.CASH) }
            )
            PaymentModeOption(
                modifier = Modifier.weight(1f),
                label = "Transfer",
                icon = Icons.Default.SyncAlt,
                selected = selected == PaymentMode.BANK_TRANSFER,
                onClick = { onSelect(PaymentMode.BANK_TRANSFER) }
            )
        }
    }
}

@Composable
private fun PaymentModeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgeYetType = LocalBudgeYetTypography.current
    val isDark = MaterialTheme.colorScheme.surface.isDark
    val selectedAccent = if (isDark) BrandTealLight else MaterialTheme.colorScheme.secondary
    val borderColor = if (selected) selectedAccent else MaterialTheme.colorScheme.outlineVariant
    val contentColor = if (selected) selectedAccent else MaterialTheme.colorScheme.onSurfaceVariant
    val containerColor = if (selected) selectedAccent.copy(alpha = 0.12f) else Color.Transparent

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.height(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = budgeYetType.labelMd, color = contentColor)
    }
}

// androidx.compose.material3.DatePickerDialog is Android-only (actual, not commonMain) in this
// Compose Multiplatform version — DatePicker/rememberDatePickerState are common, so this wraps
// them in a plain Dialog to keep the picker working on iOS too.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDatePickerDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.toUtcEpochMillis())

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column {
                DatePicker(state = datePickerState, showModeToggle = false)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { onConfirm(epochMillisToLocalDate(it)) }
                        },
                        enabled = datePickerState.selectedDateMillis != null
                    ) { Text("OK") }
                }
            }
        }
    }
}
