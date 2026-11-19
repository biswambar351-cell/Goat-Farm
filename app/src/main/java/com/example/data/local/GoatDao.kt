package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for performing reactive and suspending operations
 * on the local `goats` Room database table.
 */
@Dao
interface GoatDao {

    @Query("SELECT * FROM goats ORDER BY name ASC")
    fun getAllGoats(): Flow<List<Goat>>

    @Query("SELECT * FROM goats WHERE id = :id")
    fun getGoatById(id: Int): Flow<Goat?>

    @Query("SELECT * FROM goats WHERE health_status = :healthStatus ORDER BY name ASC")
    fun getGoatsByHealthStatus(healthStatus: String): Flow<List<Goat>>

    @Query("SELECT * FROM goats WHERE breed = :breed ORDER BY name ASC")
    fun getGoatsByBreed(breed: String): Flow<List<Goat>>

    @Query("SELECT * FROM goats WHERE name LIKE '%' || :query || '%' OR breed LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchGoats(query: String): Flow<List<Goat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoat(goat: Goat): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goats: List<Goat>)

    @Update
    suspend fun updateGoat(goat: Goat)

    @Delete
    suspend fun deleteGoat(goat: Goat)

    @Query("DELETE FROM goats WHERE id = :id")
    suspend fun deleteGoatById(id: Int)

    @Query("DELETE FROM goats")
    suspend fun deleteAllGoats()
}
