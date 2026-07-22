package com.astute.calories.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astute.calories.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightEntry)

    @Query("SELECT * FROM weight_entries WHERE date = :date")
    fun getForDate(date: LocalDate): Flow<WeightEntry?>

    @Query("SELECT * FROM weight_entries ORDER BY date DESC")
    fun getRecent(): Flow<List<WeightEntry>>

    @Query("DELETE FROM weight_entries WHERE date = :date")
    suspend fun delete(date: LocalDate)
}
