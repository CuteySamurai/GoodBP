package com.goodbp.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {
    @Insert suspend fun insert(r: Reading): Long
    @Query("DELETE FROM readings WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM readings ORDER BY takenAt DESC")
    fun all(): Flow<List<Reading>>
    @Query("SELECT * FROM readings WHERE takenAt >= :since ORDER BY takenAt")
    suspend fun since(since: Long): List<Reading>
}

@Dao
interface MedicineDao {
    @Insert suspend fun insert(m: Medicine): Long
    @Query("SELECT * FROM medicines") fun all(): Flow<List<Medicine>>
    @Insert suspend fun insertConfirmation(c: MedConfirmation): Long
}

@Database(entities = [Reading::class, Medicine::class, MedConfirmation::class], version = 1)
abstract class GoodBpDatabase : RoomDatabase() {
    abstract fun readings(): ReadingDao
    abstract fun medicines(): MedicineDao

    companion object {
        @Volatile private var instance: GoodBpDatabase? = null
        fun get(context: Context): GoodBpDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, GoodBpDatabase::class.java, "goodbp.db")
                .build().also { instance = it }
        }
    }
}
