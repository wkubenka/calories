package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.FoodCacheDao
import com.astute.calories.data.local.entity.CachedFood
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val foodCacheDao: FoodCacheDao
) {
    suspend fun getCachedFood(barcode: String): CachedFood? =
        foodCacheDao.getByBarcode(barcode)

    suspend fun searchCachedFoods(query: String): List<CachedFood> =
        foodCacheDao.searchByName(query)

    suspend fun cacheFood(food: CachedFood) =
        foodCacheDao.upsert(food)

    suspend fun cacheFoods(foods: List<CachedFood>) =
        foodCacheDao.upsertAll(foods)
}
