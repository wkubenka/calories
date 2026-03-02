package com.astute.calories.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astute.calories.data.local.entity.CachedFood

@Dao
interface FoodCacheDao {
    @Query("SELECT * FROM cached_foods WHERE barcode = :barcode")
    suspend fun getByBarcode(barcode: String): CachedFood?

    @Query("SELECT * FROM cached_foods WHERE searchQuery = :query ORDER BY lastAccessed DESC LIMIT :limit")
    suspend fun searchByName(query: String, limit: Int = 20): List<CachedFood>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(food: CachedFood)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(foods: List<CachedFood>)

    @Query("DELETE FROM cached_foods WHERE lastAccessed < :before")
    suspend fun deleteOlderThan(before: Long)
}
