package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.FoodCacheDao
import com.astute.calories.data.local.entity.CachedFood
import com.astute.calories.data.remote.OpenFoodFactsApi
import com.astute.calories.data.remote.dto.ProductDto
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed class SearchResult {
    data class Success(val foods: List<CachedFood>) : SearchResult()
    data class Error(val message: String) : SearchResult()
}

@Singleton
class FoodRepository @Inject constructor(
    private val foodCacheDao: FoodCacheDao,
    private val api: OpenFoodFactsApi
) {
    suspend fun searchFoods(query: String): SearchResult {
        // Return cached hits first
        val cached = foodCacheDao.searchByName(query)
        if (cached.isNotEmpty()) return SearchResult.Success(cached)

        // Fall back to API
        return try {
            val response = api.searchByName(query)
            val foods = response.products
                ?.mapNotNull { it.toCachedFood() }
                ?: emptyList()
            if (foods.isNotEmpty()) foodCacheDao.upsertAll(foods)
            SearchResult.Success(foods)
        } catch (e: HttpException) {
            SearchResult.Error("Server error (${e.code()}). Please try again.")
        } catch (e: IOException) {
            SearchResult.Error("Network error. Check your connection and try again.")
        } catch (_: Exception) {
            SearchResult.Error("Something went wrong. Please try again.")
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

    /**
     * Extract grams from serving size strings like "30g", "1 cup (240g)", "250 ml (253 g)".
     * Prefers a number followed by "g" (grams). Falls back to the first number in the string.
     */
    private fun parseServingSizeGrams(servingSize: String?): Float? {
        if (servingSize.isNullOrBlank()) return null
        // Match a number directly before 'g' (e.g., "240g", "253 g", "30.5g")
        val gramsMatch = Regex("""(\d+\.?\d*)\s*g\b""", RegexOption.IGNORE_CASE).find(servingSize)
        if (gramsMatch != null) return gramsMatch.groupValues[1].toFloatOrNull()
        // Fall back to the first number in the string
        val firstNumber = Regex("""(\d+\.?\d*)""").find(servingSize)
        return firstNumber?.groupValues?.get(1)?.toFloatOrNull()
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
            servingSizeG = parseServingSizeGrams(servingSize),
            servingSizeLabel = servingSize?.trim()?.takeIf { it.isNotBlank() },
            imageUrl = imageUrl,
            lastAccessed = Instant.now()
        )
    }
}
