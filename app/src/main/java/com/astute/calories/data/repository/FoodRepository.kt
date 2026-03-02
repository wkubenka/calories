package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.FoodCacheDao
import com.astute.calories.data.local.entity.CachedFood
import com.astute.calories.data.remote.OpenFoodFactsApi
import com.astute.calories.data.remote.dto.ProductDto
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val foodCacheDao: FoodCacheDao,
    private val api: OpenFoodFactsApi
) {
    suspend fun searchFoods(query: String): List<CachedFood> {
        // Return cached hits first
        val cached = foodCacheDao.searchByName(query)
        if (cached.isNotEmpty()) return cached

        // Fall back to API
        return try {
            val response = api.searchByName(query)
            val foods = response.products
                ?.mapNotNull { it.toCachedFood() }
                ?: emptyList()
            foodCacheDao.upsertAll(foods)
            foods
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun lookupByBarcode(barcode: String): CachedFood? {
        // Check cache first
        foodCacheDao.getByBarcode(barcode)?.let { return it }

        // Fall back to API
        return try {
            val response = api.getByBarcode(barcode)
            if (response.status == 1 && response.product != null) {
                val food = response.product.toCachedFood()
                food?.let { foodCacheDao.upsert(it) }
                food
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun ProductDto.toCachedFood(): CachedFood? {
        val barcode = code ?: return null
        val name = productName ?: return null
        if (name.isBlank()) return null

        return CachedFood(
            barcode = barcode,
            name = name,
            calories = nutriments?.energyKcal100g?.toInt() ?: 0,
            proteinG = nutriments?.proteins100g ?: 0f,
            carbsG = nutriments?.carbohydrates100g ?: 0f,
            fatG = nutriments?.fat100g ?: 0f,
            servingSizeG = servingSize?.filter { it.isDigit() || it == '.' }?.toFloatOrNull(),
            imageUrl = imageUrl,
            lastAccessed = Instant.now()
        )
    }
}
