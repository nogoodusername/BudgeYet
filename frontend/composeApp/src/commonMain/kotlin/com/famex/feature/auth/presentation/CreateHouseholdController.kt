package com.famex.feature.auth.presentation

import com.famex.core.model.Household
import com.famex.feature.auth.domain.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class CreateHouseholdEvent {
    data class Created(val household: Household) : CreateHouseholdEvent()
}

class CreateHouseholdController(
    private val email: String,
    private val repository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(CreateHouseholdUiState())
    val uiState: StateFlow<CreateHouseholdUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateHouseholdEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<CreateHouseholdEvent> = _events.asSharedFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onCurrencyChange(value: String) = _uiState.update { it.copy(currency = value) }
    fun onCycleStartDayChange(value: Int) = _uiState.update { it.copy(cycleStartDay = value) }

    fun onCreate() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Enter a household name") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            try {
                val household = repository.createHousehold(email, state.name.trim(), state.currency, state.cycleStartDay)
                _uiState.update { it.copy(isCreating = false) }
                _events.emit(CreateHouseholdEvent.Created(household))
            } catch (t: Throwable) {
                _uiState.update { it.copy(isCreating = false, error = t.message ?: "Couldn't create household") }
            }
        }
    }
}
