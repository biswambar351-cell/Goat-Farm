package com.example.data.repository

import com.example.data.local.Goat
import com.example.data.local.GoatDao
import com.example.data.model.Goat as CloudGoat
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstracting local Room database access for [Goat] entities.
 */
class LocalGoatRepository(private val goatDao: GoatDao) {

    val allGoats: Flow<List<Goat>> = goatDao.getAllGoats()

    fun getGoatById(id: Int): Flow<Goat?> = goatDao.getGoatById(id)

    fun getGoatsByHealthStatus(healthStatus: String): Flow<List<Goat>> =
        goatDao.getGoatsByHealthStatus(healthStatus)

    fun getGoatsByBreed(breed: String): Flow<List<Goat>> =
        goatDao.getGoatsByBreed(breed)

    fun searchGoats(query: String): Flow<List<Goat>> =
        goatDao.searchGoats(query)

    suspend fun insert(goat: Goat): Long = goatDao.insertGoat(goat)

    suspend fun insertAll(goats: List<Goat>) = goatDao.insertAll(goats)

    suspend fun update(goat: Goat) = goatDao.updateGoat(goat)

    suspend fun delete(goat: Goat) = goatDao.deleteGoat(goat)

    suspend fun deleteById(id: Int) = goatDao.deleteGoatById(id)

    /**
     * Replaces local cached goats with a fresh snapshot from cloud records.
     */
    suspend fun replaceCacheFromCloud(cloudGoats: List<CloudGoat>) {
        val localEntities = cloudGoats.mapIndexed { index, cloud ->
            Goat(
                id = index + 1,
                name = cloud.name.ifBlank { cloud.tag },
                birthDate = cloud.birthDate,
                breed = cloud.breed,
                healthStatus = cloud.status
            )
        }
        goatDao.deleteAllGoats()
        if (localEntities.isNotEmpty()) {
            goatDao.insertAll(localEntities)
        }
    }
}
