package com.astute.calories.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.astute.calories.data.local.entity.SavedMeal
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMealDao {
    @Query("SELECT * FROM saved_meals ORDER BY category, name")
    fun getAll(): Flow<List<SavedMeal>>

    @Query("SELECT * FROM saved_meals WHERE category = :category ORDER BY name")
    fun getByCategory(category: String): Flow<List<SavedMeal>>

    @Query("SELECT * FROM saved_meals WHERE id = :id")
    suspend fun getById(id: Long): SavedMeal?

    @Insert
    suspend fun insert(meal: SavedMeal): Long

    @Update
    suspend fun update(meal: SavedMeal)

    @Delete
    suspend fun delete(meal: SavedMeal)
}
