package com.famex.core.util

import kotlin.math.roundToInt

fun currencySymbolFor(currencyCode: String): String = when (currencyCode.uppercase()) {
    "USD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    "INR" -> "₹"
    else -> "$currencyCode "
}

fun formatAmount(amount: Double, currencySymbol: String): String =
    "$currencySymbol${amount.roundToInt()}"
