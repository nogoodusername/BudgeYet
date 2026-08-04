package com.famex.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.famex.core.navigation.PinSentContext
import com.famex.theme.LocalFamExTypography

/**
 * Stitch "PIN Sent Confirmation" screen (e37a8090b453475fb182330fec704a93). Reused for both the
 * post-signup ("check your email for your PIN") and forgot-PIN ("a new PIN has been sent")
 * flows — same layout, different copy per [PinSentContext].
 */
@Composable
fun PinSentScreen(
    uiState: PinSentUiState,
    onGoToSignIn: () -> Unit,
    onResend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val famExType = LocalFamExTypography.current

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.MarkEmailRead, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "PIN Sent", style = famExType.headlineLg, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (uiState.context) {
                PinSentContext.SIGN_UP -> "We've created your account and emailed you a 6-digit PIN. Check your inbox (and spam folder) to sign in."
                PinSentContext.FORGOT_PIN -> "If an account exists for this email, a new 6-digit PIN has been sent. Please check your inbox (and spam folder)."
            },
            style = famExType.bodyMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "RECIPIENT EMAIL", style = famExType.labelSm, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = maskEmail(uiState.email), style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGoToSignIn,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = "Go to Sign In", style = famExType.headlineSm)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Didn't receive the email? Wait a few minutes before trying again.",
            style = famExType.labelSm,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onResend, enabled = !uiState.isResending) {
            Text(
                text = when {
                    uiState.isResending -> "Resending…"
                    uiState.justResent -> "Sent again!"
                    else -> "Resend Code"
                },
                style = famExType.labelMd,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        uiState.resendError?.let { error ->
            Text(text = error, style = famExType.labelSm, color = MaterialTheme.colorScheme.error)
        }
    }
}

// "j***n@example.com" — keeps the first/last local-part character visible, matching the
// Stitch mockup's masking, without exposing the full address.
private fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    if (at <= 1) return email
    val local = email.substring(0, at)
    val domain = email.substring(at)
    return "${local.first()}***${local.last()}$domain"
}
