package com.budgeyet.feature.profile.presentation

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.budgeyet.core.ui.fieldColors
import com.budgeyet.theme.LocalBudgeYetTypography

/**
 * Stitch "Invite Options" screen (d7b9b90eabd5428ea745981e91cb6c73). Drops the current-members
 * list the earlier "Invite Members (Refined)" mockup showed here — that's now covered by the
 * pending-invite cards on HouseholdMembersScreen, so this page focuses purely on the two ways
 * to invite: email or join code. "Send Invite" creates a pending invite rather than adding the
 * member directly (see FakeProfileRepository.inviteMember). The join code's expiry line mirrors
 * the backend's Invite.expires_at (INVITE_EXPIRY_DAYS = 7) via Household.joinCodeExpiresAt.
 */
@Composable
fun InviteMemberScreen(
    uiState: InviteMemberUiState,
    onEmailChange: (String) -> Unit,
    onSendInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgeYetType = LocalBudgeYetTypography.current

    when {
        uiState.isLoading && uiState.household == null ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        uiState.errorMessage != null && uiState.household == null ->
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage, style = budgeYetType.bodyLg)
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
                        Text(text = "Invite Member", style = budgeYetType.headlineLg, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Grow your household. Invite someone to join the ${household.name} plan.",
                            style = budgeYetType.bodyMd,
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
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Invite via Email", style = budgeYetType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "Email Address", style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = uiState.emailDraft,
                                    onValueChange = onEmailChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text(text = "colleague@example.com", style = budgeYetType.bodyMd) },
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
                                Text(text = if (uiState.isSending) "Sending…" else "Send Invite", style = budgeYetType.labelMd)
                            }
                            uiState.sendError?.let { error ->
                                Text(text = error, style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.error)
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
                            Text(text = "Share Join Code", style = budgeYetType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Your Invite Code", style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        text = uiState.joinCode ?: "Generating…",
                                        style = budgeYetType.headlineMd.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    enabled = uiState.joinCode != null,
                                    onClick = { uiState.joinCode?.let { clipboardManager.setText(AnnotatedString(it)) } },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy code", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            uiState.joinCodeExpiryText?.let { expiryText ->
                                Text(
                                    text = "For security, this unique join code will expire automatically on $expiryText.",
                                    style = budgeYetType.labelSm,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
