package com.famex.feature.transaction.presentation

import com.famex.core.model.PaymentMode
import com.famex.core.model.Transaction
import com.famex.core.model.TransactionType
import com.famex.feature.category.domain.CategoryRepository
import com.famex.feature.profile.domain.ProfileRepository
import com.famex.feature.transaction.domain.TransactionRepository
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
import kotlin.math.abs
import kotlin.math.roundToInt

sealed class EditTransactionEvent {
    data object Saved : EditTransactionEvent()
    data object Deleted : EditTransactionEvent()
}

class EditTransactionController(
    private val transactionId: Long,
    private val categoryRepository: CategoryRepository,
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(EditTransactionUiState())
    val uiState: StateFlow<EditTransactionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditTransactionEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<EditTransactionEvent> = _events.asSharedFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val transaction = transactionRepository.getTransaction(transactionId)
                if (transaction == null) {
                    _uiState.update { it.copy(isLoading = false, notFound = true) }
                    return@launch
                }
                val categories = categoryRepository.getCategories()
                val household = profileRepository.getHousehold()
                val payerId = household.members.find { it.user.id == transaction.paidBy.id }?.id

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        type = transaction.type,
                        amountText = formatAmountInput(transaction.amount),
                        merchant = transaction.merchant,
                        categories = categories,
                        selectedCategoryId = transaction.categoryId,
                        householdMembers = household.members,
                        paidByMemberId = payerId,
                        paymentMode = transaction.paymentMode,
                        notes = transaction.notes ?: ""
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }

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

        val category = if (state.isExpense) state.selectedCategory else null

        scope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                transactionRepository.updateTransaction(
                    Transaction(
                        id = transactionId,
                        merchant = state.merchant.trim(),
                        amount = amount!!,
                        type = state.type,
                        paymentMode = state.paymentMode,
                        categoryId = category?.id,
                        categoryName = category?.name,
                        paidBy = payer!!.user,
                        notes = state.notes.trim().ifBlank { null },
                        transactionDateText = state.dateText,
                        createdAtText = state.dateText
                    )
                )
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(EditTransactionEvent.Saved)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, saveError = t.message ?: "Couldn't save changes") }
            }
        }
    }

    fun onDelete() {
        scope.launch {
            _uiState.update { it.copy(isDeleting = true, saveError = null) }
            try {
                transactionRepository.deleteTransaction(transactionId)
                _uiState.update { it.copy(isDeleting = false) }
                _events.emit(EditTransactionEvent.Deleted)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isDeleting = false, saveError = t.message ?: "Couldn't delete transaction") }
            }
        }
    }
}

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

// Plain kotlin.math formatting (no java.util.Formatter / String.format) so this stays usable
// from iosMain/wasmJs too — turns 142.5 into "142.50" for the editable amount field.
private fun formatAmountInput(amount: Double): String {
    val totalCents = (amount * 100).roundToInt()
    val whole = totalCents / 100
    val cents = abs(totalCents % 100)
    return "$whole.${cents.toString().padStart(2, '0')}"
}
