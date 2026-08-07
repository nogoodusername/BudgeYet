package com.budgeyet.feature.auth.presentation

import com.budgeyet.core.model.Household
import com.budgeyet.core.util.currentMonthYearLabel
import com.budgeyet.feature.auth.domain.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class BudgetGoalEvent {
    data class Saved(val household: Household, val monthlyGoalAmount: Double) : BudgetGoalEvent()
    data class Skipped(val household: Household) : BudgetGoalEvent()
}

class BudgetGoalController(
    private val household: Household,
    private val repository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(
        BudgetGoalUiState(budgetName = "${currentMonthYearLabel()} Budget")
    )
    val uiState: StateFlow<BudgetGoalUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BudgetGoalEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<BudgetGoalEvent> = _events.asSharedFlow()

    fun onBudgetNameChange(value: String) = _uiState.update { it.copy(budgetName = value, error = null) }

    fun onGoalAmountChange(value: String) {
        if (value.isNotEmpty() && !value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) return
        _uiState.update { it.copy(monthlyGoalAmountText = value, error = null) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.budgetName.isBlank()) {
            _uiState.update { it.copy(error = "Enter a budget name") }
            return
        }
        val amount = state.monthlyGoalAmountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(error = "Enter a valid monthly goal amount") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                repository.setupBudget(household.id, state.budgetName.trim(), amount)
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(BudgetGoalEvent.Saved(household, amount))
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, error = t.message ?: "Couldn't save budget goal") }
            }
        }
    }

    fun onSkip() {
        scope.launch { _events.emit(BudgetGoalEvent.Skipped(household)) }
    }
}
