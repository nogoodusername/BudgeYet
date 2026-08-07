package com.budgeyet.feature.transaction.presentation

import com.budgeyet.core.model.Category
import com.budgeyet.core.model.HouseholdMember
import com.budgeyet.core.model.PaymentMode
import com.budgeyet.core.model.Transaction
import com.budgeyet.core.util.todayLocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

enum class DateRangeFilter { ALL, THIS_MONTH, LAST_MONTH, CUSTOM }

data class HistoryUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val householdMembers: List<HouseholdMember> = emptyList(),
    val categories: List<Category> = emptyList(),
    val currency: String = "USD",
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val dateRangeFilter: DateRangeFilter = DateRangeFilter.ALL,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    val selectedPayerUserIds: Set<Long> = emptySet(),
    val selectedPaymentModes: Set<PaymentMode> = emptySet(),
    val showFilterSheet: Boolean = false,
    val showCustomStartPicker: Boolean = false,
    val showCustomEndPicker: Boolean = false
) {
    val categoriesById: Map<Long, Category> get() = categories.associateBy { it.id }

    val hasActiveFilters: Boolean
        get() = dateRangeFilter != DateRangeFilter.ALL || selectedPayerUserIds.isNotEmpty() || selectedPaymentModes.isNotEmpty()

    val activeFilterCount: Int
        get() = listOf(
            dateRangeFilter != DateRangeFilter.ALL,
            selectedPayerUserIds.isNotEmpty(),
            selectedPaymentModes.isNotEmpty()
        ).count { it }

    val filteredTransactions: List<Transaction>
        get() {
            var list = transactions

            if (searchQuery.isNotBlank()) {
                list = list.filter { transaction ->
                    transaction.merchant.contains(searchQuery, ignoreCase = true) ||
                        transaction.categoryName?.contains(searchQuery, ignoreCase = true) == true ||
                        transaction.paidBy.nickname.contains(searchQuery, ignoreCase = true)
                }
            }

            if (selectedPayerUserIds.isNotEmpty()) {
                list = list.filter { it.paidBy.id in selectedPayerUserIds }
            }

            if (selectedPaymentModes.isNotEmpty()) {
                list = list.filter { it.paymentMode in selectedPaymentModes }
            }

            list = when (dateRangeFilter) {
                DateRangeFilter.ALL -> list
                DateRangeFilter.THIS_MONTH -> {
                    val today = todayLocalDate()
                    list.filter { it.transactionDate.year == today.year && it.transactionDate.monthNumber == today.monthNumber }
                }
                DateRangeFilter.LAST_MONTH -> {
                    val firstOfThisMonth = LocalDate(todayLocalDate().year, todayLocalDate().monthNumber, 1)
                    val lastMonthEnd = firstOfThisMonth.minus(1, DateTimeUnit.DAY)
                    val lastMonthStart = LocalDate(lastMonthEnd.year, lastMonthEnd.monthNumber, 1)
                    list.filter { it.transactionDate >= lastMonthStart && it.transactionDate <= lastMonthEnd }
                }
                DateRangeFilter.CUSTOM -> {
                    val start = customStartDate
                    val end = customEndDate
                    if (start != null && end != null) {
                        list.filter { it.transactionDate >= start && it.transactionDate <= end }
                    } else {
                        list
                    }
                }
            }

            return list.sortedByDescending { it.transactionDate }
        }
}
