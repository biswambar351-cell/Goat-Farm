package com.example.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.Goat
import com.example.data.local.GoatDao
import com.example.data.local.GoatFarmDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GoatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: GoatFarmDatabase
    private lateinit var goatDao: GoatDao
    private lateinit var viewModel: GoatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GoatFarmDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        goatDao = database.goatDao()
        viewModel = GoatViewModel(goatDao)
    }

    @After
    fun teardown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun addAndFetchGoats_updatesUiStateSuccess() = runTest(testDispatcher) {
        val collectorJob = launch { viewModel.uiState.collect {} }

        var insertedId = 0L
        viewModel.addGoat(
            name = "Bella",
            birthDate = "2024-03-15",
            breed = "Boer",
            healthStatus = "Healthy"
        ) { result ->
            insertedId = result.getOrDefault(0L)
        }
        advanceUntilIdle()

        assertTrue(insertedId > 0L)
        val state = viewModel.uiState.filterIsInstance<GoatUiState.Success>().first { it.goats.size == 1 }
        assertEquals("Bella", state.goats.first().name)
        assertEquals("Boer", state.goats.first().breed)
        assertEquals("Healthy", state.goats.first().healthStatus)

        collectorJob.cancel()
    }

    @Test
    fun deleteGoatAndDeleteById_removesFromUiState() = runTest(testDispatcher) {
        val collectorJob = launch { viewModel.uiState.collect {} }

        viewModel.addGoat("Luna", "2023-05-10", "Saanen", "Healthy")
        viewModel.addGoat("Rocky", "2022-11-01", "Boer", "Needs Checkup")
        advanceUntilIdle()

        val initial = viewModel.uiState.filterIsInstance<GoatUiState.Success>().first { it.goats.size == 2 }
        assertEquals(2, initial.goats.size)

        val luna = initial.goats.first { it.name == "Luna" }
        viewModel.deleteGoat(luna)
        advanceUntilIdle()

        val afterFirstDelete = viewModel.uiState.filterIsInstance<GoatUiState.Success>().first { it.goats.size == 1 }
        assertEquals("Rocky", afterFirstDelete.goats.first().name)

        viewModel.deleteGoatById(afterFirstDelete.goats.first().id)
        advanceUntilIdle()

        val afterSecondDelete = viewModel.uiState.filterIsInstance<GoatUiState.Success>().first { it.goats.isEmpty() }
        assertTrue(afterSecondDelete.goats.isEmpty())

        collectorJob.cancel()
    }

    @Test
    fun searchAndHealthFilter_filtersGoatsReactively() = runTest(testDispatcher) {
        val collectorJob = launch { viewModel.uiState.collect {} }

        viewModel.addGoat(Goat(name = "Daisy", birthDate = "2024-01-20", breed = "Nubian", healthStatus = "Healthy"))
        viewModel.addGoat(Goat(name = "Milo", birthDate = "2024-06-11", breed = "Boer", healthStatus = "Under Treatment"))
        advanceUntilIdle()

        viewModel.setHealthStatusFilter("Under Treatment")
        advanceUntilIdle()

        val filteredByHealth = viewModel.uiState.filterIsInstance<GoatUiState.Success>().first {
            it.goats.size == 1 && it.goats.first().name == "Milo"
        }
        assertEquals("Milo", filteredByHealth.goats.first().name)

        viewModel.setHealthStatusFilter("All")
        viewModel.setSearchQuery("Nubian")
        advanceUntilIdle()

        val filteredByBreed = viewModel.uiState.filterIsInstance<GoatUiState.Success>().first {
            it.goats.size == 1 && it.goats.first().name == "Daisy"
        }
        assertEquals("Daisy", filteredByBreed.goats.first().name)

        collectorJob.cancel()
    }

    @Test
    fun addGoatWithBlankName_setsErrorMessage() = runTest(testDispatcher) {
        var result: Result<Long>? = null
        viewModel.addGoat(
            name = "   ",
            birthDate = "2025-01-01",
            breed = "Boer",
            healthStatus = "Healthy"
        ) { res ->
            result = res
        }

        assertNotNull(result)
        assertTrue(result!!.isFailure)
        assertEquals("Goat name cannot be empty.", viewModel.errorMessage.value)
    }
}
