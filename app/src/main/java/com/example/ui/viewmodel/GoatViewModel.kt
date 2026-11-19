package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.Goat
import com.example.data.local.GoatDao
import com.example.data.local.GoatFarmDatabase
import com.example.data.repository.LocalGoatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "GoatViewModel"

/**
 * Sealed UI state representing local Room database fetch status for [Goat] entities.
 */
sealed interface GoatUiState {
    data object Loading : GoatUiState
    data class Success(val goats: List<Goat>) : GoatUiState
    data class Error(val message: String) : GoatUiState
}

/**
 * ViewModel managing local Room state for fetching, adding, updating, filtering,
 * and deleting [Goat] records via [LocalGoatRepository] and [GoatDao].
 */
class GoatViewModel(
    private val repository: LocalGoatRepository
) : ViewModel() {

    constructor(goatDao: GoatDao) : this(LocalGoatRepository(goatDao))

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _healthStatusFilter = MutableStateFlow("All")
    val healthStatusFilter: StateFlow<String> = _healthStatusFilter.asStateFlow()

    private val _isOperationInProgress = MutableStateFlow(false)
    val isOperationInProgress: StateFlow<Boolean> = _isOperationInProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _statusEvent = MutableSharedFlow<String>()
    val statusEvent: SharedFlow<String> = _statusEvent.asSharedFlow()

    /**
     * Reactive UI state combining all goats from [GoatDao] with search and health filters.
     */
    val uiState: StateFlow<GoatUiState> = combine(
        repository.allGoats,
        _searchQuery,
        _healthStatusFilter
    ) { allGoats, query, statusFilter ->
        val filtered = allGoats.filter { goat ->
            val matchesQuery = query.isBlank() ||
                goat.name.contains(query.trim(), ignoreCase = true) ||
                goat.breed.contains(query.trim(), ignoreCase = true) ||
                goat.id.toString() == query.trim()

            val matchesStatus = statusFilter.equals("All", ignoreCase = true) ||
                goat.healthStatus.equals(statusFilter, ignoreCase = true)

            matchesQuery && matchesStatus
        }
        GoatUiState.Success(filtered) as GoatUiState
    }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.e(TAG, "Error fetching goats from Room database", error)
            emit(GoatUiState.Error(error.localizedMessage ?: "Failed to load goats from database."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = GoatUiState.Loading
        )

    /**
     * Convenience reactive list of goats exposed directly from [uiState].
     */
    val goats: StateFlow<List<Goat>> = uiState
        .map { state -> (state as? GoatUiState.Success)?.goats ?: emptyList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHealthStatusFilter(healthStatus: String) {
        _healthStatusFilter.value = healthStatus
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun getGoatById(id: Int): Flow<Goat?> = repository.getGoatById(id)

    fun addGoat(
        name: String,
        birthDate: String,
        breed: String,
        healthStatus: String,
        onResult: ((Result<Long>) -> Unit)? = null
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            val error = IllegalArgumentException("Goat name cannot be empty.")
            _errorMessage.value = error.message
            onResult?.invoke(Result.failure(error))
            return
        }

        val goat = Goat(
            name = trimmedName,
            birthDate = birthDate.trim(),
            breed = breed.trim(),
            healthStatus = healthStatus.trim().ifBlank { "Healthy" }
        )
        addGoat(goat, onResult)
    }

    fun addGoat(
        goat: Goat,
        onResult: ((Result<Long>) -> Unit)? = null
    ) {
        if (goat.name.isBlank()) {
            val error = IllegalArgumentException("Goat name cannot be empty.")
            _errorMessage.value = error.message
            onResult?.invoke(Result.failure(error))
            return
        }

        viewModelScope.launch {
            _isOperationInProgress.value = true
            _errorMessage.value = null
            try {
                val rowId = repository.insert(goat)
                _statusEvent.emit("Added ${goat.name} to local herd")
                onResult?.invoke(Result.success(rowId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert goat", e)
                val msg = e.localizedMessage ?: "Failed to add goat."
                _errorMessage.value = msg
                _statusEvent.emit(msg)
                onResult?.invoke(Result.failure(e))
            } finally {
                _isOperationInProgress.value = false
            }
        }
    }

    fun updateGoat(
        goat: Goat,
        onResult: ((Result<Unit>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isOperationInProgress.value = true
            _errorMessage.value = null
            try {
                repository.update(goat)
                _statusEvent.emit("Updated ${goat.name}")
                onResult?.invoke(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update goat id=${goat.id}", e)
                val msg = e.localizedMessage ?: "Failed to update goat."
                _errorMessage.value = msg
                _statusEvent.emit(msg)
                onResult?.invoke(Result.failure(e))
            } finally {
                _isOperationInProgress.value = false
            }
        }
    }

    fun deleteGoat(
        goat: Goat,
        onResult: ((Result<Unit>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isOperationInProgress.value = true
            _errorMessage.value = null
            try {
                repository.delete(goat)
                _statusEvent.emit("Deleted ${goat.name}")
                onResult?.invoke(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete goat id=${goat.id}", e)
                val msg = e.localizedMessage ?: "Failed to delete goat."
                _errorMessage.value = msg
                _statusEvent.emit(msg)
                onResult?.invoke(Result.failure(e))
            } finally {
                _isOperationInProgress.value = false
            }
        }
    }

    fun deleteGoatById(
        id: Int,
        onResult: ((Result<Unit>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isOperationInProgress.value = true
            _errorMessage.value = null
            try {
                repository.deleteById(id)
                _statusEvent.emit("Removed goat #$id")
                onResult?.invoke(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete goat by id=$id", e)
                val msg = e.localizedMessage ?: "Failed to delete goat."
                _errorMessage.value = msg
                _statusEvent.emit(msg)
                onResult?.invoke(Result.failure(e))
            } finally {
                _isOperationInProgress.value = false
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val dao = GoatFarmDatabase.getInstance(context.applicationContext).goatDao()
                GoatViewModel(LocalGoatRepository(dao))
            }
        }
    }
}
