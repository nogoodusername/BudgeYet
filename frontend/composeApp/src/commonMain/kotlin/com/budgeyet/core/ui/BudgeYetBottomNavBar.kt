package com.budgeyet.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgeyet.theme.LocalBudgeYetTypography

/**
 * Consistent bottom navigation bar (Stitch screen f77403d7db614bcba0e872081e09dfee):
 * 4 destinations plus an inline "Add" action, all styled the same — no floating/overlapping FAB.
 */
@Composable
fun BudgeYetBottomNavBar(
    selectedTab: BottomNavTab,
    showAddButton: Boolean,
    onDashboard: () -> Unit,
    onCategories: () -> Unit,
    onAdd: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Filled.Dashboard,
                label = "Dashboard",
                selected = selectedTab == BottomNavTab.Dashboard,
                onClick = onDashboard
            )
            BottomNavItem(
                icon = Icons.Filled.Category,
                label = "Categories",
                selected = selectedTab == BottomNavTab.Categories,
                onClick = onCategories
            )
            if (showAddButton) {
                BottomNavItem(
                    icon = Icons.Filled.Add,
                    label = "Add",
                    selected = false,
                    emphasized = true,
                    onClick = onAdd
                )
            }
            BottomNavItem(
                icon = Icons.Filled.History,
                label = "History",
                selected = selectedTab == BottomNavTab.History,
                onClick = onHistory
            )
            BottomNavItem(
                icon = Icons.Filled.Person,
                label = "Profile",
                selected = selectedTab == BottomNavTab.Profile,
                onClick = onProfile
            )
        }
    }
}

enum class BottomNavTab { Dashboard, Categories, History, Profile, None }

@Composable
private fun RowScope.BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    val budgeYetType = LocalBudgeYetTypography.current
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (emphasized) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(1.dp, color, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
            }
        } else {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(text = label, style = budgeYetType.labelSm, color = color)
    }
}
