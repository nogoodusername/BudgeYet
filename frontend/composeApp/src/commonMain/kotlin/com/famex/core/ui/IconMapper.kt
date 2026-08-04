package com.famex.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.famex.core.model.PaymentMode

// Category.icon stores a small string key (see fixtures/DummyData.kt) rather than a
// full icon-pack reference — this is the single place that key resolves to a glyph.
fun categoryIcon(iconKey: String): ImageVector = when (iconKey.lowercase()) {
    "cart" -> Icons.Default.ShoppingCart
    "restaurant" -> Icons.Default.Restaurant
    "flash" -> Icons.Default.Bolt
    "car" -> Icons.Default.DirectionsCar
    "savings" -> Icons.Default.Savings
    "home" -> Icons.Default.Home
    "medical" -> Icons.Default.MedicalServices
    "fitness" -> Icons.Default.FitnessCenter
    "school" -> Icons.Default.School
    "movie" -> Icons.Default.Movie
    else -> Icons.Default.Category
}

// The Add Category icon-picker grid (Stitch "Add Category Form" export) — a fixed set of
// keys that all resolve through categoryIcon() above, so picking one round-trips cleanly
// through Category.icon.
val categoryIconChoices: List<String> = listOf(
    "cart", "restaurant", "home", "car", "medical", "fitness", "school", "movie"
)

fun paymentModeIcon(mode: PaymentMode): ImageVector = when (mode) {
    PaymentMode.CASH -> Icons.Default.Payments
    PaymentMode.CARD -> Icons.Default.CreditCard
    PaymentMode.BANK_TRANSFER -> Icons.Default.AccountBalance
    PaymentMode.OTHER -> Icons.Default.MoreHoriz
}

fun paymentModeLabel(mode: PaymentMode): String = when (mode) {
    PaymentMode.CASH -> "Cash"
    PaymentMode.CARD -> "Card"
    PaymentMode.BANK_TRANSFER -> "Bank Transfer"
    PaymentMode.OTHER -> "Other"
}
