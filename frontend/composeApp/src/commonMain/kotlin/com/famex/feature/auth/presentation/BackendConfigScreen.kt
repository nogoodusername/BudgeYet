package com.famex.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.famex.core.ui.fieldColors
import com.famex.theme.LocalFamExTypography

/**
 * Stitch "Backend Configuration" screen (bef77b03cd4240daba76886faf1e39ed) — reached from the
 * gear icon on the Sign In / Sign Up screen. PRD A0/Section 9.9: default to the hosted backend,
 * let the user point at a self-hosted deployment instead. In-memory only for now (see
 * BackendConfig) — no local persistence layer yet, so a saved custom URL doesn't survive a
 * cold start — but "Server Reachable" now pings the real DB-independent /api/v1/ping endpoint
 * (AuthRepository.checkServerReachable) rather than being a UI mock.
 */
@Composable
fun BackendConfigScreen(
    uiState: BackendConfigUiState,
    onSelectHosted: () -> Unit,
    onSelectCustom: () -> Unit,
    onCustomUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val famExType = LocalFamExTypography.current

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    text = "Fam-Ex connects to a secure hosted backend by default. For advanced control and privacy, you can configure the app to connect to your own self-hosted deployment.",
                    style = famExType.bodyMd,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Server Connection", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        BackendOptionCard(
            icon = Icons.Default.Cloud,
            title = "Hosted (Default)",
            description = "Recommended for most users",
            selected = uiState.selection == BackendConfigSelection.HOSTED,
            onClick = onSelectHosted
        )

        Spacer(modifier = Modifier.height(12.dp))

        BackendOptionCard(
            icon = Icons.Default.Dns,
            title = "Custom URL",
            description = "Connect to a self-hosted instance",
            selected = uiState.selection == BackendConfigSelection.CUSTOM,
            onClick = onSelectCustom
        )

        if (uiState.selection == BackendConfigSelection.CUSTOM) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Server URL", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.customUrl,
                onValueChange = onCustomUrlChange,
                placeholder = { Text(text = "https://your-server.example.com", style = famExType.bodyMd) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors()
            )
            ReachabilityIndicator(status = uiState.reachability)
        }

        uiState.saveError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, style = famExType.labelSm, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onSave,
            enabled = !uiState.isSaving && uiState.canSave,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = if (uiState.isSaving) "Saving…" else "Save & Continue", style = famExType.headlineSm)
        }
    }
}

@Composable
private fun ReachabilityIndicator(status: ReachabilityStatus, modifier: Modifier = Modifier) {
    if (status == ReachabilityStatus.IDLE) return
    val famExType = LocalFamExTypography.current

    val (icon, label, color) = when (status) {
        ReachabilityStatus.IDLE -> return
        ReachabilityStatus.INVALID -> Triple(Icons.AutoMirrored.Filled.HelpOutline, "Enter a valid URL (https://...)", MaterialTheme.colorScheme.onSurfaceVariant)
        ReachabilityStatus.CHECKING -> Triple(null, "Validating…", MaterialTheme.colorScheme.onSurfaceVariant)
        ReachabilityStatus.REACHABLE -> Triple(Icons.Default.CheckCircle, "Server Reachable", MaterialTheme.colorScheme.secondary)
        ReachabilityStatus.UNREACHABLE -> Triple(Icons.Default.ErrorOutline, "Server unreachable", MaterialTheme.colorScheme.error)
    }

    Row(
        modifier = modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        } else {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = color)
        }
        Text(text = label, style = famExType.labelSm, color = color)
    }
}

@Composable
private fun BackendOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val famExType = LocalFamExTypography.current
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                Text(text = description, style = famExType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
            )
        }
    }
}
