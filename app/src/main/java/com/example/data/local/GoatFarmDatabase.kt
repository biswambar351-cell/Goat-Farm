package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database holder for local goat herd persistence and offline caching.
 */
@Database(entities = [Goat::class], version = 1, exportSchema = false)
abstract class GoatFarmDatabase : RoomDatabase() {

    abstract fun goatDao(): GoatDao

    companion object {
        @Volatile
        private var INSTANCE: GoatFarmDatabase? = null

        fun getInstance(context: Context): GoatFarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GoatFarmDatabase::class.java,
                    "goat_farm_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
