package com.budgeyet.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.budgeyet.core.ui.fieldColors
import com.budgeyet.theme.LocalBudgeYetTypography

/**
 * Stitch "Join Household" screen (e055704711404d32af9b657728ebe8f1). Reached from Household
 * Choice's "Join a Household" card.
 */
@Composable
fun JoinHouseholdScreen(
    uiState: JoinHouseholdUiState,
    onInviteCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgeYetType = LocalBudgeYetTypography.current

    // verticalScroll (rather than the weight(1f) spacer this used to pin the button to the
    // bottom with) so the button can scroll up above the keyboard; OnboardingRoute supplies
    // the keyboard-height bottom inset (keyboardAwarePadding) that makes the extra range real.
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(text = "Join a Household", style = budgeYetType.headlineLg, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Enter the invite code shared by your household administrator to start budgeting together.",
            style = budgeYetType.bodyMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Invite Code", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.inviteCode,
            onValueChange = onInviteCodeChange,
            placeholder = { Text(text = "e.g. ABC-123-XYZ", style = budgeYetType.bodyMd) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = fieldColors()
        )

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
                Column {
                    Text(text = "Shared Access", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "Once you join, you will see all shared transactions and the monthly budget.",
                        style = budgeYetType.bodyMd,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onJoin,
            enabled = !uiState.isJoining,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = if (uiState.isJoining) "Joining…" else "Join Household", style = budgeYetType.headlineSm)
        }
    }
}
