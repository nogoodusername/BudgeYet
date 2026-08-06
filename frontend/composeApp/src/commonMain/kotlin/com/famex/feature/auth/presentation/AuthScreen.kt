package com.famex.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.famex.core.navigation.AuthTab
import com.famex.core.ui.PinInputField
import com.famex.core.ui.fieldColors
import com.famex.theme.LocalFamExTypography

/**
 * Stitch "Sign In (Fixed)" (3a07c803e1d44a8095ef999513223e99) and "Sign Up"
 * (b734818294eb4ed2bcc66ca9c0b388d6) screens, combined into one tabbed screen as both mockups
 * share the same header/tab-switcher chrome. Sign Up's "Create 6-Digit PIN" field (plus a
 * Confirm PIN field to catch typos) is user-chosen — the backend takes it as-is
 * (`UserCreate.pin`) rather than generating and emailing one, so signup no longer needs an
 * email round-trip before the account is usable; see `AuthController.onSignUp`, which logs the
 * user straight in with the PIN they just typed.
 */
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onTabChange: (AuthTab) -> Unit,
    onLoginEmailChange: (String) -> Unit,
    onLoginPinChange: (String) -> Unit,
    onLogin: () -> Unit,
    onForgotPin: () -> Unit,
    onSignUpFullNameChange: (String) -> Unit,
    onSignUpNicknameChange: (String) -> Unit,
    onSignUpEmailChange: (String) -> Unit,
    onSignUpPinChange: (String) -> Unit,
    onSignUpPinConfirmChange: (String) -> Unit,
    onSignUp: () -> Unit,
    onOpenBackendConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val famExType = LocalFamExTypography.current

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Fam-Ex", style = famExType.headlineMd, color = MaterialTheme.colorScheme.onSurface)
            IconButton(onClick = onOpenBackendConfig) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Backend Configuration")
            }
        }

        Column(
            // imePadding() after verticalScroll extends the scrollable range by the keyboard's
            // height, so fields/buttons near the bottom of the form (Confirm PIN, Create
            // Account) can be scrolled up above the keyboard instead of staying pinned behind
            // it — iOS has no windowSoftInputMode equivalent to resize the view automatically.
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthTabSwitcher(selected = uiState.tab, onTabChange = onTabChange)

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.tab == AuthTab.LOG_IN) {
                LogInForm(
                    uiState = uiState,
                    onLoginEmailChange = onLoginEmailChange,
                    onLoginPinChange = onLoginPinChange,
                    onLogin = onLogin,
                    onForgotPin = onForgotPin
                )
            } else {
                SignUpForm(
                    uiState = uiState,
                    onSignUpFullNameChange = onSignUpFullNameChange,
                    onSignUpNicknameChange = onSignUpNicknameChange,
                    onSignUpEmailChange = onSignUpEmailChange,
                    onSignUpPinChange = onSignUpPinChange,
                    onSignUpPinConfirmChange = onSignUpPinConfirmChange,
                    onSignUp = onSignUp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AuthTabSwitcher(selected: AuthTab, onTabChange: (AuthTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        AuthTabOption(
            modifier = Modifier.weight(1f),
            label = "Log In",
            selected = selected == AuthTab.LOG_IN,
            onClick = { onTabChange(AuthTab.LOG_IN) }
        )
        AuthTabOption(
            modifier = Modifier.weight(1f),
            label = "Sign Up",
            selected = selected == AuthTab.SIGN_UP,
            onClick = { onTabChange(AuthTab.SIGN_UP) }
        )
    }
}

@Composable
private fun AuthTabOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val famExType = LocalFamExTypography.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (selected) Modifier.background(MaterialTheme.colorScheme.surface) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = famExType.labelMd,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun LogInForm(
    uiState: AuthUiState,
    onLoginEmailChange: (String) -> Unit,
    onLoginPinChange: (String) -> Unit,
    onLogin: () -> Unit,
    onForgotPin: () -> Unit
) {
    val famExType = LocalFamExTypography.current

    Box(
        modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Welcome Back", style = famExType.headlineLg, color = MaterialTheme.colorScheme.onSurface)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Secure access to your family vault.",
        style = famExType.bodyMd,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(text = "Email Address", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.loginEmail,
            onValueChange = onLoginEmailChange,
            placeholder = { Text(text = "household@example.com", style = famExType.bodyMd) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = fieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Secure 6-Digit PIN", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onForgotPin) {
                Text(text = "Forgot PIN?", style = famExType.labelMd, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        PinInputField(value = uiState.loginPin, onValueChange = onLoginPinChange)
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onLogin,
        enabled = !uiState.isLoggingIn,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text = if (uiState.isLoggingIn) "Accessing…" else "Access Vault", style = famExType.headlineSm)
    }

    uiState.loginError?.let { error ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = error, style = famExType.labelSm, color = MaterialTheme.colorScheme.error)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "Your financial data is securely stored.", style = famExType.labelSm, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun SignUpForm(
    uiState: AuthUiState,
    onSignUpFullNameChange: (String) -> Unit,
    onSignUpNicknameChange: (String) -> Unit,
    onSignUpEmailChange: (String) -> Unit,
    onSignUpPinChange: (String) -> Unit,
    onSignUpPinConfirmChange: (String) -> Unit,
    onSignUp: () -> Unit
) {
    val famExType = LocalFamExTypography.current

    Text(text = "Create your account", style = famExType.headlineLg, color = MaterialTheme.colorScheme.onSurface)
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = "Set up a new household vault.", style = famExType.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(24.dp))

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        LabeledField(label = "Full Name", value = uiState.signUpFullName, onValueChange = onSignUpFullNameChange, placeholder = "Jane Doe")
        Spacer(modifier = Modifier.height(16.dp))
        LabeledField(
            label = "Nickname",
            trailingLabel = "(Optional)",
            value = uiState.signUpNickname,
            onValueChange = onSignUpNicknameChange,
            placeholder = "e.g. Mom"
        )
        Spacer(modifier = Modifier.height(16.dp))
        LabeledField(label = "Email Address", value = uiState.signUpEmail, onValueChange = onSignUpEmailChange, placeholder = "jane@example.com")

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Create 6-Digit PIN", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        PinInputField(value = uiState.signUpPin, onValueChange = onSignUpPinChange)

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Confirm PIN", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        PinInputField(value = uiState.signUpPinConfirm, onValueChange = onSignUpPinConfirmChange)
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onSignUp,
        enabled = !uiState.isSigningUp,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text = if (uiState.isSigningUp) "Creating…" else "Create Account", style = famExType.headlineSm)
    }

    uiState.signUpError?.let { error ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = error, style = famExType.labelSm, color = MaterialTheme.colorScheme.error)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "By creating an account, you agree to our Terms.",
        style = famExType.labelSm,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    trailingLabel: String? = null
) {
    val famExType = LocalFamExTypography.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (trailingLabel != null) {
            Text(text = trailingLabel, style = famExType.labelSm, color = MaterialTheme.colorScheme.outline)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder, style = famExType.bodyMd) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = fieldColors()
    )
}
