package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.DailyLogDao
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class DailyLogRepositoryTest {

    private lateinit var dao: DailyLogDao
    private lateinit var repository: DailyLogRepository

    private val today = LocalDate.of(2025, 1, 15)

    private val sampleEntry = LogEntry(
        id = 1,
        date = today,
        mealCategory = MealCategory.BREAKFAST,
        foodName = "Oatmeal",
        calories = 300,
        proteinG = 10f,
        carbsG = 50f,
        fatG = 5f,
        servingSize = 100f,
        quantity = 1f,
        addedAt = Instant.now()
    )

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
        repository = DailyLogRepository(dao)
    }

    @Test
    fun `getEntriesForDate returns entries from dao`() = runTest {
        val entries = listOf(sampleEntry)
        every { dao.getEntriesForDate(today) } returns flowOf(entries)

        val result = repository.getEntriesForDate(today).first()

        assertEquals(1, result.size)
        assertEquals("Oatmeal", result[0].foodName)
    }

    @Test
    fun `getTotalCaloriesForDate returns sum from dao`() = runTest {
        every { dao.getTotalCaloriesForDate(today) } returns flowOf(750)

        val result = repository.getTotalCaloriesForDate(today).first()

        assertEquals(750, result)
    }

    @Test
    fun `addEntry calls dao insert`() = runTest {
        coEvery { dao.insert(any()) } returns 1L

        val id = repository.addEntry(sampleEntry)

        assertEquals(1L, id)
        coVerify { dao.insert(sampleEntry) }
    }

    @Test
    fun `updateEntry calls dao update`() = runTest {
        repository.updateEntry(sampleEntry)

        coVerify { dao.update(sampleEntry) }
    }

    @Test
    fun `removeEntry calls dao delete`() = runTest {
        repository.removeEntry(sampleEntry)

        coVerify { dao.delete(sampleEntry) }
    }

    @Test
    fun `clearDate calls dao deleteAllForDate`() = runTest {
        repository.clearDate(today)

        coVerify { dao.deleteAllForDate(today) }
    }

    @Test
    fun `deleteEntriesBefore calls dao deleteEntriesBefore`() = runTest {
        val cutoff = LocalDate.of(2025, 1, 14)

        repository.deleteEntriesBefore(cutoff)

        coVerify { dao.deleteEntriesBefore(cutoff) }
    }
}
