package com.budgeyet.feature.auth.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgeyet.core.ui.fieldColors
import com.budgeyet.theme.LocalBudgeYetTypography

/**
 * Stitch "Forgot PIN" screen (ea92d4309dd943d18ff7162dbe4c6f29). Reached from the "Forgot PIN?"
 * link on the Log In tab.
 */
@Composable
fun ForgotPinScreen(
    uiState: ForgotPinUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgeYetType = LocalBudgeYetTypography.current

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(text = "Forgot PIN?", style = budgeYetType.headlineLg, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your email address and we'll send you a new 6-digit PIN to get back into your account.",
            style = budgeYetType.bodyMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Email Address", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            placeholder = { Text(text = "e.g. name@family.com", style = budgeYetType.bodyMd) },
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

        Button(
            onClick = onSubmit,
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = if (uiState.isSubmitting) "Sending…" else "Send New PIN", style = budgeYetType.headlineSm)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBackToSignIn, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Back to Sign In", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
