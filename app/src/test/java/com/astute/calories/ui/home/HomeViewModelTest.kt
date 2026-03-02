package com.astute.calories.ui.home

import app.cash.turbine.test
import com.astute.calories.data.local.UserPreferences
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.local.entity.SavedMeal
import com.astute.calories.data.repository.DailyLogRepository
import com.astute.calories.data.repository.SavedMealRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var dailyLogRepository: DailyLogRepository
    private lateinit var savedMealRepository: SavedMealRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val sampleEntry = LogEntry(
        id = 1,
        date = LocalDate.now(),
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
        Dispatchers.setMain(testDispatcher)
        dailyLogRepository = mockk(relaxed = true)
        savedMealRepository = mockk(relaxed = true)
        userPreferences = mockk()

        every { dailyLogRepository.getEntriesForDate(any()) } returns flowOf(listOf(sampleEntry))
        every { userPreferences.calorieGoal } returns flowOf(2000)
        every { savedMealRepository.getAll() } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(dailyLogRepository, savedMealRepository, userPreferences)

    @Test
    fun `uiState computes totals from entries`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            // Skip initial
            val initial = awaitItem()
            val state = awaitItem()
            assertEquals(300, state.totalCalories)
            assertEquals(10f, state.totalProtein)
            assertEquals(50f, state.totalCarbs)
            assertEquals(5f, state.totalFat)
            assertEquals(2000, state.calorieGoal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState groups entries by meal category`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // skip initial
            val state = awaitItem()
            assertEquals(1, state.entriesByCategory[MealCategory.BREAKFAST]?.size)
            assertEquals(null, state.entriesByCategory[MealCategory.LUNCH])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addQuickEntry calls repository`() = runTest {
        viewModel = createViewModel()
        coEvery { dailyLogRepository.addEntry(any()) } returns 1L

        viewModel.addQuickEntry("Apple", 95, 0.5f, 25f, 0.3f, MealCategory.SNACKS)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            dailyLogRepository.addEntry(match {
                it.foodName == "Apple" && it.calories == 95 && it.mealCategory == MealCategory.SNACKS
            })
        }
    }

    @Test
    fun `removeEntry calls repository`() = runTest {
        viewModel = createViewModel()

        viewModel.removeEntry(sampleEntry)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { dailyLogRepository.removeEntry(sampleEntry) }
    }

    @Test
    fun `updateEntry calls repository`() = runTest {
        viewModel = createViewModel()
        val updated = sampleEntry.copy(calories = 400)

        viewModel.updateEntry(updated)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { dailyLogRepository.updateEntry(updated) }
    }
}
