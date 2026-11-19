package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BreedingRecord
import com.example.data.model.FinanceRecord
import com.example.data.model.Goat
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Dedicated repository for performing CRUD operations on the [Goat] entity in Firestore.
 * Integrates directly with the multi-tenant database configuration defined in firebase_applet_config.xml.
 */
class GoatRepository(private val db: FirebaseFirestore) {

    /**
     * Secondary constructor that initializes Firestore with the configured project database ID.
     */
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun goatsCollection(userId: String) =
        db.collection("users").document(userId).collection("goats")

    /**
     * Observes real-time updates for all goats belonging to [userId].
     *
     * @param userId The authenticated farmer's Firebase UID.
     * @return A [Flow] emitting the updated list of goats whenever data changes.
     */
    fun observeGoats(userId: String): Flow<List<Goat>> {
        return goatsCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Goat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    /**
     * Fetches a single goat by its [goatId].
     *
     * @param userId The authenticated farmer's Firebase UID.
     * @param goatId The Firestore document ID of the goat.
     * @return [Result] containing the [Goat] object if found, null otherwise.
     */
    suspend fun getGoatById(userId: String, goatId: String): Result<Goat?> {
        val docRef = goatsCollection(userId).document(goatId)
        return try {
            val snapshot = docRef.get().await()
            val goat = snapshot.toObject(Goat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(goat)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Creates a new goat document in Firestore under the user's subcollection.
     *
     * @param userId The authenticated farmer's Firebase UID.
     * @param goat The [Goat] entity to create.
     * @return [Result] containing the generated document ID upon success.
     */
    suspend fun addGoat(userId: String, goat: Goat): Result<String> {
        val docRef = goatsCollection(userId).document()
        return try {
            val payload = goat.toCreateMap(userId)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing goat document with sanitized fields and a server-side updatedAt timestamp.
     *
     * @param userId The authenticated farmer's Firebase UID.
     * @param goatId The Firestore document ID to update.
     * @param goat The updated [Goat] entity.
     * @return [Result] representing operation outcome.
     */
    suspend fun updateGoat(userId: String, goatId: String, goat: Goat): Result<Unit> {
        val docRef = goatsCollection(userId).document(goatId)
        return try {
            val payload = goat.toUpdateMap()
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Deletes a goat document from Firestore.
     *
     * @param userId The authenticated farmer's Firebase UID.
     * @param goatId The Firestore document ID to delete.
     * @return [Result] representing operation outcome.
     */
    suspend fun deleteGoat(userId: String, goatId: String): Result<Unit> {
        val docRef = goatsCollection(userId).document(goatId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Health Logs for Goat ---

    private fun healthCollection(userId: String) =
        db.collection("users").document(userId).collection("health_records")

    /**
     * Observes real-time health log events specifically for [goatTag].
     */
    fun observeHealthLogsForGoat(userId: String, goatTag: String): Flow<List<com.example.data.model.HealthLog>> {
        return healthCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val tag = doc.getString("goatTag") ?: ""
                    if (tag.equals(goatTag, ignoreCase = true)) {
                        val recordType = doc.getString("recordType") ?: "Checkup"
                        val treatment = doc.getString("treatment") ?: ""
                        val notes = doc.getString("notes") ?: ""
                        val desc = when {
                            notes.isNotBlank() && treatment.isNotBlank() && notes != treatment -> "$treatment - $notes"
                            treatment.isNotBlank() -> treatment
                            else -> notes
                        }
                        com.example.data.model.HealthLog(
                            id = doc.id,
                            userId = doc.getString("userId") ?: userId,
                            goatTag = tag,
                            date = doc.getString("administeredDate") ?: "",
                            type = recordType,
                            description = desc,
                            nextDueDate = doc.getString("nextDueDate") ?: "",
                            createdAt = doc.getTimestamp("createdAt"),
                            updatedAt = doc.getTimestamp("updatedAt")
                        )
                    } else null
                }.sortedByDescending { it.date }
            }
    }

    /**
     * Records a new HealthLog for a goat into Firestore.
     */
    suspend fun addHealthLog(userId: String, log: com.example.data.model.HealthLog): Result<String> {
        val docRef = healthCollection(userId).document()
        return try {
            val payload = log.toCreateMap(userId)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Deletes a health log record from Firestore.
     */
    suspend fun deleteHealthLog(userId: String, logId: String): Result<Unit> {
        val docRef = healthCollection(userId).document(logId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Milk Production for Specific Goat ---

    private fun milkCollection(userId: String) =
        db.collection("users").document(userId).collection("milk_logs")

    /**
     * Stores a new [MilkProduction] log for a specific goat in Firestore.
     */
    suspend fun storeMilkProduction(userId: String, milkProduction: com.example.data.model.MilkProduction): Result<String> {
        val docRef = milkCollection(userId).document()
        return try {
            val payload = milkProduction.toCreateMap(userId)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Observes real-time [MilkProduction] logs for a specific goat.
     */
    fun observeMilkProductionForGoat(userId: String, goatId: String): Flow<List<com.example.data.model.MilkProduction>> {
        return milkCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val doeTag = doc.getString("doeTag") ?: doc.getString("goatId") ?: ""
                    if (doeTag.equals(goatId, ignoreCase = true)) {
                        val yield = doc.getDouble("yieldLiters") ?: doc.getDouble("quantityLiters") ?: 0.0
                        com.example.data.model.MilkProduction(
                            id = doc.id,
                            userId = doc.getString("userId") ?: userId,
                            goatId = doeTag,
                            date = doc.getString("date") ?: "",
                            quantityLiters = yield,
                            createdAt = doc.getTimestamp("createdAt"),
                            updatedAt = doc.getTimestamp("updatedAt")
                        )
                    } else null
                }.sortedByDescending { it.date }
            }
    }

    /**
     * Retrieves [MilkProduction] logs once for a specific goat.
     */
    suspend fun getMilkProductionForGoat(userId: String, goatId: String): Result<List<com.example.data.model.MilkProduction>> {
        return try {
            val snapshot = milkCollection(userId).get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                val doeTag = doc.getString("doeTag") ?: doc.getString("goatId") ?: ""
                if (doeTag.equals(goatId, ignoreCase = true)) {
                    val yield = doc.getDouble("yieldLiters") ?: doc.getDouble("quantityLiters") ?: 0.0
                    com.example.data.model.MilkProduction(
                        id = doc.id,
                        userId = doc.getString("userId") ?: userId,
                        goatId = doeTag,
                        date = doc.getString("date") ?: "",
                        quantityLiters = yield,
                        createdAt = doc.getTimestamp("createdAt"),
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                } else null
            }.sortedByDescending { it.date }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$userId/milk_logs")
            Result.failure(e)
        }
    }

    /**
     * Observes all [MilkProduction] logs for the entire herd.
     */
    fun observeAllMilkProduction(userId: String): Flow<List<com.example.data.model.MilkProduction>> {
        return milkCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val doeTag = doc.getString("doeTag") ?: doc.getString("goatId") ?: ""
                    val yield = doc.getDouble("yieldLiters") ?: doc.getDouble("quantityLiters") ?: 0.0
                    com.example.data.model.MilkProduction(
                        id = doc.id,
                        userId = doc.getString("userId") ?: userId,
                        goatId = doeTag,
                        date = doc.getString("date") ?: "",
                        quantityLiters = yield,
                        createdAt = doc.getTimestamp("createdAt"),
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                }.sortedByDescending { it.date }
            }
    }

    /**
     * Deletes a milk production record.
     */
    suspend fun deleteMilkProduction(userId: String, logId: String): Result<Unit> {
        val docRef = milkCollection(userId).document(logId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Farm Profile (Customizable Farm Name & Address) ---

    /**
     * Observes the customizable farm profile for the authenticated farmer.
     */
    fun observeFarmProfile(userId: String): Flow<com.example.data.model.FarmProfile> {
        return db.collection("users").document(userId)
            .snapshots()
            .map { doc ->
                if (doc.exists()) {
                    val farmName = doc.getString("farmName") ?: "Khulana Goat Farm"
                    val address = doc.getString("address") ?: "Khulna, Bangladesh"
                    val ownerName = doc.getString("ownerName") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    val notes = doc.getString("notes") ?: ""
                    com.example.data.model.FarmProfile(
                        farmName = farmName,
                        ownerName = ownerName,
                        address = address,
                        phone = phone,
                        notes = notes,
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                } else {
                    com.example.data.model.FarmProfile()
                }
            }
    }

    /**
     * Updates the farmer's custom farm name, address, owner name, and phone.
     */
    suspend fun updateFarmProfile(userId: String, profile: com.example.data.model.FarmProfile): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val payload = profile.toMap()
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Breeding Records for Specific Goat ---

    private fun breedingsCollection(userId: String) =
        db.collection("users").document(userId).collection("breedings")

    /**
     * Stores a new [BreedingRecord] (tracking mating date, sire/dam IDs, and expected delivery date) in Firestore.
     */
    suspend fun storeBreedingRecord(userId: String, record: BreedingRecord): Result<String> {
        val docRef = breedingsCollection(userId).document()
        return try {
            val payload = record.toCreateMap(userId)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Observes real-time [BreedingRecord] entries for a specific goat (as Doe/Dam or Buck/Sire).
     */
    fun observeBreedingRecordsForGoat(userId: String, goatTag: String): Flow<List<BreedingRecord>> {
        return breedingsCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val doeTag = doc.getString("doeTag") ?: ""
                    val buckTag = doc.getString("buckTag") ?: ""
                    if (doeTag.equals(goatTag, ignoreCase = true) || buckTag.equals(goatTag, ignoreCase = true)) {
                        doc.toObject(BreedingRecord::class.java)
                    } else null
                }.sortedByDescending { it.breedingDate }
            }
    }

    /**
     * Retrieves all [BreedingRecord] entries for a specific goat once.
     */
    suspend fun getBreedingRecordsForGoat(userId: String, goatTag: String): Result<List<BreedingRecord>> {
        return try {
            val snapshot = breedingsCollection(userId).get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                val doeTag = doc.getString("doeTag") ?: ""
                val buckTag = doc.getString("buckTag") ?: ""
                if (doeTag.equals(goatTag, ignoreCase = true) || buckTag.equals(goatTag, ignoreCase = true)) {
                    doc.toObject(BreedingRecord::class.java)
                } else null
            }.sortedByDescending { it.breedingDate }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$userId/breedings")
            Result.failure(e)
        }
    }

    /**
     * Deletes a breeding record from Firestore.
     */
    suspend fun deleteBreedingRecord(userId: String, recordId: String): Result<Unit> {
        val docRef = breedingsCollection(userId).document(recordId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Finance Records (Daily Expenses & Income Tracking) ---

    private fun transactionsCollection(userId: String) =
        db.collection("users").document(userId).collection("transactions")

    /**
     * Stores a new [FinanceRecord] tracking daily expense or income into Firestore.
     */
    suspend fun storeFinanceRecord(userId: String, record: FinanceRecord): Result<String> {
        val docRef = transactionsCollection(userId).document()
        return try {
            val payload = record.toCreateMap(userId)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Observes real-time [FinanceRecord] logs for the farm.
     */
    fun observeFinanceRecords(userId: String): Flow<List<FinanceRecord>> {
        return transactionsCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FinanceRecord::class.java)
                }.sortedByDescending { it.date }
            }
    }

    /**
     * Retrieves all [FinanceRecord] logs once.
     */
    suspend fun getFinanceRecords(userId: String): Result<List<FinanceRecord>> {
        return try {
            val snapshot = transactionsCollection(userId).get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FinanceRecord::class.java)
            }.sortedByDescending { it.date }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$userId/transactions")
            Result.failure(e)
        }
    }

    /**
     * Deletes a financial transaction record.
     */
    suspend fun deleteFinanceRecord(userId: String, recordId: String): Result<Unit> {
        val docRef = transactionsCollection(userId).document(recordId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
