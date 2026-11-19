package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BreedingRecord
import com.example.data.model.Goat
import com.example.data.model.MilkLog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class GoatFarmRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addGoat_validPayload_createsDocumentAndReturnsId() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = GoatFarmRepository(firestore)

    val randomTag = "TAG-${UUID.randomUUID().toString().take(6)}"
    val goat = Goat(
      tag = randomTag,
      name = "Bella",
      breed = "Nubian",
      gender = "Doe",
      status = "Active",
      weightKg = 45.5
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.addGoat(uid, goat)
    }
    assertTrue("Expected addGoat to succeed", result.isSuccess)
    val goatId = result.getOrThrow()
    assertNotNull(goatId)
    assertTrue(goatId.isNotBlank())
  }

  @Test
  fun observeGoats_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = GoatFarmRepository(firestore)

    val randomTag = "TAG-${UUID.randomUUID().toString().take(6)}"
    val goat = Goat(
      tag = randomTag,
      name = "Daisy",
      breed = "Alpine",
      gender = "Doe",
      status = "Active"
    )

    val goatId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.addGoat(uid, goat).getOrThrow()
    }

    val emittedList = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeGoats(uid).first { list -> list.any { it.id == goatId } }
    }
    assertTrue(emittedList.any { it.id == goatId })
  }

  @Test
  fun observeGoats_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = GoatFarmRepository(firestore)

    var caught = false
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeGoats("unauth_user_id").first()
      }
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      if (firestoreException != null && firestoreException.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        caught = true
      }
    }
    assertTrue("Expected PERMISSION_DENIED exception for unauthenticated observer", caught)
  }

  @Test
  fun addMilkLogAndBreeding_validPayload_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = GoatFarmRepository(firestore)

    val milkLog = MilkLog(
      doeTag = "TAG-001",
      date = "2026-11-08",
      session = "Morning",
      yieldLiters = 2.8
    )
    val milkResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.addMilkLog(uid, milkLog)
    }
    assertTrue("Expected addMilkLog to succeed", milkResult.isSuccess)

    val breeding = BreedingRecord(
      doeTag = "TAG-001",
      buckTag = "BUCK-01",
      breedingDate = "2026-11-01",
      expectedKiddingDate = "2027-03-31",
      status = "Bred"
    )
    val breedingResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.addBreeding(uid, breeding)
    }
    assertTrue("Expected addBreeding to succeed", breedingResult.isSuccess)
  }

  companion object {
    private const val ALICE_EMAIL = "alice@goatfarm.test"
    private const val DEFAULT_TIMEOUT_MS = 8000L
    private const val FLOW_TIMEOUT_MS = 5000L
  }
}
