package com.budgeyet.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.budgeyet.core.model.PaymentMode

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
    "flight" -> Icons.Default.Flight
    "pets" -> Icons.Default.Pets
    "gift" -> Icons.Default.CardGiftcard
    "clothing" -> Icons.Default.Checkroom
    "devices" -> Icons.Default.Devices
    "insurance" -> Icons.Default.Security
    "subscriptions" -> Icons.Default.Subscriptions
    "childcare" -> Icons.Default.ChildCare
    "cafe" -> Icons.Default.LocalCafe
    "internet" -> Icons.Default.Wifi
    "personal_care" -> Icons.Default.Spa
    "donation" -> Icons.Default.VolunteerActivism
    else -> Icons.Default.Category
}

// The Add Category icon-picker grid (Stitch "Add Category Form" export) — a fixed set of
// keys that all resolve through categoryIcon() above, so picking one round-trips cleanly
// through Category.icon. The first categoryIconGridPreviewCount entries are shown inline on
// the Add Category form; the rest are only reachable via the "See all icons" picker sheet, so
// growing this list doesn't grow that form's fixed height.
val categoryIconChoices: List<String> = listOf(
    "cart", "restaurant", "home", "car", "medical", "fitness", "school", "movie",
    "flight", "savings",
    "pets", "gift", "clothing", "devices", "insurance", "subscriptions", "childcare", "cafe", "internet",
    "personal_care", "donation", "flash"
)

// Rows shown inline on the Add Category form before it falls back to "See all icons" —
// 2 rows x 5 columns, matching IconSelectionCard's grid.
const val categoryIconGridPreviewCount: Int = 10

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
