package com.budgeyet.feature.transaction.presentation

import com.budgeyet.core.model.PaymentMode
import com.budgeyet.core.model.Transaction
import com.budgeyet.core.model.TransactionType
import com.budgeyet.feature.category.domain.CategoryRepository
import com.budgeyet.feature.profile.domain.ProfileRepository
import com.budgeyet.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

sealed class AddTransactionEvent {
    data object Saved : AddTransactionEvent()
}

class AddTransactionController(
    private val categoryRepository: CategoryRepository,
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    // Save is a one-time event, not state — never replay: {} for the same reason navigation
    // events never replay.
    private val _events = MutableSharedFlow<AddTransactionEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<AddTransactionEvent> = _events.asSharedFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val categories = categoryRepository.getCategories()
                val household = profileRepository.getHousehold()
                val currentUser = profileRepository.getCurrentUser()
                val defaultPayerId = household.members.find { it.user.id == currentUser.id }?.id
                    ?: household.members.firstOrNull()?.id

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        categories = categories,
                        selectedCategoryId = state.selectedCategoryId ?: categories.firstOrNull()?.id,
                        householdMembers = household.members,
                        paidByMemberId = state.paidByMemberId ?: defaultPayerId,
                        currency = household.currency
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }

    fun onTypeChange(type: TransactionType) = _uiState.update { it.copy(type = type) }

    fun onAmountChange(rawValue: String) = _uiState.update { it.copy(amountText = sanitizeAmountInput(rawValue)) }

    fun onMerchantChange(value: String) = _uiState.update { it.copy(merchant = value) }

    fun onPaidByChange(memberId: Long) = _uiState.update { it.copy(paidByMemberId = memberId) }

    fun onPaymentModeChange(mode: PaymentMode) = _uiState.update { it.copy(paymentMode = mode) }

    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun onOpenDatePicker() = _uiState.update { it.copy(showDatePicker = true) }

    fun onCloseDatePicker() = _uiState.update { it.copy(showDatePicker = false) }

    fun onDateSelected(date: LocalDate) = _uiState.update { it.copy(selectedDate = date, showDatePicker = false) }

    fun onOpenCategoryPicker() = _uiState.update { it.copy(showCategoryPicker = true, categorySearchQuery = "") }

    fun onCloseCategoryPicker() = _uiState.update { it.copy(showCategoryPicker = false, categorySearchQuery = "") }

    fun onCategorySearchChange(query: String) = _uiState.update { it.copy(categorySearchQuery = query) }

    fun onCategorySelected(categoryId: Long) = _uiState.update {
        it.copy(selectedCategoryId = categoryId, showCategoryPicker = false, categorySearchQuery = "")
    }

    fun onSave() {
        val state = _uiState.value
        val amount = state.amountText.toDoubleOrNull()
        val payer = state.paidByMember

        val validationError = when {
            amount == null || amount <= 0.0 -> "Enter a valid amount"
            state.merchant.isBlank() -> "Enter a merchant or description"
            payer == null -> "Select who paid"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(saveError = validationError) }
            return
        }

        // Income transactions don't carry a category (hidden in the form for that type) even
        // though selectedCategoryId may still hold a leftover expense-category pick.
        val category = if (state.isExpense) state.selectedCategory else null

        scope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                transactionRepository.addTransaction(
                    Transaction(
                        id = 0,
                        merchant = state.merchant.trim(),
                        amount = amount!!,
                        type = state.type,
                        paymentMode = state.paymentMode,
                        categoryId = category?.id,
                        categoryName = category?.name,
                        paidBy = payer!!.user,
                        notes = state.notes.trim().ifBlank { null },
                        transactionDate = state.selectedDate,
                        transactionDateText = state.dateText,
                        createdAtText = "Just now"
                    )
                )
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(AddTransactionEvent.Saved)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, saveError = t.message ?: "Couldn't save transaction") }
            }
        }
    }
}

// Keeps the amount field a plain decimal string (digits + a single '.') without reformatting
// mid-edit, matching the limit-draft pattern in CategoryListController.
private fun sanitizeAmountInput(rawValue: String): String {
    var seenDot = false
    return rawValue.filter { c ->
        when {
            c.isDigit() -> true
            c == '.' && !seenDot -> { seenDot = true; true }
            else -> false
        }
    }
}
