package com.goodbp.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
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
    @Update suspend fun update(m: Medicine)
    @Query("DELETE FROM medicines WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM medicines ORDER BY id") fun all(): Flow<List<Medicine>>
    @Query("SELECT * FROM medicines ORDER BY id") suspend fun allOnce(): List<Medicine>
    @Query("SELECT * FROM medicines WHERE id = :id") suspend fun get(id: Long): Medicine?
    @Insert suspend fun insertConfirmation(c: MedConfirmation): Long
    @Query("SELECT * FROM med_confirmations WHERE dueAt >= :since ORDER BY dueAt")
    fun confirmationsSince(since: Long): Flow<List<MedConfirmation>>
    @Query("SELECT COUNT(*) FROM med_confirmations WHERE medicineId = :medId AND dueAt = :dueAt")
    suspend fun countFor(medId: Long, dueAt: Long): Int
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
