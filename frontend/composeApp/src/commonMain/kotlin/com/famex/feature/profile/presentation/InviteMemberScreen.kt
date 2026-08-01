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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.famex.core.model.Household
import com.famex.core.model.MemberRole
import com.famex.core.ui.fieldColors
import com.famex.theme.BrandTeal
import com.famex.theme.LocalFamExTypography

/**
 * Stitch "Invite Members (Refined)" screen (5d56f98a6c91489c84b91abdac6a680a). No pending-invite
 * state is modeled anywhere in this app, so "Send Invite" adds the member directly rather than
 * simulating an acceptance flow — and the mockup's "Invites expire in 7 days" line is dropped
 * since nothing here tracks an expiry.
 */
@Composable
fun InviteMemberScreen(
    uiState: InviteMemberUiState,
    onEmailChange: (String) -> Unit,
    onSendInvite: () -> Unit,
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
            val clipboardManager = LocalClipboardManager.current

            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
                item {
                    Column {
                        Text(text = "Grow Your Household", style = famExType.headlineLg, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Invite partners, roommates, or family members to share expenses.",
                            style = famExType.bodyMd,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Household Members (${household.members.size}/${Household.MAX_MEMBERS})",
                                style = famExType.headlineSm,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            household.members.forEachIndexed { index, member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandTeal.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = member.user.nickname.take(1).uppercase(), style = famExType.labelMd, color = BrandTeal)
                                    }
                                    Column {
                                        Text(text = member.user.nickname, style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                                        Text(
                                            text = if (member.role == MemberRole.ADMIN) "Admin" else "Member",
                                            style = famExType.labelSm,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (index != household.members.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Invite via Email", style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "Email Address", style = famExType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = uiState.emailDraft,
                                    onValueChange = onEmailChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text(text = "colleague@example.com", style = famExType.bodyMd) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = fieldColors()
                                )
                            }
                            Button(
                                onClick = onSendInvite,
                                enabled = !uiState.isSending,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(text = if (uiState.isSending) "Sending…" else "Send Invite", style = famExType.labelMd)
                            }
                            uiState.sendError?.let { error ->
                                Text(text = error, style = famExType.labelSm, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Share Join Code", style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Your Invite Code", style = famExType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = uiState.joinCode,
                                        style = famExType.headlineMd.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { clipboardManager.setText(AnnotatedString(uiState.joinCode)) },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy code", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
