package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.FoodCacheDao
import com.astute.calories.data.local.entity.CachedFood
import com.astute.calories.data.remote.OpenFoodFactsApi
import com.astute.calories.data.remote.dto.NutrimentsDto
import com.astute.calories.data.remote.dto.ProductDto
import com.astute.calories.data.remote.dto.ProductLookupResponse
import com.astute.calories.data.remote.dto.SearchResponse
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
    private lateinit var api: OpenFoodFactsApi
    private lateinit var repository: FoodRepository

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

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
        api = mockk()
        repository = FoodRepository(dao, api)
    }

    @Test
    fun `searchFoods returns cached results when available`() = runTest {
        coEvery { dao.searchByName("test") } returns listOf(cachedFood)

        val result = repository.searchFoods("test")

        assertTrue(result is SearchResult.Success)
        val foods = (result as SearchResult.Success).foods
        assertEquals(1, foods.size)
        assertEquals("Test Food", foods[0].name)
        coVerify(exactly = 0) { api.searchByName(any()) }
    }

    @Test
    fun `searchFoods falls back to API when cache is empty`() = runTest {
        coEvery { dao.searchByName("banana") } returns emptyList()
        coEvery { api.searchByName("banana") } returns SearchResponse(
            products = listOf(
                ProductDto(
                    code = "789",
                    productName = "Banana",
                    nutriments = NutrimentsDto(
                        energyKcal100g = 89f,
                        proteins100g = 1.1f,
                        carbohydrates100g = 23f,
                        fat100g = 0.3f
                    ),
                    servingSize = "120g",
                    imageUrl = null
                )
            )
        )

        val result = repository.searchFoods("banana")

        assertTrue(result is SearchResult.Success)
        val foods = (result as SearchResult.Success).foods
        assertEquals(1, foods.size)
        assertEquals("Banana", foods[0].name)
        assertEquals(89, foods[0].calories)
        coVerify { dao.upsertAll(any()) }
    }

    @Test
    fun `searchFoods returns error on API failure`() = runTest {
        coEvery { dao.searchByName("fail") } returns emptyList()
        coEvery { api.searchByName("fail") } throws RuntimeException("Network error")

        val result = repository.searchFoods("fail")

        assertTrue(result is SearchResult.Error)
        val error = (result as SearchResult.Error)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `lookupByBarcode returns cached food when available`() = runTest {
        coEvery { dao.getByBarcode("123456") } returns cachedFood

        val result = repository.lookupByBarcode("123456")

        assertNotNull(result)
        assertEquals("Test Food", result?.name)
        coVerify(exactly = 0) { api.getByBarcode(any()) }
    }

    @Test
    fun `lookupByBarcode falls back to API when not cached`() = runTest {
        coEvery { dao.getByBarcode("999") } returns null
        coEvery { api.getByBarcode("999") } returns ProductLookupResponse(
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
    fun `searchFoods parses simple gram serving size`() = runTest {
        coEvery { dao.searchByName("apple") } returns emptyList()
        coEvery { api.searchByName("apple") } returns SearchResponse(
            products = listOf(
                ProductDto("1", "Apple", NutrimentsDto(52f, 0.3f, 14f, 0.2f), "150g", null)
            )
        )

        val result = repository.searchFoods("apple") as SearchResult.Success
        assertEquals(150f, result.foods[0].servingSizeG)
    }

    @Test
    fun `searchFoods parses grams from compound serving size`() = runTest {
        coEvery { dao.searchByName("milk") } returns emptyList()
        coEvery { api.searchByName("milk") } returns SearchResponse(
            products = listOf(
                ProductDto("2", "Milk", NutrimentsDto(42f, 3.4f, 5f, 1f), "250 ml (253 g)", null)
            )
        )

        val result = repository.searchFoods("milk") as SearchResult.Success
        assertEquals(253f, result.foods[0].servingSizeG)
    }

    @Test
    fun `searchFoods parses grams from parenthetical serving size`() = runTest {
        coEvery { dao.searchByName("bread") } returns emptyList()
        coEvery { api.searchByName("bread") } returns SearchResponse(
            products = listOf(
                ProductDto("3", "Bread", NutrimentsDto(265f, 9f, 49f, 3.2f), "2 slices (56g)", null)
            )
        )

        val result = repository.searchFoods("bread") as SearchResult.Success
        assertEquals(56f, result.foods[0].servingSizeG)
    }

    @Test
    fun `searchFoods falls back to first number when no grams unit`() = runTest {
        coEvery { dao.searchByName("cup") } returns emptyList()
        coEvery { api.searchByName("cup") } returns SearchResponse(
            products = listOf(
                ProductDto("4", "Yogurt", NutrimentsDto(59f, 10f, 3.6f, 0.4f), "1 cup", null)
            )
        )

        val result = repository.searchFoods("cup") as SearchResult.Success
        assertEquals(1f, result.foods[0].servingSizeG)
    }

    @Test
    fun `searchFoods returns null servingSizeG for null serving size`() = runTest {
        coEvery { dao.searchByName("mystery") } returns emptyList()
        coEvery { api.searchByName("mystery") } returns SearchResponse(
            products = listOf(
                ProductDto("5", "Mystery Food", NutrimentsDto(100f, 5f, 10f, 3f), null, null)
            )
        )

        val result = repository.searchFoods("mystery") as SearchResult.Success
        assertNull(result.foods[0].servingSizeG)
    }

    @Test
    fun `searchFoods stores servingSizeLabel from API`() = runTest {
        coEvery { dao.searchByName("juice") } returns emptyList()
        coEvery { api.searchByName("juice") } returns SearchResponse(
            products = listOf(
                ProductDto("6", "Orange Juice", NutrimentsDto(45f, 0.7f, 10f, 0.2f), "250 ml (250g)", null)
            )
        )

        val result = repository.searchFoods("juice") as SearchResult.Success
        assertEquals("250 ml (250g)", result.foods[0].servingSizeLabel)
    }

    @Test
    fun `lookupByBarcode returns null when product not found`() = runTest {
        coEvery { dao.getByBarcode("000") } returns null
        coEvery { api.getByBarcode("000") } returns ProductLookupResponse(
            status = 0,
            product = null
        )

        val result = repository.lookupByBarcode("000")

        assertNull(result)
    }
}
