package com.budgeyet.feature.transaction.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.budgeyet.core.model.HouseholdMember
import com.budgeyet.core.model.PaymentMode
import com.budgeyet.core.ui.InitialsAvatar
import com.budgeyet.core.ui.paymentModeIcon
import com.budgeyet.core.ui.paymentModeLabel
import com.budgeyet.core.util.toDisplayText
import com.budgeyet.theme.LocalBudgeYetTypography
import kotlinx.datetime.LocalDate

/**
 * Stitch "Transaction Filters Overlay" screen (081cad226e364e769476a1c32108f7d0) — a
 * bottom-anchored sheet rather than Material3's ModalBottomSheet, matching how
 * TransactionDatePickerDialog already wraps a plain Dialog for iOS portability.
 */
@Composable
fun TransactionFilterSheet(
    uiState: HistoryUiState,
    onDismiss: () -> Unit,
    onSelectDateRangeFilter: (DateRangeFilter) -> Unit,
    onTogglePayer: (Long) -> Unit,
    onTogglePaymentMode: (PaymentMode) -> Unit,
    onOpenCustomStartPicker: () -> Unit,
    onOpenCustomEndPicker: () -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    val budgeYetType = LocalBudgeYetTypography.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f), contentAlignment = Alignment.BottomCenter) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(width = 48.dp, height = 5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Filters", style = budgeYetType.headlineLg, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close filters")
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(text = "Date Range", style = budgeYetType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DateRangeChip(
                                        label = "This Month",
                                        selected = uiState.dateRangeFilter == DateRangeFilter.THIS_MONTH,
                                        onClick = { onSelectDateRangeFilter(DateRangeFilter.THIS_MONTH) }
                                    )
                                    DateRangeChip(
                                        label = "Last Month",
                                        selected = uiState.dateRangeFilter == DateRangeFilter.LAST_MONTH,
                                        onClick = { onSelectDateRangeFilter(DateRangeFilter.LAST_MONTH) }
                                    )
                                    DateRangeChip(
                                        label = "Custom",
                                        selected = uiState.dateRangeFilter == DateRangeFilter.CUSTOM,
                                        onClick = { onSelectDateRangeFilter(DateRangeFilter.CUSTOM) }
                                    )
                                }
                                if (uiState.dateRangeFilter == DateRangeFilter.CUSTOM) {
                                    CustomDateRangeRow(
                                        startDate = uiState.customStartDate,
                                        endDate = uiState.customEndDate,
                                        onStartClick = onOpenCustomStartPicker,
                                        onEndClick = onOpenCustomEndPicker
                                    )
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(text = "Household Member", style = budgeYetType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(uiState.householdMembers, key = { it.id }) { member ->
                                        MemberChip(
                                            member = member,
                                            selected = member.user.id in uiState.selectedPayerUserIds,
                                            onClick = { onTogglePayer(member.user.id) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(text = "Payment Mode", style = budgeYetType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PaymentMode.entries.chunked(2).forEach { rowModes ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            rowModes.forEach { mode ->
                                                PaymentModeCheckRow(
                                                    mode = mode,
                                                    checked = mode in uiState.selectedPaymentModes,
                                                    onClick = { onTogglePaymentMode(mode) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (rowModes.size == 1) Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onApply,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(text = "Apply Filters", style = budgeYetType.headlineSm)
                        }
                        TextButton(onClick = onReset) {
                            Text(text = "Reset", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateRangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val budgeYetType = LocalBudgeYetTypography.current
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
    val containerColor = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text = label, style = budgeYetType.labelMd, color = contentColor)
    }
}

@Composable
private fun CustomDateRangeRow(startDate: LocalDate?, endDate: LocalDate?, onStartClick: () -> Unit, onEndClick: () -> Unit) {
    val budgeYetType = LocalBudgeYetTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "START DATE", style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = startDate?.toDisplayText() ?: "Select",
                style = budgeYetType.bodyMd,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable(onClick = onStartClick)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "END DATE", style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = endDate?.toDisplayText() ?: "Select",
                style = budgeYetType.bodyMd,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable(onClick = onEndClick)
            )
        }
    }
}

@Composable
private fun MemberChip(member: HouseholdMember, selected: Boolean, onClick: () -> Unit) {
    val budgeYetType = LocalBudgeYetTypography.current
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(end = 12.dp, top = 4.dp, bottom = 4.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InitialsAvatar(name = member.user.nickname)
        Text(
            text = member.user.nickname,
            style = budgeYetType.labelMd,
            color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PaymentModeCheckRow(mode: PaymentMode, checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val budgeYetType = LocalBudgeYetTypography.current
    val borderColor = if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = paymentModeIcon(mode), contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = paymentModeLabel(mode), style = budgeYetType.bodyMd, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.background)
                .border(1.dp, if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(12.dp))
            }
        }
    }
}
