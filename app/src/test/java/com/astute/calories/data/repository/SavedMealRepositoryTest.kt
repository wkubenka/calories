package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.SavedMealDao
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.local.entity.SavedMeal
import com.astute.calories.data.local.entity.SavedMealItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class SavedMealRepositoryTest {

    private lateinit var dao: SavedMealDao
    private lateinit var moshi: Moshi
    private lateinit var repository: SavedMealRepository

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        repository = SavedMealRepository(dao, moshi)
    }

    @Test
    fun `parseItems and toJson round-trip correctly`() {
        val items = listOf(
            SavedMealItem("Eggs", 150, 12f, 1f, 10f, 100f, 2f, null),
            SavedMealItem("Toast", 80, 3f, 15f, 1f, 30f, 1f, null)
        )

        val json = repository.toJson(items)
        val parsed = repository.parseItems(json)

        assertEquals(2, parsed.size)
        assertEquals("Eggs", parsed[0].foodName)
        assertEquals(150, parsed[0].calories)
        assertEquals("Toast", parsed[1].foodName)
    }

    @Test
    fun `logEntriesToSavedItems converts entries correctly`() {
        val entries = listOf(
            LogEntry(
                id = 1,
                date = LocalDate.now(),
                mealCategory = MealCategory.BREAKFAST,
                foodName = "Pancakes",
                calories = 350,
                proteinG = 8f,
                carbsG = 45f,
                fatG = 12f,
                servingSize = 200f,
                quantity = 1f,
                barcode = "123",
                addedAt = Instant.now()
            )
        )

        val items = repository.logEntriesToSavedItems(entries)

        assertEquals(1, items.size)
        assertEquals("Pancakes", items[0].foodName)
        assertEquals(350, items[0].calories)
        assertEquals("123", items[0].barcode)
    }

    @Test
    fun `savedItemsToLogEntries converts items correctly`() {
        val items = listOf(
            SavedMealItem("Salad", 120, 5f, 10f, 6f, 150f, 1f, null)
        )
        val date = LocalDate.of(2025, 3, 1)

        val entries = repository.savedItemsToLogEntries(items, MealCategory.LUNCH, date)

        assertEquals(1, entries.size)
        assertEquals("Salad", entries[0].foodName)
        assertEquals(MealCategory.LUNCH, entries[0].mealCategory)
        assertEquals(date, entries[0].date)
        assertEquals(0L, entries[0].id) // new entry, no ID
    }

    @Test
    fun `save delegates to dao insert`() = runTest {
        val meal = SavedMeal(name = "My Meal", category = MealCategory.DINNER, itemsJson = "[]")
        coEvery { dao.insert(meal) } returns 1L

        val id = repository.save(meal)

        assertEquals(1L, id)
        coVerify { dao.insert(meal) }
    }

    @Test
    fun `delete delegates to dao delete`() = runTest {
        val meal = SavedMeal(id = 5, name = "Old Meal", category = MealCategory.SNACKS, itemsJson = "[]")

        repository.delete(meal)

        coVerify { dao.delete(meal) }
    }
}
