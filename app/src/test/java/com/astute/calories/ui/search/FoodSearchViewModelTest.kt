package com.astute.calories.ui.search

import app.cash.turbine.test
import com.astute.calories.data.local.entity.CachedFood
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.repository.DailyLogRepository
import com.astute.calories.data.repository.FoodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FoodSearchViewModelTest {

    private lateinit var foodRepository: FoodRepository
    private lateinit var dailyLogRepository: DailyLogRepository
    private lateinit var viewModel: FoodSearchViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val sampleFood = CachedFood(
        barcode = "123",
        name = "Banana",
        calories = 89,
        proteinG = 1.1f,
        carbsG = 23f,
        fatG = 0.3f,
        servingSizeG = 120f,
        imageUrl = null,
        lastAccessed = Instant.now()
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        foodRepository = mockk(relaxed = true)
        dailyLogRepository = mockk(relaxed = true)
        viewModel = FoodSearchViewModel(foodRepository, dailyLogRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.query)
            assertTrue(state.results.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addFoodToLog creates entry with scaled calories`() = runTest {
        coEvery { dailyLogRepository.addEntry(any()) } returns 1L

        // 120g serving, qty 1, food has 89 kcal/100g => 89 * 1.2 = 106
        viewModel.addFoodToLog(sampleFood, 120f, 1f, MealCategory.SNACKS)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            dailyLogRepository.addEntry(match {
                it.foodName == "Banana" &&
                it.calories == 106 &&
                it.mealCategory == MealCategory.SNACKS
            })
        }
    }

    @Test
    fun `addFoodToLog scales macros by serving and quantity`() = runTest {
        coEvery { dailyLogRepository.addEntry(any()) } returns 1L

        // 200g serving, qty 2 => factor = 4.0
        viewModel.addFoodToLog(sampleFood, 200f, 2f, MealCategory.LUNCH)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            dailyLogRepository.addEntry(match {
                it.calories == (89 * 4) && // 356
                it.proteinG == 1.1f * 4f &&
                it.carbsG == 23f * 4f
            })
        }
    }
}
