package com.astute.calories.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.calories.data.local.UserPreferences
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.local.entity.SavedMeal
import com.astute.calories.data.repository.DailyLogRepository
import com.astute.calories.data.repository.SavedMealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val date: LocalDate = LocalDate.now(),
    val calorieGoal: Int = 2000,
    val totalCalories: Int = 0,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val entriesByCategory: Map<MealCategory, List<LogEntry>> = emptyMap(),
    val savedMealsByCategory: Map<MealCategory, List<SavedMeal>> = emptyMap()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val savedMealRepository: SavedMealRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HomeUiState> = combine(
        today,
        dailyLogRepository.getEntriesForDate(LocalDate.now()),
        userPreferences.calorieGoal,
        savedMealRepository.getAll()
    ) { date, entries, goal, savedMeals ->
        val grouped = entries.groupBy { it.mealCategory }
        val savedGrouped = savedMeals.groupBy { it.category }
        HomeUiState(
            date = date,
            calorieGoal = goal,
            totalCalories = entries.sumOf { it.calories },
            totalProtein = entries.sumOf { it.proteinG.toDouble() }.toFloat(),
            totalCarbs = entries.sumOf { it.carbsG.toDouble() }.toFloat(),
            totalFat = entries.sumOf { it.fatG.toDouble() }.toFloat(),
            entriesByCategory = grouped,
            savedMealsByCategory = savedGrouped
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun addQuickEntry(
        foodName: String,
        calories: Int,
        proteinG: Float,
        carbsG: Float,
        fatG: Float,
        mealCategory: MealCategory
    ) {
        viewModelScope.launch {
            dailyLogRepository.addEntry(
                LogEntry(
                    date = LocalDate.now(),
                    mealCategory = mealCategory,
                    foodName = foodName,
                    calories = calories,
                    proteinG = proteinG,
                    carbsG = carbsG,
                    fatG = fatG,
                    servingSize = 0f,
                    quantity = 1f,
                    addedAt = Instant.now()
                )
            )
        }
    }

    fun updateEntry(entry: LogEntry) {
        viewModelScope.launch {
            dailyLogRepository.updateEntry(entry)
        }
    }

    fun removeEntry(entry: LogEntry) {
        viewModelScope.launch {
            dailyLogRepository.removeEntry(entry)
        }
    }

    fun loadSavedMeal(meal: SavedMeal) {
        viewModelScope.launch {
            val items = savedMealRepository.parseItems(meal.itemsJson)
            val entries = savedMealRepository.savedItemsToLogEntries(
                items = items,
                category = meal.category,
                date = LocalDate.now()
            )
            entries.forEach { dailyLogRepository.addEntry(it) }
        }
    }

    fun copyYesterday() {
        viewModelScope.launch {
            val yesterday = LocalDate.now().minusDays(1)
            val entries = dailyLogRepository.getEntriesForDate(yesterday).first()
            entries.forEach { entry ->
                dailyLogRepository.addEntry(
                    entry.copy(
                        id = 0,
                        date = LocalDate.now(),
                        addedAt = Instant.now()
                    )
                )
            }
        }
    }
}
