package com.budgeyet.feature.auth.presentation

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.budgeyet.generated.resources.Res
import com.budgeyet.generated.resources.budge_yet_logo
import com.budgeyet.theme.LocalBudgeYetTypography
import org.jetbrains.compose.resources.painterResource

/**
 * Stitch "Welcome: Visibility (Text-Only)" screen (c319a67b8ac548c69ce6e4d5dce57e65) — the
 * first screen a signed-out user sees. No top bar; the onboarding funnel starts here.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgeYetType = LocalBudgeYetTypography.current

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Image(
            painter = painterResource(Res.drawable.budge_yet_logo),
            contentDescription = null,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to BudgeYet",
            style = budgeYetType.headlineLg,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "The single source of truth for your household finances. Achieve stability through shared visibility, collaborative tracking, and clear goals.",
            style = budgeYetType.bodyMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        WelcomeFeatureRow(
            icon = Icons.Default.Visibility,
            title = "Shared Visibility",
            description = "Eliminate money guesswork with real-time shared feeds."
        )
        Spacer(modifier = Modifier.height(20.dp))
        WelcomeFeatureRow(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            title = "Track Progress",
            description = "Celebrate positive habits with high-contrast progress indicators."
        )
        Spacer(modifier = Modifier.height(20.dp))
        WelcomeFeatureRow(
            icon = Icons.Default.Security,
            title = "Secure Vault",
            description = "Your household data is stable and protected."
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = "Get Started", style = budgeYetType.headlineSm)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Already have an account?", style = budgeYetType.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onLogIn) {
                Text(text = "Log In", style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun WelcomeFeatureRow(icon: ImageVector, title: String, description: String) {
    val budgeYetType = LocalBudgeYetTypography.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
        Column {
            Text(text = title, style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, style = budgeYetType.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
