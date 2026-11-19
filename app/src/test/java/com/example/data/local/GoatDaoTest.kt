package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.LocalGoatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoatDaoTest {

    private lateinit var database: GoatFarmDatabase
    private lateinit var goatDao: GoatDao
    private lateinit var repository: LocalGoatRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GoatFarmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        goatDao = database.goatDao()
        repository = LocalGoatRepository(goatDao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveGoat_returnsInsertedEntity() = runTest {
        val goat = Goat(
            name = "Bella",
            birthDate = "2024-03-15",
            breed = "Boer",
            healthStatus = "Healthy"
        )

        val insertedId = repository.insert(goat).toInt()
        assertTrue(insertedId > 0)

        val fetched = repository.getGoatById(insertedId).first()
        assertNotNull(fetched)
        assertEquals("Bella", fetched?.name)
        assertEquals("2024-03-15", fetched?.birthDate)
        assertEquals("Boer", fetched?.breed)
        assertEquals("Healthy", fetched?.healthStatus)
    }

    @Test
    fun filterByHealthStatusAndBreed_returnsMatchingGoats() = runTest {
        repository.insertAll(
            listOf(
                Goat(name = "Luna", birthDate = "2023-05-10", breed = "Saanen", healthStatus = "Healthy"),
                Goat(name = "Rocky", birthDate = "2022-11-01", breed = "Boer", healthStatus = "Needs Checkup"),
                Goat(name = "Daisy", birthDate = "2024-01-20", breed = "Saanen", healthStatus = "Under Treatment")
            )
        )

        val all = repository.allGoats.first()
        assertEquals(3, all.size)

        val saanens = repository.getGoatsByBreed("Saanen").first()
        assertEquals(2, saanens.size)

        val needsCheckup = repository.getGoatsByHealthStatus("Needs Checkup").first()
        assertEquals(1, needsCheckup.size)
        assertEquals("Rocky", needsCheckup.first().name)
    }

    @Test
    fun updateAndDeleteGoat_persistsChanges() = runTest {
        val id = repository.insert(
            Goat(name = "Milo", birthDate = "2025-02-12", breed = "Nubian", healthStatus = "Under Treatment")
        ).toInt()

        val existing = repository.getGoatById(id).first()!!
        repository.update(existing.copy(healthStatus = "Healthy"))

        val updated = repository.getGoatById(id).first()
        assertEquals("Healthy", updated?.healthStatus)

        repository.deleteById(id)
        val afterDelete = repository.getGoatById(id).first()
        assertNull(afterDelete)
    }
}
