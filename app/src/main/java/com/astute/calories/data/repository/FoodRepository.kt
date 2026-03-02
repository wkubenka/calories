package com.astute.calories.data.repository

import android.util.Log
import com.astute.calories.data.local.dao.FoodCacheDao
import com.astute.calories.data.local.entity.CachedFood
import com.astute.calories.data.remote.FdaFoodApi
import com.astute.calories.data.remote.OpenFoodFactsApi
import com.astute.calories.data.remote.dto.FdaFoodDto
import com.astute.calories.data.remote.dto.ProductDto
import com.astute.calories.di.FdaApiKey
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
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
    private val openFoodFactsApi: OpenFoodFactsApi,
    private val fdaApi: FdaFoodApi,
    @FdaApiKey private val fdaApiKey: String
) {
    suspend fun searchFoods(query: String): SearchResult {
        // Return cached hits first
        val cached = foodCacheDao.searchByName(query)
        if (cached.isNotEmpty()) return SearchResult.Success(cached)

        // Fall back to FDA API
        return try {
            val response = fdaApi.searchFoods(apiKey = fdaApiKey, query = query)
            val foods = response.foods
                ?.mapNotNull { it.toCachedFood(query) }
                ?: emptyList()
            if (foods.isNotEmpty()) foodCacheDao.upsertAll(foods)
            SearchResult.Success(foods)
        } catch (e: HttpException) {
            Log.e("FoodRepository", "HTTP error searching foods", e)
            SearchResult.Error("Server error (${e.code()}). Please try again.")
        } catch (e: JsonDataException) {
            Log.e("FoodRepository", "JSON parse error searching foods", e)
            SearchResult.Error("Data format error: ${e.message}")
        } catch (e: JsonEncodingException) {
            Log.e("FoodRepository", "JSON encoding error searching foods", e)
            SearchResult.Error("Data format error: ${e.message}")
        } catch (e: IOException) {
            Log.e("FoodRepository", "IO error searching foods", e)
            SearchResult.Error("Network error: ${e.javaClass.simpleName} - ${e.message}")
        } catch (e: Exception) {
            Log.e("FoodRepository", "Unexpected error searching foods", e)
            SearchResult.Error("Error: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    suspend fun lookupByBarcode(barcode: String): CachedFood? {
        // Check cache first
        foodCacheDao.getByBarcode(barcode)?.let { return it }

        // Fall back to OpenFoodFacts API (FDA has no barcode endpoint)
        return try {
            val response = openFoodFactsApi.getByBarcode(barcode)
            if (response.status == 1 && response.product != null) {
                val food = response.product.toCachedFood()
                food?.let { foodCacheDao.upsert(it) }
                food
            } else null
        } catch (_: Exception) {
            null
        }
    }

    // --- FDA mapping ---

    private fun FdaFoodDto.toCachedFood(searchQuery: String? = null): CachedFood? {
        val name = description ?: return null
        if (name.isBlank()) return null

        val nutrientMap = foodNutrients
            ?.filter { it.nutrientNumber != null && it.value != null }
            ?.associate { it.nutrientNumber!! to it.value!! }
            ?: emptyMap()

        val calories = nutrientMap["208"] ?: 0f
        val protein = nutrientMap["203"] ?: 0f
        val carbs = nutrientMap["205"] ?: 0f
        val fat = nutrientMap["204"] ?: 0f

        // FDA search API returns nutrient values already per 100g
        val servingG = servingSize?.takeIf { it > 0f }

        return CachedFood(
            barcode = fdcId.toString(),
            name = formatFdaName(name, brandOwner),
            calories = calories.toInt(),
            proteinG = protein,
            carbsG = carbs,
            fatG = fat,
            servingSizeG = servingG,
            servingSizeLabel = buildFdaServingLabel(),
            imageUrl = null,
            searchQuery = searchQuery,
            lastAccessed = Instant.now()
        )
    }

    private fun FdaFoodDto.buildFdaServingLabel(): String? {
        householdServingFullText?.takeIf { it.isNotBlank() }?.let { return it }
        if (servingSize != null && servingSizeUnit != null) {
            return "${servingSize.toInt()} ${servingSizeUnit.lowercase()}"
        }
        return null
    }

    private fun formatFdaName(description: String, brandOwner: String?): String {
        // FDA descriptions are ALL CAPS — title-case them
        val titleCased = description.lowercase().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
        return if (!brandOwner.isNullOrBlank()) {
            "$titleCased ($brandOwner)"
        } else {
            titleCased
        }
    }

    // --- OpenFoodFacts mapping (used for barcode lookups) ---

    private fun parseServingSizeGrams(servingSize: String?): Float? {
        if (servingSize.isNullOrBlank()) return null
        val gramsMatch = Regex("""(\d+\.?\d*)\s*g\b""", RegexOption.IGNORE_CASE).find(servingSize)
        if (gramsMatch != null) return gramsMatch.groupValues[1].toFloatOrNull()
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
