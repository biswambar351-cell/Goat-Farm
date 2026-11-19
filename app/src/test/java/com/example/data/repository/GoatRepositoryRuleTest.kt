package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Goat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class GoatRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addAndObserveGoat_withGoatRepository_succeeds() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repo = GoatRepository(firestore)

        val randomTag = "TAG-${UUID.randomUUID().toString().take(6)}"
        val goat = Goat(
            tag = randomTag,
            name = "Buttercup",
            breed = "Saanen",
            gender = "Doe",
            status = "Lactating",
            weightKg = 52.0
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addGoat(uid, goat)
        }
        assertTrue("Expected addGoat to succeed", createResult.isSuccess)
        val goatId = createResult.getOrThrow()
        assertNotNull(goatId)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getGoatById(uid, goatId)
        }
        assertTrue(fetched.isSuccess)
        val loadedGoat = fetched.getOrThrow()
        assertNotNull(loadedGoat)
        assertEquals(randomTag, loadedGoat?.tag)
        assertEquals("Buttercup", loadedGoat?.name)

        val goats = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.observeGoats(uid).first { list -> list.any { it.tag == randomTag } }
        }
        assertTrue(goats.any { it.tag == randomTag })
    }

    @Test
    fun addAndObserveHealthLogs_forGoat_succeeds() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repo = GoatRepository(firestore)

        val goatTag = "TAG-HL-${UUID.randomUUID().toString().take(4)}"
        val healthLog = com.example.data.model.HealthLog(
            goatTag = goatTag,
            date = "2026-11-08",
            type = "Vaccination",
            description = "CDT booster 2ml sub-Q"
        )

        val logResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addHealthLog(uid, healthLog)
        }
        assertTrue("Expected addHealthLog to succeed", logResult.isSuccess)
        val logId = logResult.getOrThrow()
        assertNotNull(logId)

        val logs = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.observeHealthLogsForGoat(uid, goatTag).first { it.isNotEmpty() }
        }
        assertEquals(1, logs.size)
        assertEquals("Vaccination", logs[0].type)
        assertEquals("2026-11-08", logs[0].date)
        assertEquals("CDT booster 2ml sub-Q", logs[0].description)
    }

    @Test
    fun storeAndObserveMilkProduction_forGoat_succeeds() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repo = GoatRepository(firestore)

        val goatId = "DOE-${UUID.randomUUID().toString().take(4)}"
        val milk = com.example.data.model.MilkProduction(
            goatId = goatId,
            date = "2026-11-09",
            quantityLiters = 3.2
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.storeMilkProduction(uid, milk)
        }
        assertTrue("Expected storeMilkProduction to succeed", result.isSuccess)

        val list = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.observeMilkProductionForGoat(uid, goatId).first { it.isNotEmpty() }
        }
        assertEquals(1, list.size)
        assertEquals(goatId, list[0].goatId)
        assertEquals("2026-11-09", list[0].date)
        assertEquals(3.2, list[0].quantityLiters, 0.001)
    }

    @Test
    fun updateAndObserveFarmProfile_succeeds() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repo = GoatRepository(firestore)

        val customProfile = com.example.data.model.FarmProfile(
            farmName = "Green Pastures Goat Dairy",
            ownerName = "Rahim Farmer",
            address = "Rupsha, Khulna",
            phone = "+8801700112233"
        )

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateFarmProfile(uid, customProfile)
        }
        assertTrue("Expected updateFarmProfile to succeed", updateResult.isSuccess)

        val observed = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.observeFarmProfile(uid).first { it.farmName == "Green Pastures Goat Dairy" }
        }
        assertEquals("Green Pastures Goat Dairy", observed.farmName)
        assertEquals("Rupsha, Khulna", observed.address)
        assertEquals("Rahim Farmer", observed.ownerName)
    }

    @Test
    fun storeAndObserveBreedingRecord_forGoat_succeeds() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repo = GoatRepository(firestore)

        val damId = "DOE-${UUID.randomUUID().toString().take(4)}"
        val sireId = "BUCK-${UUID.randomUUID().toString().take(4)}"
        val matingDate = "2026-11-01"
        val expectedDelivery = com.example.data.model.BreedingRecord.calculateExpectedDeliveryDate(matingDate)

        val breeding = com.example.data.model.BreedingRecord.create(
            damId = damId,
            sireId = sireId,
            matingDate = matingDate,
            expectedDeliveryDate = expectedDelivery,
            status = "Confirmed Pregnant",
            notes = "Ultrasound confirmed twins"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.storeBreedingRecord(uid, breeding)
        }
        assertTrue("Expected storeBreedingRecord to succeed", result.isSuccess)
        val recordId = result.getOrThrow()
        assertNotNull(recordId)

        // Observe as Dam
        val damRecords = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.observeBreedingRecordsForGoat(uid, damId).first { it.isNotEmpty() }
        }
        assertEquals(1, damRecords.size)
        assertEquals(damId, damRecords[0].damId)
        assertEquals(sireId, damRecords[0].sireId)
        assertEquals(matingDate, damRecords[0].matingDate)
        assertEquals(expectedDelivery, damRecords[0].expectedDeliveryDate)
        assertEquals("Confirmed Pregnant", damRecords[0].status)

        // Observe as Sire
        val sireRecords = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.observeBreedingRecordsForGoat(uid, sireId).first { it.isNotEmpty() }
        }
        assertEquals(1, sireRecords.size)
        assertEquals(damId, sireRecords[0].damId)
        assertEquals(sireId, sireRecords[0].sireId)
    }

    @Test
    fun storeAndObserveFinanceRecord_succeeds() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repo = GoatRepository(firestore)

        val uniqueDesc = "Feed purchase ${UUID.randomUUID().toString().take(6)}"
        val expenseRecord = com.example.data.model.FinanceRecord(
            type = "Expense",
            category = "Feed",
            amount = 145.50,
            date = "2026-11-09",
            description = uniqueDesc
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.storeFinanceRecord(uid, expenseRecord)
        }
        assertTrue("Expected storeFinanceRecord to succeed", result.isSuccess)
        val recordId = result.getOrThrow()
        assertNotNull(recordId)

        val records = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.observeFinanceRecords(uid).first { list ->
                list.any { it.description == uniqueDesc }
            }
        }
        val found = records.first { it.description == uniqueDesc }
        assertEquals("Expense", found.type)
        assertEquals("Feed", found.category)
        assertEquals(145.50, found.amount, 0.001)
        assertEquals("2026-11-09", found.date)
        assertEquals("2026-11", found.yearMonth)
        assertTrue(found.isExpense)
    }

    companion object {
        private const val TEST_USER_EMAIL = "goat_repo_test@example.com"
        private const val DEFAULT_TIMEOUT_MS = 8000L
    }
}
