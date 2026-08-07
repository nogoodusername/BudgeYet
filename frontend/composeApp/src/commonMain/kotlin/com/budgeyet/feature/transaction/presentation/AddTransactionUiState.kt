package com.budgeyet.feature.transaction.presentation

import com.budgeyet.core.model.Category
import com.budgeyet.core.model.HouseholdMember
import com.budgeyet.core.model.PaymentMode
import com.budgeyet.core.model.TransactionType
import com.budgeyet.core.util.todayLocalDate
import com.budgeyet.core.util.toDisplayText
import kotlinx.datetime.LocalDate

data class AddTransactionUiState(
    val isLoading: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "0.00",
    val merchant: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedDate: LocalDate = todayLocalDate(),
    val householdMembers: List<HouseholdMember> = emptyList(),
    val currency: String = "USD",
    val paidByMemberId: Long? = null,
    val paymentMode: PaymentMode = PaymentMode.CARD,
    val notes: String = "",
    // Category picker and date picker are in-flow overlays (not nav-stack entries) so they
    // can share this controller's state directly instead of needing a nav result channel.
    val showCategoryPicker: Boolean = false,
    val categorySearchQuery: String = "",
    val showDatePicker: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val errorMessage: String? = null
) {
    val isExpense: Boolean get() = type == TransactionType.EXPENSE

    val selectedCategory: Category? get() = categories.find { it.id == selectedCategoryId }

    val dateText: String get() = selectedDate.toDisplayText()

    val filteredCategories: List<Category>
        get() = if (categorySearchQuery.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(categorySearchQuery, ignoreCase = true) }
        }

    val paidByMember: HouseholdMember? get() = householdMembers.find { it.id == paidByMemberId }
}
