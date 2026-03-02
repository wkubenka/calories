package com.astute.calories.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.astute.calories.data.local.entity.LogEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM log_entries WHERE date = :date ORDER BY addedAt ASC")
    fun getEntriesForDate(date: LocalDate): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries WHERE date = :date AND mealCategory = :category ORDER BY addedAt ASC")
    fun getEntriesForDateAndCategory(date: LocalDate, category: String): Flow<List<LogEntry>>

    @Query("SELECT SUM(calories) FROM log_entries WHERE date = :date")
    fun getTotalCaloriesForDate(date: LocalDate): Flow<Int?>

    @Insert
    suspend fun insert(entry: LogEntry): Long

    @Update
    suspend fun update(entry: LogEntry)

    @Delete
    suspend fun delete(entry: LogEntry)

    @Query("DELETE FROM log_entries WHERE date = :date")
    suspend fun deleteAllForDate(date: LocalDate)

    @Query("DELETE FROM log_entries WHERE date < :date")
    suspend fun deleteEntriesBefore(date: LocalDate)
}
