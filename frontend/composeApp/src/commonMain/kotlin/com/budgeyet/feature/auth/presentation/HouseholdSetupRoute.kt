package com.budgeyet.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgeyet.core.model.Household
import com.budgeyet.core.navigation.BackHandler
import com.budgeyet.core.ui.dismissKeyboardOnTap
import com.budgeyet.core.ui.keyboardAwarePadding
import com.budgeyet.theme.LocalBudgeYetTypography

private enum class HouseholdSetupStep { Choice, Create, Join }

/**
 * Shown when the app has a valid session but no active household — a fresh login that hasn't
 * picked one yet, or a solo Owner who just deleted theirs (see HouseholdMembersScreen's Danger
 * Zone). Mirrors the onboarding funnel's Household Choice → Create / Join sub-flow, but for an
 * already-authenticated user: the access token is still valid, so this reuses
 * Create/JoinHouseholdRoute directly and hands the resulting Household back to App.kt to fold
 * into the session. Creating here skips the onboarding Budget Goal / Configure Categories
 * steps — those stay reachable from the Dashboard's "set up budget" CTA.
 */
@Composable
fun HouseholdSetupRoute(
    email: String,
    onHouseholdReady: (Household) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(HouseholdSetupStep.Choice) }
    val budgeYetType = LocalBudgeYetTypography.current

    BackHandler(enabled = step != HouseholdSetupStep.Choice) { step = HouseholdSetupStep.Choice }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .dismissKeyboardOnTap()
            .keyboardAwarePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (step == HouseholdSetupStep.Choice) {
                Text(
                    text = "BudgeYet",
                    style = budgeYetType.headlineSm,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
                TextButton(onClick = onSignOut) { Text("Sign Out") }
            } else {
                IconButton(onClick = { step = HouseholdSetupStep.Choice }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        when (step) {
            HouseholdSetupStep.Choice -> HouseholdChoiceScreen(
                onCreateHousehold = { step = HouseholdSetupStep.Create },
                onJoinHousehold = { step = HouseholdSetupStep.Join }
            )

            HouseholdSetupStep.Create -> CreateHouseholdRoute(
                email = email,
                onCreated = onHouseholdReady
            )

            HouseholdSetupStep.Join -> JoinHouseholdRoute(
                email = email,
                onJoined = onHouseholdReady
            )
        }
    }
}
