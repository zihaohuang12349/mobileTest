package com.example.mobiletest.ui.page

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletest.data.ShippingRepository
import com.example.mobiletest.data.model.ShippingItinerary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

data class ShipListUiState(
    val userId: String = "user123",
    val list: List<ShippingItinerary> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val creating: Boolean = false,
    val updating: Boolean = false,
    val deleting: Boolean = false,
    val message: String = "",
    val showCreateDialog: Boolean = false,
    val validityInput: String = "",
    val showUpdateDialog: Boolean = false,
    val updateTargetId: String = "",
    val updateValidityInput: String = "",
    val nowMillis: Long = System.currentTimeMillis()
)

class ShipListViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ShippingRepository(application)

    private val _uiState = MutableStateFlow(ShipListUiState())
    val uiState: StateFlow<ShipListUiState> = _uiState.asStateFlow()

    private var collectJob: Job? = null

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1.seconds)
        }
    }

    init {
        collect("user123")
        refresh()
        viewModelScope.launch {
            ticker.collect { now ->
                _uiState.update { it.copy(nowMillis = now) }
            }
        }
    }

    fun onResume() {
        refresh()
    }

    private fun collect(userId: String) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repo.observe(userId).collect { list ->
                _uiState.update { it.copy(list = list) }
            }
        }
    }

    fun onClearMessage() {
        _uiState.update { it.copy(message = "") }
    }

    fun onUserIdChanged(value: String) {
        _uiState.update { it.copy(userId = value) }
    }

    fun onSearch() {
        refresh()
    }

    fun onPullRefresh() {
        val state = _uiState.value
        if (state.loading || state.creating || state.updating || state.deleting) return
        _uiState.update { it.copy(refreshing = true) }
        refresh(silent = true)
    }

    fun onAdd() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun onDismissCreate() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun onValidityInput(value: String) {
        _uiState.update { it.copy(validityInput = value) }
    }

    fun onConfirmCreate() {
        val state = _uiState.value
        val seconds = state.validityInput.toLongOrNull()
        if (seconds == null || seconds <= 0) {
            _uiState.update { it.copy(message = "Please enter valid seconds") }
            return
        }
        _uiState.update { it.copy(creating = true) }
        viewModelScope.launch {
            repo.create(state.userId, seconds)
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(message = "Created: ${data.id}", validityInput = "", showCreateDialog = false, creating = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = e.message ?: "Create failed", showCreateDialog = false, creating = false) }
                }
        }
    }

    fun onUpdate(id: String) {
        _uiState.update { it.copy(showUpdateDialog = true, updateTargetId = id, updateValidityInput = "") }
    }

    fun onDismissUpdate() {
        if (!_uiState.value.updating) {
            _uiState.update { it.copy(showUpdateDialog = false) }
        }
    }

    fun onUpdateValidityInput(value: String) {
        _uiState.update { it.copy(updateValidityInput = value) }
    }

    fun onConfirmUpdate() {
        val state = _uiState.value
        val seconds = state.updateValidityInput.toLongOrNull()
        if (seconds == null || seconds <= 0) {
            _uiState.update { it.copy(message = "Please enter valid seconds") }
            return
        }
        _uiState.update { it.copy(updating = true) }
        viewModelScope.launch {
            val expiry = (System.currentTimeMillis() / 1000 + seconds).toString()
            repo.update(state.updateTargetId, state.userId, expiry)
                .onSuccess {
                    _uiState.update {
                        it.copy(message = "Updated: ${state.updateTargetId}", showUpdateDialog = false, updating = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = e.message ?: "Update failed", showUpdateDialog = false, updating = false) }
                }
        }
    }

    fun onDelete(id: String) {
        val userId = _uiState.value.userId
        _uiState.update { it.copy(deleting = true) }
        viewModelScope.launch {
            repo.delete(id, userId)
                .onSuccess {
                    _uiState.update { it.copy(message = "Deleted: $id", deleting = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = e.message ?: "Delete failed", deleting = false) }
                }
        }
    }

    private fun refresh(silent: Boolean = false) {
        val userId = _uiState.value.userId
        collect(userId)
        if (!silent) {
            _uiState.update { it.copy(loading = true) }
        }
        viewModelScope.launch {
            repo.refresh(userId)
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(message = if (data.isEmpty()) "No data" else "", loading = false, refreshing = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = e.message ?: "Load failed", loading = false, refreshing = false) }
                }
        }
    }
}
