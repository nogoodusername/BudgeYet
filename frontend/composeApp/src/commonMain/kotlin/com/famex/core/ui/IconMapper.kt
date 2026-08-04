package com.famex.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

// Category.icon stores a small string key (see fixtures/DummyData.kt) rather than a
// full icon-pack reference — this is the single place that key resolves to a glyph.
fun categoryIcon(iconKey: String): ImageVector = when (iconKey.lowercase()) {
    "cart" -> Icons.Default.ShoppingCart
    "restaurant" -> Icons.Default.Restaurant
    "flash" -> Icons.Default.Bolt
    "car" -> Icons.Default.DirectionsCar
    "savings" -> Icons.Default.Savings
    else -> Icons.Default.Category
}
