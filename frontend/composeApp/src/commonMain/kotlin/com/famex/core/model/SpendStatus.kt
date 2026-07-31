package com.famex.core.model

// Thresholds must stay identical across dashboard and category views (PRD requirement).
enum class SpendStatus { ON_TRACK, WARNING, OVER_BUDGET }

fun spendStatusFor(percentUsed: Float): SpendStatus = when {
    percentUsed >= 1f -> SpendStatus.OVER_BUDGET
    percentUsed >= 0.75f -> SpendStatus.WARNING
    else -> SpendStatus.ON_TRACK
}
