package com.famex.feature.auth.presentation

data class CreateHouseholdUiState(
    val name: String = "",
    val currency: String = "USD",
    val cycleStartDay: Int = 1,
    val isCreating: Boolean = false,
    val error: String? = null
)

val createHouseholdCurrencyOptions = listOf("USD" to "USD ($)", "EUR" to "EUR (€)", "GBP" to "GBP (£)", "JPY" to "JPY (¥)")
val createHouseholdCycleStartDayOptions = listOf(1, 5, 10, 15, 20, 25)

fun cycleStartDayLabel(day: Int): String {
    val suffix = when {
        day % 10 == 1 && day != 11 -> "st"
        day % 10 == 2 && day != 12 -> "nd"
        day % 10 == 3 && day != 13 -> "rd"
        else -> "th"
    }
    return "$day$suffix of the month"
}
