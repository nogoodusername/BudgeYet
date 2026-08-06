package com.famex.feature.transaction.presentation

import com.famex.core.model.PaymentMode
import com.famex.feature.category.domain.CategoryRepository
import com.famex.feature.profile.domain.ProfileRepository
import com.famex.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class HistoryController(
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository,
    private val categoryRepository: CategoryRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val transactions = transactionRepository.getTransactions()
                val household = profileRepository.getHousehold()
                val categories = categoryRepository.getCategories()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        transactions = transactions,
                        householdMembers = household.members,
                        categories = categories,
                        currency = household.currency
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }

    fun onSearchChange(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun onOpenFilterSheet() = _uiState.update { it.copy(showFilterSheet = true) }

    fun onCloseFilterSheet() = _uiState.update { it.copy(showFilterSheet = false) }

    fun onResetFilters() = _uiState.update {
        it.copy(
            dateRangeFilter = DateRangeFilter.ALL,
            customStartDate = null,
            customEndDate = null,
            selectedPayerUserIds = emptySet(),
            selectedPaymentModes = emptySet()
        )
    }

    fun onSelectDateRangeFilter(filter: DateRangeFilter) = _uiState.update { it.copy(dateRangeFilter = filter) }

    fun onTogglePayer(userId: Long) = _uiState.update {
        val current = it.selectedPayerUserIds
        it.copy(selectedPayerUserIds = if (userId in current) current - userId else current + userId)
    }

    fun onTogglePaymentMode(mode: PaymentMode) = _uiState.update {
        val current = it.selectedPaymentModes
        it.copy(selectedPaymentModes = if (mode in current) current - mode else current + mode)
    }

    fun onOpenCustomStartPicker() = _uiState.update { it.copy(showCustomStartPicker = true) }

    fun onCloseCustomStartPicker() = _uiState.update { it.copy(showCustomStartPicker = false) }

    fun onCustomStartSelected(date: LocalDate) = _uiState.update {
        it.copy(customStartDate = date, showCustomStartPicker = false, dateRangeFilter = DateRangeFilter.CUSTOM)
    }

    fun onOpenCustomEndPicker() = _uiState.update { it.copy(showCustomEndPicker = true) }

    fun onCloseCustomEndPicker() = _uiState.update { it.copy(showCustomEndPicker = false) }

    fun onCustomEndSelected(date: LocalDate) = _uiState.update {
        it.copy(customEndDate = date, showCustomEndPicker = false, dateRangeFilter = DateRangeFilter.CUSTOM)
    }
}
