package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity representing a goat stored in the local SQLite database.
 */
@Entity(tableName = "goats")
data class Goat(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "birth_date")
    val birthDate: String,

    @ColumnInfo(name = "breed")
    val breed: String,

    @ColumnInfo(name = "health_status")
    val healthStatus: String
)
