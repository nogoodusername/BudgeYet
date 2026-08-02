package com.famex.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.famex.core.model.Household
import com.famex.core.model.HouseholdMember
import com.famex.core.model.MemberRole
import com.famex.theme.BrandTeal
import com.famex.theme.LocalFamExTypography

/**
 * Stitch "Member Management" (33944542fd5f4da3ac3c26eb5f93d93a) plus its "Admin Menu" variant
 * (36f95bafde3a4c72a195ac31e842664c), which generalized the action menu to every non-Owner
 * row: Members get Promote to Admin, Admins get Promote to Owner / Demote to Member, and both
 * get Remove — all backed by role-specific confirmation dialogs. Owner has no self-action menu
 * (its card style/email subtitle intentionally kept from the original screen rather than the
 * Admin-Menu mockup's simpler avatar-photo layout, since only the menu itself changed here).
 */
@Composable
fun HouseholdMembersScreen(
    uiState: HouseholdMembersUiState,
    onRequestRoleChange: (HouseholdMember, MemberRole) -> Unit,
    onCancelRoleChange: () -> Unit,
    onConfirmRoleChange: () -> Unit,
    onRequestRemove: (HouseholdMember) -> Unit,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    onNavigateToInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val famExType = LocalFamExTypography.current

    when {
        uiState.isLoading && uiState.household == null ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        uiState.errorMessage != null && uiState.household == null ->
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage, style = famExType.bodyLg)
            }

        uiState.household != null -> {
            val household = uiState.household
            val atCapacity = household.members.size >= Household.MAX_MEMBERS

            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
                item {
                    Text(
                        text = "Manage family access and roles for your shared finances.",
                        style = famExType.bodyMd,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            household.members.forEachIndexed { index, member ->
                                MemberRow(
                                    member = member,
                                    onRoleChange = { newRole -> onRequestRoleChange(member, newRole) },
                                    onRemove = { onRequestRemove(member) }
                                )
                                if (index != household.members.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                }
                            }
                        }
                    }
                }

                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onNavigateToInvite,
                            enabled = !atCapacity,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (atCapacity) "Household is full (${household.members.size}/${Household.MAX_MEMBERS})" else "Invite Member",
                                style = famExType.labelMd
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Role Privileges", style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                RolePrivilegeRow(
                                    icon = Icons.Default.Star,
                                    title = "Owner",
                                    description = "Full control, including transferring ownership to another Admin."
                                )
                                RolePrivilegeRow(
                                    icon = Icons.Default.Shield,
                                    title = "Admins",
                                    description = "Can manage members, change budget limits, and view all account logs."
                                )
                                RolePrivilegeRow(
                                    icon = Icons.Default.Person,
                                    title = "Members",
                                    description = "Can log transactions and view shared goal progress."
                                )
                            }
                        }
                    }
                }
            }

            uiState.pendingRoleChange?.let { request ->
                when (request.newRole) {
                    MemberRole.ADMIN -> PromoteToAdminDialog(
                        memberName = request.member.user.nickname,
                        isProcessing = uiState.isProcessing,
                        errorMessage = uiState.actionError,
                        onConfirm = onConfirmRoleChange,
                        onDismiss = onCancelRoleChange
                    )
                    MemberRole.OWNER -> PromoteToOwnerDialog(
                        memberName = request.member.user.nickname,
                        currentOwnerName = household.members.find { it.role == MemberRole.OWNER }?.user?.nickname,
                        isProcessing = uiState.isProcessing,
                        errorMessage = uiState.actionError,
                        onConfirm = onConfirmRoleChange,
                        onDismiss = onCancelRoleChange
                    )
                    MemberRole.MEMBER -> DemoteToMemberDialog(
                        memberName = request.member.user.nickname,
                        isProcessing = uiState.isProcessing,
                        errorMessage = uiState.actionError,
                        onConfirm = onConfirmRoleChange,
                        onDismiss = onCancelRoleChange
                    )
                }
            }

            uiState.pendingRemoveMember?.let { member ->
                RemoveMemberDialog(
                    memberName = member.user.nickname,
                    householdName = household.name,
                    isProcessing = uiState.isProcessing,
                    errorMessage = uiState.actionError,
                    onConfirm = onConfirmRemove,
                    onDismiss = onCancelRemove
                )
            }
        }
    }
}

@Composable
private fun MemberRow(member: HouseholdMember, onRoleChange: (MemberRole) -> Unit, onRemove: () -> Unit) {
    val famExType = LocalFamExTypography.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(BrandTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = member.user.nickname.take(1).uppercase(), style = famExType.headlineSm, color = BrandTeal)
                }
                val badgeIcon = when (member.role) {
                    MemberRole.OWNER -> Icons.Default.Star
                    MemberRole.ADMIN -> Icons.Default.Shield
                    MemberRole.MEMBER -> null
                }
                if (badgeIcon != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (member.role == MemberRole.OWNER) BrandTeal else MaterialTheme.colorScheme.onSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = member.user.nickname, style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                    RoleBadge(role = member.role)
                }
                Text(text = member.user.email, style = famExType.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Owner has no self-action menu — its status only changes when someone else is
        // promoted to Owner (an automatic ownership transfer, see updateMemberRole).
        if (member.role != MemberRole.OWNER) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Member actions", tint = MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (member.role == MemberRole.ADMIN) {
                        DropdownMenuItem(
                            text = { Text("Promote to Owner") },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                            onClick = { menuExpanded = false; onRoleChange(MemberRole.OWNER) }
                        )
                        DropdownMenuItem(
                            text = { Text("Demote to Member") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            onClick = { menuExpanded = false; onRoleChange(MemberRole.MEMBER) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Promote to Admin") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                            onClick = { menuExpanded = false; onRoleChange(MemberRole.ADMIN) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Remove from Household", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onRemove() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(role: MemberRole) {
    val famExType = LocalFamExTypography.current
    val containerColor: Color
    val contentColor: Color
    val label: String
    when (role) {
        MemberRole.OWNER -> {
            containerColor = BrandTeal
            contentColor = Color.White
            label = "OWNER"
        }
        MemberRole.ADMIN -> {
            containerColor = MaterialTheme.colorScheme.onSurface
            contentColor = MaterialTheme.colorScheme.surface
            label = "ADMIN"
        }
        MemberRole.MEMBER -> {
            containerColor = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            label = "MEMBER"
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, style = famExType.labelSm.copy(fontSize = 10.sp), color = contentColor)
    }
}

@Composable
private fun RolePrivilegeRow(icon: ImageVector, title: String, description: String) {
    val famExType = LocalFamExTypography.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        Column {
            Text(text = title, style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, style = famExType.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConfirmationDialogShell(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    headline: String,
    body: String,
    confirmLabel: String,
    confirmLabelBusy: String,
    isProcessing: Boolean,
    errorMessage: String?,
    confirmContainerColor: Color,
    confirmContentColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    val famExType = LocalFamExTypography.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = headline, style = famExType.headlineMd, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = body, style = famExType.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

                extraContent?.let {
                    Spacer(modifier = Modifier.height(20.dp))
                    it()
                }

                Spacer(modifier = Modifier.height(20.dp))

                errorMessage?.let {
                    Text(text = it, style = famExType.labelSm, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onConfirm,
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = confirmContainerColor, contentColor = confirmContentColor)
                    ) {
                        Text(text = if (isProcessing) confirmLabelBusy else confirmLabel, style = famExType.headlineSm)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Cancel", style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoteToAdminDialog(
    memberName: String,
    isProcessing: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialogShell(
        icon = Icons.Default.AdminPanelSettings,
        iconTint = BrandTeal,
        iconBackground = BrandTeal.copy(alpha = 0.15f),
        headline = "Promote to Admin?",
        body = "You are about to upgrade $memberName from a Member to a Household Admin.",
        confirmLabel = "Promote to Admin",
        confirmLabelBusy = "Promoting…",
        isProcessing = isProcessing,
        errorMessage = errorMessage,
        confirmContainerColor = MaterialTheme.colorScheme.onSurface,
        confirmContentColor = MaterialTheme.colorScheme.surface,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        extraContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    RolePrivilegeRow(Icons.Default.Group, "Manage Members", "Invite new family members and edit existing roles.")
                    RolePrivilegeRow(Icons.Default.TrackChanges, "Budget Controls", "Define monthly spending limits and category goals.")
                    RolePrivilegeRow(Icons.Default.Assessment, "Financial Reporting", "Access high-level summaries and expense exports.")
                }
            }
        }
    )
}

@Composable
private fun PromoteToOwnerDialog(
    memberName: String,
    currentOwnerName: String?,
    isProcessing: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val body = if (currentOwnerName != null) {
        "$memberName will become the new Owner, and $currentOwnerName will be demoted to Admin. Only one member can hold the Owner role at a time."
    } else {
        "$memberName will become the Household Owner."
    }
    ConfirmationDialogShell(
        icon = Icons.Default.Star,
        iconTint = BrandTeal,
        iconBackground = BrandTeal.copy(alpha = 0.15f),
        headline = "Transfer Ownership?",
        body = body,
        confirmLabel = "Transfer Ownership",
        confirmLabelBusy = "Transferring…",
        isProcessing = isProcessing,
        errorMessage = errorMessage,
        confirmContainerColor = MaterialTheme.colorScheme.onSurface,
        confirmContentColor = MaterialTheme.colorScheme.surface,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun DemoteToMemberDialog(
    memberName: String,
    isProcessing: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialogShell(
        icon = Icons.Default.Person,
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        iconBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        headline = "Demote to Member?",
        body = "$memberName will lose Admin privileges and won't be able to manage members, change budget limits, or view account logs.",
        confirmLabel = "Demote to Member",
        confirmLabelBusy = "Demoting…",
        isProcessing = isProcessing,
        errorMessage = errorMessage,
        confirmContainerColor = MaterialTheme.colorScheme.onSurface,
        confirmContentColor = MaterialTheme.colorScheme.surface,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun RemoveMemberDialog(
    memberName: String,
    householdName: String,
    isProcessing: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialogShell(
        icon = Icons.Default.PersonRemove,
        iconTint = MaterialTheme.colorScheme.error,
        iconBackground = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
        headline = "Remove Member?",
        body = "Are you sure you want to remove $memberName from the $householdName household? " +
            "They will lose access to all shared budgets and transaction history immediately.",
        confirmLabel = "Remove Member",
        confirmLabelBusy = "Removing…",
        isProcessing = isProcessing,
        errorMessage = errorMessage,
        confirmContainerColor = MaterialTheme.colorScheme.error,
        confirmContentColor = MaterialTheme.colorScheme.onError,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
