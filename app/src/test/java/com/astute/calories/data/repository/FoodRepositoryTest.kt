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

        val results = repository.searchFoods("test")

        assertEquals(1, results.size)
        assertEquals("Test Food", results[0].name)
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

        val results = repository.searchFoods("banana")

        assertEquals(1, results.size)
        assertEquals("Banana", results[0].name)
        assertEquals(89, results[0].calories)
        coVerify { dao.upsertAll(any()) }
    }

    @Test
    fun `searchFoods returns empty on API failure`() = runTest {
        coEvery { dao.searchByName("fail") } returns emptyList()
        coEvery { api.searchByName("fail") } throws RuntimeException("Network error")

        val results = repository.searchFoods("fail")

        assertEquals(0, results.size)
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
