package com.famex.feature.auth.presentation

import com.famex.core.model.BackendConfig
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

sealed class BackendConfigEvent {
    data object Saved : BackendConfigEvent()
}

class BackendConfigController(
    private val repository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(BackendConfigUiState())
    val uiState: StateFlow<BackendConfigUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BackendConfigEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<BackendConfigEvent> = _events.asSharedFlow()

    fun load() {
        scope.launch {
            when (val config = repository.getBackendConfig()) {
                BackendConfig.Hosted -> _uiState.update { it.copy(selection = BackendConfigSelection.HOSTED) }
                is BackendConfig.Custom -> _uiState.update {
                    it.copy(selection = BackendConfigSelection.CUSTOM, customUrl = config.url)
                }
            }
        }
    }

    fun onSelectHosted() = _uiState.update { it.copy(selection = BackendConfigSelection.HOSTED, saveError = null) }
    fun onSelectCustom() = _uiState.update { it.copy(selection = BackendConfigSelection.CUSTOM, saveError = null) }
    fun onCustomUrlChange(value: String) = _uiState.update { it.copy(customUrl = value, saveError = null) }

    fun onSave() {
        val state = _uiState.value
        val config = when (state.selection) {
            BackendConfigSelection.HOSTED -> BackendConfig.Hosted
            BackendConfigSelection.CUSTOM -> {
                if (!state.customUrl.startsWith("http://") && !state.customUrl.startsWith("https://")) {
                    _uiState.update { it.copy(saveError = "Enter a valid URL (https://...)") }
                    return
                }
                BackendConfig.Custom(state.customUrl.trim())
            }
        }

        scope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                repository.setBackendConfig(config)
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(BackendConfigEvent.Saved)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, saveError = t.message ?: "Couldn't save") }
            }
        }
    }
}
