package com.budgeyet.core.util

import kotlin.math.roundToInt

fun currencySymbolFor(currencyCode: String): String = when (currencyCode.uppercase()) {
    "USD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    "INR" -> "₹"
    "JPY" -> "¥"
    "CAD" -> "CA$"
    "AUD" -> "AU$"
    "CHF" -> "Fr"
    "CNY" -> "¥"
    "BRL" -> "R$"
    "KRW" -> "₩"
    "SEK" -> "kr"
    else -> "$currencyCode "
}

fun formatAmount(amount: Double, currencySymbol: String): String =
    "$currencySymbol${groupThousands(amount.roundToInt())}"

private fun groupThousands(value: Int): String {
    val digits = kotlin.math.abs(value).toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (value < 0) "-$grouped" else grouped
}
