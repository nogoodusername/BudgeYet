package com.famex.feature.auth.presentation

import com.famex.core.model.BackendConfig
import com.famex.feature.auth.domain.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

// Debounce before pinging on every keystroke — long enough to not spam the network while
// someone is still typing a URL, short enough to feel responsive once they pause.
private const val PING_DEBOUNCE_MS = 500L

class BackendConfigController(
    private val repository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(BackendConfigUiState())
    val uiState: StateFlow<BackendConfigUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BackendConfigEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<BackendConfigEvent> = _events.asSharedFlow()

    private var pingJob: Job? = null

    fun load() {
        scope.launch {
            when (val config = repository.getBackendConfig()) {
                BackendConfig.Hosted -> _uiState.update { it.copy(selection = BackendConfigSelection.HOSTED) }
                is BackendConfig.Custom -> {
                    _uiState.update { it.copy(selection = BackendConfigSelection.CUSTOM, customUrl = config.url) }
                    schedulePing(config.url)
                }
            }
        }
    }

    fun onSelectHosted() {
        pingJob?.cancel()
        _uiState.update { it.copy(selection = BackendConfigSelection.HOSTED, saveError = null) }
    }

    fun onSelectCustom() {
        _uiState.update { it.copy(selection = BackendConfigSelection.CUSTOM, saveError = null) }
        schedulePing(_uiState.value.customUrl)
    }

    fun onCustomUrlChange(value: String) {
        _uiState.update { it.copy(customUrl = value, saveError = null) }
        schedulePing(value)
    }

    fun onSave() {
        val state = _uiState.value
        val config = when (state.selection) {
            BackendConfigSelection.HOSTED -> BackendConfig.Hosted
            BackendConfigSelection.CUSTOM -> {
                if (!state.canSave) return
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

    private fun schedulePing(url: String) {
        pingJob?.cancel()

        if (url.isBlank()) {
            _uiState.update { it.copy(reachability = ReachabilityStatus.IDLE) }
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _uiState.update { it.copy(reachability = ReachabilityStatus.INVALID) }
            return
        }

        pingJob = scope.launch {
            _uiState.update { it.copy(reachability = ReachabilityStatus.CHECKING) }
            delay(PING_DEBOUNCE_MS)
            try {
                repository.checkServerReachable(url)
                _uiState.update { it.copy(reachability = ReachabilityStatus.REACHABLE) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(reachability = ReachabilityStatus.UNREACHABLE) }
            }
        }
    }
}
