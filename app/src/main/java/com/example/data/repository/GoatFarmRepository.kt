package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BreedingRecord
import com.example.data.model.FarmTransaction
import com.example.data.model.Goat
import com.example.data.model.HealthRecord
import com.example.data.model.MilkLog
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GoatFarmRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // --- Goats ---
    fun observeGoats(userId: String): Flow<List<Goat>> {
        return db.collection("users").document(userId).collection("goats")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Goat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    suspend fun addGoat(userId: String, goat: Goat): Result<String> {
        val col = db.collection("users").document(userId).collection("goats")
        val docRef = col.document()
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

    suspend fun updateGoat(userId: String, goatId: String, goat: Goat): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("goats").document(goatId)
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

    suspend fun deleteGoat(userId: String, goatId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("goats").document(goatId)
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

    // --- Breedings ---
    fun observeBreedings(userId: String): Flow<List<BreedingRecord>> {
        return db.collection("users").document(userId).collection("breedings")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BreedingRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    suspend fun addBreeding(userId: String, breeding: BreedingRecord): Result<String> {
        val col = db.collection("users").document(userId).collection("breedings")
        val docRef = col.document()
        return try {
            val payload = breeding.toCreateMap(userId)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateBreeding(userId: String, breedingId: String, breeding: BreedingRecord): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("breedings").document(breedingId)
        return try {
            val payload = breeding.toUpdateMap()
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBreeding(userId: String, breedingId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("breedings").document(breedingId)
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

    // --- Milk Logs ---
    fun observeMilkLogs(userId: String): Flow<List<MilkLog>> {
        return db.collection("users").document(userId).collection("milk_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MilkLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    suspend fun addMilkLog(userId: String, log: MilkLog): Result<String> {
        val col = db.collection("users").document(userId).collection("milk_logs")
        val docRef = col.document()
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

    suspend fun deleteMilkLog(userId: String, logId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("milk_logs").document(logId)
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

    // --- Health Records ---
    fun observeHealthRecords(userId: String): Flow<List<HealthRecord>> {
        return db.collection("users").document(userId).collection("health_records")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(HealthRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    suspend fun addHealthRecord(userId: String, record: HealthRecord): Result<String> {
        val col = db.collection("users").document(userId).collection("health_records")
        val docRef = col.document()
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

    suspend fun deleteHealthRecord(userId: String, recordId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("health_records").document(recordId)
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

    // --- Financial Transactions ---
    fun observeTransactions(userId: String): Flow<List<FarmTransaction>> {
        return db.collection("users").document(userId).collection("transactions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FarmTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    suspend fun addTransaction(userId: String, transaction: FarmTransaction): Result<String> {
        val col = db.collection("users").document(userId).collection("transactions")
        val docRef = col.document()
        return try {
            val payload = transaction.toCreateMap(userId)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("transactions").document(transactionId)
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

    // --- Farm Profile ---
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
                    val logoType = doc.getString("logoType") ?: "default"
                    val logoEmblemId = doc.getString("logoEmblemId") ?: "goat_pets"
                    val logoColorIndex = doc.getLong("logoColorIndex")?.toInt() ?: 0
                    val logoInitials = doc.getString("logoInitials") ?: ""
                    val logoShape = doc.getString("logoShape") ?: "circle"
                    val logoImageBase64 = doc.getString("logoImageBase64") ?: ""
                    com.example.data.model.FarmProfile(
                        farmName = farmName,
                        ownerName = ownerName,
                        address = address,
                        phone = phone,
                        notes = notes,
                        logoType = logoType,
                        logoEmblemId = logoEmblemId,
                        logoColorIndex = logoColorIndex,
                        logoInitials = logoInitials,
                        logoShape = logoShape,
                        logoImageBase64 = logoImageBase64,
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                } else {
                    com.example.data.model.FarmProfile()
                }
            }
    }

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
}
