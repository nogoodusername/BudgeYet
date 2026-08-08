package com.budgeyet.feature.category.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.budgeyet.core.ui.categoryIconChoices
import com.budgeyet.theme.LocalBudgeYetTypography

// Bottom-anchored Dialog rather than Material3's ModalBottomSheet, matching
// TransactionFilterSheet — keeps the pattern consistent and portable to iOS.
@Composable
fun IconPickerSheet(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val budgeYetType = LocalBudgeYetTypography.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // The Dialog's platform window defaults to WRAP_CONTENT height + center gravity, so a
        // fractional-height outer Box here would shrink the whole window to that fraction and
        // have it centered on screen instead of docked to the bottom. fillMaxSize() on the
        // outer Box forces the window to size to the full screen; the height fraction goes on
        // the Surface instead, which BottomCenter then has real slack to anchor at the bottom.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(width = 48.dp, height = 5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Select Icon", style = budgeYetType.headlineLg, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close icon picker")
                            }
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(categoryIconChoices) { iconKey ->
                            IconGridItem(
                                iconKey = iconKey,
                                selected = iconKey == selectedIcon,
                                onClick = { onIconSelected(iconKey) }
                            )
                        }
                    }
                }
            }
        }
    }
}
