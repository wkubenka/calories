package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.FoodCacheDao
import com.astute.calories.data.local.entity.CachedFood
import com.astute.calories.data.remote.FdaFoodApi
import com.astute.calories.data.remote.OpenFoodFactsApi
import com.astute.calories.data.remote.dto.FdaFoodDto
import com.astute.calories.data.remote.dto.FdaNutrientDto
import com.astute.calories.data.remote.dto.FdaSearchResponse
import com.astute.calories.data.remote.dto.NutrimentsDto
import com.astute.calories.data.remote.dto.ProductDto
import com.astute.calories.data.remote.dto.ProductLookupResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class FoodRepositoryTest {

    private lateinit var dao: FoodCacheDao
    private lateinit var offApi: OpenFoodFactsApi
    private lateinit var fdaApi: FdaFoodApi
    private lateinit var repository: FoodRepository

    private val fdaApiKey = "test-api-key"

    private val cachedFood = CachedFood(
        barcode = "123456",
        name = "Test Food",
        calories = 200,
        proteinG = 10f,
        carbsG = 20f,
        fatG = 5f,
        servingSizeG = 100f,
        imageUrl = null,
        lastAccessed = Instant.now()
    )

    private fun fdaFood(
        fdcId: Int,
        description: String,
        brandOwner: String? = null,
        servingSize: Float? = null,
        servingSizeUnit: String? = null,
        householdServing: String? = null,
        nutrients: List<FdaNutrientDto> = emptyList()
    ) = FdaFoodDto(
        fdcId = fdcId,
        description = description,
        dataType = "Branded",
        gtinUpc = null,
        brandOwner = brandOwner,
        servingSize = servingSize,
        servingSizeUnit = servingSizeUnit,
        householdServingFullText = householdServing,
        foodNutrients = nutrients
    )

    private fun nutrients(kcal: Float, protein: Float, carbs: Float, fat: Float) = listOf(
        FdaNutrientDto("208", "Energy", "KCAL", kcal),
        FdaNutrientDto("203", "Protein", "G", protein),
        FdaNutrientDto("205", "Carbohydrate, by difference", "G", carbs),
        FdaNutrientDto("204", "Total lipid (fat)", "G", fat)
    )

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
        offApi = mockk()
        fdaApi = mockk()
        repository = FoodRepository(dao, offApi, fdaApi, fdaApiKey)
    }

    // --- Search (FDA) ---

    @Test
    fun `searchFoods returns cached results when available`() = runTest {
        coEvery { dao.searchByName("test") } returns listOf(cachedFood)

        val result = repository.searchFoods("test")

        assertTrue(result is SearchResult.Success)
        val foods = (result as SearchResult.Success).foods
        assertEquals(1, foods.size)
        assertEquals("Test Food", foods[0].name)
        coVerify(exactly = 0) { fdaApi.searchFoods(any(), any()) }
    }

    @Test
    fun `searchFoods falls back to FDA API when cache is empty`() = runTest {
        coEvery { dao.searchByName("banana") } returns emptyList()
        coEvery { fdaApi.searchFoods(fdaApiKey, "banana") } returns FdaSearchResponse(
            foods = listOf(
                fdaFood(
                    fdcId = 789,
                    description = "BANANA",
                    brandOwner = "Chiquita",
                    servingSize = 120f,
                    servingSizeUnit = "g",
                    householdServing = "1 medium (120g)",
                    nutrients = nutrients(89f, 1.1f, 23f, 0.3f)
                )
            ),
            totalHits = 1, currentPage = 1, totalPages = 1
        )

        val result = repository.searchFoods("banana")

        assertTrue(result is SearchResult.Success)
        val foods = (result as SearchResult.Success).foods
        assertEquals(1, foods.size)
        assertEquals("Banana (Chiquita)", foods[0].name)
        // FDA search returns nutrients already per 100g
        assertEquals(89, foods[0].calories)
        assertEquals("789", foods[0].barcode)
        coVerify { dao.upsertAll(any()) }
    }

    @Test
    fun `searchFoods uses per-100g nutrient values directly`() = runTest {
        coEvery { dao.searchByName("cheese") } returns emptyList()
        coEvery { fdaApi.searchFoods(fdaApiKey, "cheese") } returns FdaSearchResponse(
            foods = listOf(
                fdaFood(
                    fdcId = 100,
                    description = "CHEDDAR CHEESE",
                    servingSize = 28f,
                    servingSizeUnit = "g",
                    householdServing = "1 oz",
                    nutrients = nutrients(403f, 25f, 1.3f, 33f)
                )
            ),
            totalHits = 1, currentPage = 1, totalPages = 1
        )

        val result = repository.searchFoods("cheese") as SearchResult.Success
        val food = result.foods[0]
        // FDA search returns nutrients already per 100g — no conversion needed
        assertEquals(403, food.calories)
        assertEquals(25f, food.proteinG)
        assertEquals(28f, food.servingSizeG)
        assertEquals("1 oz", food.servingSizeLabel)
    }

    @Test
    fun `searchFoods handles null serving size gracefully`() = runTest {
        coEvery { dao.searchByName("generic") } returns emptyList()
        coEvery { fdaApi.searchFoods(fdaApiKey, "generic") } returns FdaSearchResponse(
            foods = listOf(
                fdaFood(
                    fdcId = 200,
                    description = "GENERIC FOOD",
                    servingSize = null,
                    nutrients = nutrients(100f, 5f, 10f, 3f)
                )
            ),
            totalHits = 1, currentPage = 1, totalPages = 1
        )

        val result = repository.searchFoods("generic") as SearchResult.Success
        val food = result.foods[0]
        // Nutrients are per 100g regardless of serving size
        assertEquals(100, food.calories)
        assertNull(food.servingSizeG)
    }

    @Test
    fun `searchFoods title-cases FDA description`() = runTest {
        coEvery { dao.searchByName("bread") } returns emptyList()
        coEvery { fdaApi.searchFoods(fdaApiKey, "bread") } returns FdaSearchResponse(
            foods = listOf(
                fdaFood(
                    fdcId = 300,
                    description = "WHOLE WHEAT BREAD",
                    brandOwner = "Wonder",
                    servingSize = 30f,
                    servingSizeUnit = "g",
                    nutrients = nutrients(80f, 3f, 14f, 1f)
                )
            ),
            totalHits = 1, currentPage = 1, totalPages = 1
        )

        val result = repository.searchFoods("bread") as SearchResult.Success
        assertEquals("Whole Wheat Bread (Wonder)", result.foods[0].name)
    }

    @Test
    fun `searchFoods returns error on API failure`() = runTest {
        coEvery { dao.searchByName("fail") } returns emptyList()
        coEvery { fdaApi.searchFoods(fdaApiKey, "fail") } throws RuntimeException("Network error")

        val result = repository.searchFoods("fail")

        assertTrue(result is SearchResult.Error)
        assertTrue((result as SearchResult.Error).message.isNotBlank())
    }

    @Test
    fun `searchFoods builds serving label from size and unit when no household text`() = runTest {
        coEvery { dao.searchByName("yogurt") } returns emptyList()
        coEvery { fdaApi.searchFoods(fdaApiKey, "yogurt") } returns FdaSearchResponse(
            foods = listOf(
                fdaFood(
                    fdcId = 400,
                    description = "GREEK YOGURT",
                    servingSize = 170f,
                    servingSizeUnit = "g",
                    householdServing = null,
                    nutrients = nutrients(100f, 17f, 6f, 0.7f)
                )
            ),
            totalHits = 1, currentPage = 1, totalPages = 1
        )

        val result = repository.searchFoods("yogurt") as SearchResult.Success
        assertEquals("170 g", result.foods[0].servingSizeLabel)
    }

    // --- Barcode lookup (OpenFoodFacts) ---

    @Test
    fun `lookupByBarcode returns cached food when available`() = runTest {
        coEvery { dao.getByBarcode("123456") } returns cachedFood

        val result = repository.lookupByBarcode("123456")

        assertNotNull(result)
        assertEquals("Test Food", result?.name)
        coVerify(exactly = 0) { offApi.getByBarcode(any()) }
    }

    @Test
    fun `lookupByBarcode falls back to OpenFoodFacts when not cached`() = runTest {
        coEvery { dao.getByBarcode("999") } returns null
        coEvery { offApi.getByBarcode("999") } returns ProductLookupResponse(
            status = 1,
            product = ProductDto(
                code = "999",
                productName = "API Product",
                nutriments = NutrimentsDto(
                    energyKcal100g = 150f,
                    proteins100g = 5f,
                    carbohydrates100g = 30f,
                    fat100g = 2f
                ),
                servingSize = null,
                imageUrl = null
            )
        )

        val result = repository.lookupByBarcode("999")

        assertNotNull(result)
        assertEquals("API Product", result?.name)
        coVerify { dao.upsert(any()) }
    }

    @Test
    fun `lookupByBarcode returns null when product not found`() = runTest {
        coEvery { dao.getByBarcode("000") } returns null
        coEvery { offApi.getByBarcode("000") } returns ProductLookupResponse(
            status = 0,
            product = null
        )

        val result = repository.lookupByBarcode("000")

        assertNull(result)
    }
}
