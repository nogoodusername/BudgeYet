package com.budgeyet.feature.dashboard.presentation

import com.budgeyet.core.cache.LocalCacheStore
import com.budgeyet.feature.dashboard.domain.DashboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Plain state holder — StateFlow<UiState> + SharedFlow<Event> on a caller-supplied
// CoroutineScope (rememberCoroutineScope() from the Route), not androidx.lifecycle.ViewModel.
class DashboardController(
    private val repository: DashboardRepository,
    private val cacheStore: LocalCacheStore,
    private val scope: CoroutineScope
) {
    // Guards against refetching every time the Route re-enters composition (tab switch). The
    // controller is hoisted into MainAppShell so its state survives; the first load() wins and
    // later ones are no-ops unless the user explicitly asks (retry / pull-to-refresh).
    private var hasLoaded = false

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DashboardEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()

    fun load(forceRefresh: Boolean = false) {
        if (hasLoaded && !forceRefresh) return
        hasLoaded = true
        scope.launch {
            // Cache-first paint: show the last-known dashboard immediately so the spinner is
            // reserved for a genuine cold start (no cache). The network fetch still runs and
            // swaps in fresh data when it lands.
            if (_uiState.value.data == null) {
                cacheStore.getCachedDashboardData()?.let { cached ->
                    _uiState.update { it.copy(data = cached) }
                }
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val data = repository.getDashboard()
                _uiState.update { it.copy(isLoading = false, data = data) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }

    fun retry() = load(forceRefresh = true)

    fun onCategoryClick(categoryId: Long) {
        scope.launch { _events.emit(DashboardEvent.NavigateToCategoryDetail(categoryId)) }
    }
}
