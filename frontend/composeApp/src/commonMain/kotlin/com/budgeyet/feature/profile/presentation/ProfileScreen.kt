package com.budgeyet.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.budgeyet.core.model.DisplayMode
import com.budgeyet.core.ui.fieldColors
import com.budgeyet.theme.BrandTeal
import com.budgeyet.theme.LocalBudgeYetTypography

private val currencyOptions = listOf(
    "USD" to "USD ($)",
    "EUR" to "EUR (€)",
    "GBP" to "GBP (£)",
    "INR" to "INR (₹)",
    "JPY" to "JPY (¥)",
    "CAD" to "CAD ($)",
    "AUD" to "AUD ($)",
    "CHF" to "CHF (Fr)",
    "CNY" to "CNY (¥)",
    "BRL" to "BRL (R$)",
    "KRW" to "KRW (₩)",
    "SEK" to "SEK (kr)",
)

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onFullNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onCurrencyChange: (String) -> Unit,
    onDisplayModeChange: (DisplayMode) -> Unit,
    onManageMembers: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading && uiState.user == null ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        uiState.errorMessage != null && uiState.user == null ->
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage)
            }

        uiState.user != null && uiState.household != null -> ProfileContent(
            uiState = uiState,
            onFullNameChange = onFullNameChange,
            onNicknameChange = onNicknameChange,
            onSaveProfile = onSaveProfile,
            onCurrencyChange = onCurrencyChange,
            onDisplayModeChange = onDisplayModeChange,
            onManageMembers = onManageMembers,
            onSignOutClick = onSignOutClick,
            modifier = modifier
        )
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onFullNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onCurrencyChange: (String) -> Unit,
    onDisplayModeChange: (DisplayMode) -> Unit,
    onManageMembers: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgeYetType = LocalBudgeYetTypography.current
    val user = uiState.user!!
    val household = uiState.household!!
    // Currency edits and Manage Members are Admin/Owner-only (PRD §5/E2) — hide the
    // whole Household Settings card from plain Members instead of letting them 403 after the
    // fact. Personal Settings (display mode) stays visible to everyone.
    val canManageHousehold = uiState.currentUserRole?.isAdminOrOwner == true

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            ProfileInfoCard(
                email = user.email,
                fullName = uiState.fullNameDraft,
                nickname = uiState.nicknameDraft,
                onFullNameChange = onFullNameChange,
                onNicknameChange = onNicknameChange,
                onSaveProfile = onSaveProfile,
                isSaving = uiState.isSavingProfile,
                hasUnsavedChanges = uiState.hasUnsavedNameChanges,
                saveError = uiState.saveError
            )
        }

        if (canManageHousehold) {
            item {
                SettingsCard(title = "Household Settings") {
                    ManageMembersRow(onClick = onManageMembers)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsDropdownRow(
                        icon = Icons.Default.Payments,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Primary Currency",
                        subtitle = "Used for all dashboard views",
                        options = currencyOptions,
                        selectedValue = household.currency,
                        onSelect = onCurrencyChange
                    )
                }
            }
        }

        item {
            SettingsCard(title = "Personal Settings") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsIconCircle(icon = Icons.Default.Palette, tint = MaterialTheme.colorScheme.onSurface)
                        Column {
                            Text(text = "Appearance", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Choose your preferred theme", style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    AppearancePicker(selected = user.displayMode, onSelect = onDisplayModeChange)
                }
            }
        }

        item {
            SignOutRow(onClick = onSignOutClick)
        }
    }
}

@Composable
private fun SignOutRow(onClick: () -> Unit) {
    val budgeYetType = LocalBudgeYetTypography.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
                Column {
                    Text(text = "Sign Out", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.error)
                    Text(
                        text = "Log out of your account on this device",
                        style = budgeYetType.labelSm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ProfileInfoCard(
    email: String,
    fullName: String,
    nickname: String,
    onFullNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    isSaving: Boolean,
    hasUnsavedChanges: Boolean,
    saveError: String?
) {
    val budgeYetType = LocalBudgeYetTypography.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(BrandTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = initialsOf(fullName), style = budgeYetType.displayAmount, color = BrandTeal)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledField(label = "Full Name") {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = onFullNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors()
                    )
                }
                LabeledField(label = "Nickname") {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = onNicknameChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors()
                    )
                }
                LabeledField(label = "Email (Read Only)") {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Button(
                onClick = onSaveProfile,
                enabled = !isSaving && hasUnsavedChanges,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = if (isSaving) "Saving…" else "Save Changes", style = budgeYetType.labelMd)
            }

            saveError?.let { error ->
                Text(text = error, style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    val budgeYetType = LocalBudgeYetTypography.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val budgeYetType = LocalBudgeYetTypography.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Text(
                text = title,
                style = budgeYetType.headlineSm,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(20.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
        }
    }
}

@Composable
private fun ManageMembersRow(onClick: () -> Unit) {
    val budgeYetType = LocalBudgeYetTypography.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
        Column {
            Text(text = "Manage Members", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "Invite family, promote admins, or remove members",
                style = budgeYetType.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsIconCircle(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdownRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    val budgeYetType = LocalBudgeYetTypography.current
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedValue }?.second ?: selectedValue

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            }
            Column {
                Text(text = title, style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

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
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearancePicker(selected: DisplayMode, onSelect: (DisplayMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppearanceOption(
            modifier = Modifier.weight(1f),
            label = "Light",
            icon = Icons.Default.LightMode,
            selected = selected == DisplayMode.LIGHT,
            onClick = { onSelect(DisplayMode.LIGHT) }
        )
        AppearanceOption(
            modifier = Modifier.weight(1f),
            label = "Dark",
            icon = Icons.Default.DarkMode,
            selected = selected == DisplayMode.DARK,
            onClick = { onSelect(DisplayMode.DARK) }
        )
        AppearanceOption(
            modifier = Modifier.weight(1f),
            label = "System",
            icon = Icons.Default.SettingsSuggest,
            selected = selected == DisplayMode.SYSTEM,
            onClick = { onSelect(DisplayMode.SYSTEM) }
        )
    }
}

@Composable
private fun AppearanceOption(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val budgeYetType = LocalBudgeYetTypography.current
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        Text(
            text = label,
            style = budgeYetType.labelSm,
            color = contentColor
        )
    }
}

private fun initialsOf(fullName: String): String {
    val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
