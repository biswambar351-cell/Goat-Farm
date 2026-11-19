package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BreedingRecord
import com.example.data.model.FarmProfile
import com.example.data.model.FarmTransaction
import com.example.data.model.FinanceRecord
import com.example.data.model.Goat
import com.example.data.model.HealthRecord
import com.example.data.model.MilkLog
import com.example.data.model.MilkProduction
import com.example.data.local.Goat as LocalGoat
import com.example.data.repository.GoatFarmRepository
import com.example.data.repository.LocalGoatRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DashboardSummary(
    val farmName: String = "Khulana Goat Farm",
    val farmAddress: String = "Khulna, Bangladesh",
    val ownerName: String = "",
    val totalGoats: Int = 0,
    val activeHealthAlerts: Int = 0,
    val currentMonthMilkLiters: Double = 0.0,
    val currentMonthName: String = "",
    val doeCount: Int = 0,
    val buckCount: Int = 0,
    val kidCount: Int = 0,
    val lactatingCount: Int = 0,
    val todayMilkLiters: Double = 0.0,
    val activePregnancies: Int = 0,
    val pendingHealthCount: Int = 0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netProfit: Double = 0.0
)

class GoatFarmViewModel(
    private val repository: GoatFarmRepository,
    private val currentUserId: String,
    private val localGoatRepository: LocalGoatRepository? = null
) : ViewModel() {

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _isOperationLoading = MutableStateFlow(false)
    val isOperationLoading: StateFlow<Boolean> = _isOperationLoading.asStateFlow()

    // Local Room cache flow for offline goat reads
    val cachedLocalGoats: StateFlow<List<LocalGoat>> =
        (localGoatRepository?.allGoats ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    // 1. Goats Flow (syncs to local Room cache on updates)
    val goats: StateFlow<UiState<List<Goat>>> = repository.observeGoats(currentUserId)
        .onEach { list ->
            localGoatRepository?.let { localRepo ->
                try {
                    localRepo.replaceCacheFromCloud(list)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update local Room goat cache", e)
                }
            }
        }
        .map<List<Goat>, UiState<List<Goat>>> { list ->
            UiState.Success(list.sortedBy { it.tag })
        }
        .catch { error ->
            val root = unwrapError(error)
            Log.w(TAG, "Error observing goats", root)
            emit(UiState.Error(root.message ?: "Failed to observe herd"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 2. Breeding Records Flow
    val breedings: StateFlow<UiState<List<BreedingRecord>>> = repository.observeBreedings(currentUserId)
        .map<List<BreedingRecord>, UiState<List<BreedingRecord>>> { list ->
            UiState.Success(list.sortedByDescending { it.breedingDate })
        }
        .catch { error ->
            val root = unwrapError(error)
            Log.w(TAG, "Error observing breedings", root)
            emit(UiState.Error(root.message ?: "Failed to observe breeding records"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 3. Milk Logs Flow
    val milkLogs: StateFlow<UiState<List<MilkLog>>> = repository.observeMilkLogs(currentUserId)
        .map<List<MilkLog>, UiState<List<MilkLog>>> { list ->
            UiState.Success(list.sortedByDescending { it.date })
        }
        .catch { error ->
            val root = unwrapError(error)
            Log.w(TAG, "Error observing milk logs", root)
            emit(UiState.Error(root.message ?: "Failed to observe milk production"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 4. Health Records Flow
    val healthRecords: StateFlow<UiState<List<HealthRecord>>> = repository.observeHealthRecords(currentUserId)
        .map<List<HealthRecord>, UiState<List<HealthRecord>>> { list ->
            UiState.Success(list.sortedByDescending { it.administeredDate })
        }
        .catch { error ->
            val root = unwrapError(error)
            Log.w(TAG, "Error observing health records", root)
            emit(UiState.Error(root.message ?: "Failed to observe health records"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 5. Transactions Flow
    val transactions: StateFlow<UiState<List<FarmTransaction>>> = repository.observeTransactions(currentUserId)
        .map<List<FarmTransaction>, UiState<List<FarmTransaction>>> { list ->
            UiState.Success(list.sortedByDescending { it.date })
        }
        .catch { error ->
            val root = unwrapError(error)
            Log.w(TAG, "Error observing transactions", root)
            emit(UiState.Error(root.message ?: "Failed to observe finances"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 6. Farm Profile Flow
    val farmProfile: StateFlow<FarmProfile> = repository.observeFarmProfile(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = FarmProfile()
        )

    // 7. Combined Herd Dashboard Summary Flow
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        combine(
            repository.observeGoats(currentUserId),
            repository.observeBreedings(currentUserId),
            repository.observeMilkLogs(currentUserId),
            repository.observeHealthRecords(currentUserId),
            repository.observeTransactions(currentUserId)
        ) { goatsList, breedingsList, milkList, healthList, transList ->
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val currentMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

            val todayMilk = milkList.filter { it.date == todayStr }.sumOf { it.yieldLiters }
            val currentMonthMilk = milkList.filter { it.date.startsWith(currentMonthPrefix) }.sumOf { it.yieldLiters }
            val activePreg = breedingsList.count { it.status == "Confirmed Pregnant" || it.status == "Bred" }

            // Active health alerts: due within the next 7 days or overdue
            val sevenDaysLater = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
            val sevenDaysLaterStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(sevenDaysLater.time)
            val activeAlerts = healthList.count { it.nextDueDate.isNotBlank() && it.nextDueDate <= sevenDaysLaterStr }

            val inc = transList.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
            val exp = transList.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }

            DashboardSummary(
                totalGoats = goatsList.size,
                activeHealthAlerts = activeAlerts,
                currentMonthMilkLiters = currentMonthMilk,
                currentMonthName = currentMonthName,
                doeCount = goatsList.count { it.gender.equals("Doe", ignoreCase = true) },
                buckCount = goatsList.count { it.gender.equals("Buck", ignoreCase = true) },
                kidCount = goatsList.count { it.gender.equals("Wether", ignoreCase = true) || it.status.equals("Kid", ignoreCase = true) },
                lactatingCount = goatsList.count { it.status.equals("Lactating", ignoreCase = true) },
                todayMilkLiters = todayMilk,
                activePregnancies = activePreg,
                pendingHealthCount = activeAlerts,
                totalIncome = inc,
                totalExpense = exp,
                netProfit = inc - exp
            )
        },
        farmProfile
    ) { summary, profile ->
        summary.copy(
            farmName = profile.farmName,
            farmAddress = profile.address,
            ownerName = profile.ownerName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DashboardSummary()
    )

    // --- Farm Profile Actions ---

    fun updateFarmProfile(profile: FarmProfile, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.updateFarmProfile(currentUserId, profile)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Farm profile updated!")
                onComplete(true)
            } else {
                _userMessage.emit("Failed to update profile: ${result.exceptionOrNull()?.localizedMessage}")
                onComplete(false)
            }
        }
    }

    fun addMilkProduction(milk: MilkProduction, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.addMilkLog(
                currentUserId,
                MilkLog(
                    doeTag = milk.goatId,
                    date = milk.date,
                    yieldLiters = milk.quantityLiters
                )
            )
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Logged ${milk.quantityLiters}L milk for ${milk.goatId}")
                onComplete(true)
            } else {
                _userMessage.emit("Failed to record milk: ${result.exceptionOrNull()?.localizedMessage}")
                onComplete(false)
            }
        }
    }

    // --- Action Methods ---

    fun addGoat(goat: Goat, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.addGoat(currentUserId, goat)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Goat ${goat.name} (${goat.tag}) added successfully!")
                onComplete(true)
            } else {
                _userMessage.emit("Failed to add goat: ${result.exceptionOrNull()?.localizedMessage}")
                onComplete(false)
            }
        }
    }

    fun updateGoat(goatId: String, goat: Goat, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.updateGoat(currentUserId, goatId, goat)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Goat ${goat.tag} updated!")
                onComplete(true)
            } else {
                _userMessage.emit("Failed to update: ${result.exceptionOrNull()?.localizedMessage}")
                onComplete(false)
            }
        }
    }

    fun deleteGoat(goatId: String, tag: String) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.deleteGoat(currentUserId, goatId)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Goat $tag deleted.")
            } else {
                _userMessage.emit("Delete failed: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun addMilkLog(log: MilkLog, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.addMilkLog(currentUserId, log)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Logged ${log.yieldLiters}L milk for ${log.doeTag}.")
                onComplete(true)
            } else {
                _userMessage.emit("Failed to log milk: ${result.exceptionOrNull()?.localizedMessage}")
                onComplete(false)
            }
        }
    }

    fun deleteMilkLog(logId: String) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.deleteMilkLog(currentUserId, logId)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Milk log removed.")
            } else {
                _userMessage.emit("Delete failed: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun addBreeding(breeding: BreedingRecord, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.addBreeding(currentUserId, breeding)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Breeding record for ${breeding.doeTag} saved.")
                onComplete(true)
            } else {
                _userMessage.emit("Failed to save breeding: ${result.exceptionOrNull()?.localizedMessage}")
                onComplete(false)
            }
        }
    }

    fun updateBreeding(breedingId: String, breeding: BreedingRecord, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.updateBreeding(currentUserId, breedingId, breeding)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Breeding status updated.")
                onComplete(true)
            } else {
                _userMessage.emit("Update failed: ${result.exceptionOrNull()?.localizedMessage}")
                onComplete(false)
            }
        }
    }

    fun deleteBreeding(breedingId: String) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.deleteBreeding(currentUserId, breedingId)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Breeding record deleted.")
            }
        }
    }

    fun addHealthRecord(record: HealthRecord, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.addHealthRecord(currentUserId, record)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Health treatment recorded for ${record.goatTag}.")
                onComplete(true)
            } else {
                _userMessage.emit("Failed to record health: ${result.exceptionOrNull()?.localizedMessage}")
                onComplete(false)
            }
        }
    }

    fun deleteHealthRecord(recordId: String) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.deleteHealthRecord(currentUserId, recordId)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Health record removed.")
            }
        }
    }

    fun addTransaction(transaction: FarmTransaction, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.addTransaction(currentUserId, transaction)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Transaction of $${transaction.amount} recorded.")
                onComplete(true)
            } else {
                _userMessage.emit("Failed to save transaction: ${result.exceptionOrNull()?.localizedMessage}")
                onComplete(false)
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.deleteTransaction(currentUserId, transactionId)
            _isOperationLoading.value = false
            if (result.isSuccess) {
                _userMessage.emit("Transaction deleted.")
            }
        }
    }

    fun addFinanceRecord(record: FinanceRecord, onComplete: (Boolean) -> Unit = {}) =
        addTransaction(record, onComplete)

    fun deleteFinanceRecord(recordId: String) =
        deleteTransaction(recordId)

    private fun unwrapError(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    companion object {
        private const val TAG = "GoatFarmVM"
    }
}
