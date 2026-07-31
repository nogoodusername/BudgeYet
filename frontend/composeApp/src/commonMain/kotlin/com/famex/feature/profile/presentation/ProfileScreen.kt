package com.famex.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.famex.core.model.MemberRole
import com.famex.core.ui.InitialsAvatar

// Read-only stub per Phase 1 scope — editable profile (name/nickname, currency/language,
// display mode) is deferred to Phase 3 alongside collaboration/invite management.
@Composable
fun ProfileScreen(uiState: ProfileUiState, modifier: Modifier = Modifier) {
    when {
        uiState.isLoading && uiState.user == null ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        uiState.errorMessage != null && uiState.user == null ->
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage)
            }

        uiState.user != null && uiState.household != null -> {
            val user = uiState.user
            val household = uiState.household
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InitialsAvatar(name = user.nickname)
                        Column {
                            Text(text = user.fullName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = user.email, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                item {
                    Column {
                        Text(text = household.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        Text(
                            text = "${household.currency} • ${household.language}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                item {
                    Text(text = "Members", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
                items(household.members) { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = member.user.nickname, fontSize = 14.sp)
                        Text(
                            text = if (member.role == MemberRole.ADMIN) "Admin" else "Member",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
