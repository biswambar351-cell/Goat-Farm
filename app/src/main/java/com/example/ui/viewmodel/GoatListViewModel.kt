package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Goat
import com.example.data.repository.GoatRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing the state and actions for the [GoatListScreen],
 * backed by [GoatRepository].
 */
class GoatListViewModel(
    private val repository: GoatRepository,
    private val userId: String
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _feedbackMessage = MutableSharedFlow<String>()
    val feedbackMessage: SharedFlow<String> = _feedbackMessage.asSharedFlow()

    /**
     * Real-time stream of goats fetched from Firestore via [GoatRepository.observeGoats].
     */
    val goatsState: StateFlow<UiState<List<Goat>>> = repository.observeGoats(userId)
        .map<List<Goat>, UiState<List<Goat>>> { list ->
            UiState.Success(list.sortedBy { it.tag })
        }
        .catch { error ->
            val root = unwrapError(error)
            Log.e(TAG, "Error observing goats for user $userId", root)
            emit(UiState.Error(root.message ?: "Failed to load goats from Firestore"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    /**
     * Adds a new goat record to Firestore.
     */
    fun addGoat(goat: Goat, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.addGoat(userId, goat)
            _isSubmitting.value = false
            if (result.isSuccess) {
                _feedbackMessage.emit("Goat ${goat.name} (${goat.tag}) added successfully!")
                onComplete(true)
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                _feedbackMessage.emit("Failed to add goat: $errorMsg")
                onComplete(false)
            }
        }
    }

    /**
     * Updates an existing goat document.
     */
    fun updateGoat(goatId: String, goat: Goat, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.updateGoat(userId, goatId, goat)
            _isSubmitting.value = false
            if (result.isSuccess) {
                _feedbackMessage.emit("Goat ${goat.tag} updated!")
                onComplete(true)
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                _feedbackMessage.emit("Failed to update: $errorMsg")
                onComplete(false)
            }
        }
    }

    /**
     * Deletes a goat record from Firestore.
     */
    fun deleteGoat(goatId: String, tag: String) {
        viewModelScope.launch {
            val result = repository.deleteGoat(userId, goatId)
            if (result.isSuccess) {
                _feedbackMessage.emit("Goat $tag deleted.")
            } else {
                _feedbackMessage.emit("Delete failed: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    private fun unwrapError(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    companion object {
        private const val TAG = "GoatListVM"

        fun provideFactory(
            repository: GoatRepository,
            userId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GoatListViewModel(repository, userId) as T
            }
        }
    }
}
