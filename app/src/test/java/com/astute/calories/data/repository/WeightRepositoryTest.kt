package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.WeightDao
import com.astute.calories.data.local.entity.WeightEntry
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

class WeightRepositoryTest {

    private lateinit var dao: WeightDao
    private lateinit var repository: WeightRepository

    private val today = LocalDate.of(2025, 1, 15)
    private val sample = WeightEntry(
        date = today,
        weightLbs = 178.4f,
        recordedAt = Instant.parse("2025-01-15T08:00:00Z")
    )

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
        repository = WeightRepository(dao)
    }

    @Test
    fun `getForDate returns entry from dao`() = runTest {
        every { dao.getForDate(today) } returns flowOf(sample)

        val result = repository.getForDate(today).first()

        assertEquals(178.4f, result?.weightLbs)
    }

    @Test
    fun `getRecent returns entries from dao`() = runTest {
        every { dao.getRecent() } returns flowOf(listOf(sample))

        val result = repository.getRecent().first()

        assertEquals(1, result.size)
    }

    @Test
    fun `upsert calls dao upsert`() = runTest {
        repository.upsert(sample)

        coVerify { dao.upsert(sample) }
    }

    @Test
    fun `delete calls dao delete`() = runTest {
        repository.delete(today)

        coVerify { dao.delete(today) }
    }
}
