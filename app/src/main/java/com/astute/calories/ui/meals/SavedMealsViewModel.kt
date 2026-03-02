package com.astute.calories.ui.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.local.entity.SavedMeal
import com.astute.calories.data.local.entity.SavedMealItem
import com.astute.calories.data.repository.SavedMealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedMealWithItems(
    val meal: SavedMeal,
    val items: List<SavedMealItem>
)

data class SavedMealsUiState(
    val mealsByCategory: Map<MealCategory, List<SavedMealWithItems>> = emptyMap()
)

@HiltViewModel
class SavedMealsViewModel @Inject constructor(
    private val savedMealRepository: SavedMealRepository
) : ViewModel() {

    val uiState: StateFlow<SavedMealsUiState> = savedMealRepository.getAll()
        .map { meals ->
            val grouped = meals
                .map { meal ->
                    SavedMealWithItems(
                        meal = meal,
                        items = savedMealRepository.parseItems(meal.itemsJson)
                    )
                }
                .groupBy { it.meal.category }
            SavedMealsUiState(mealsByCategory = grouped)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SavedMealsUiState()
        )

    fun deleteMeal(meal: SavedMeal) {
        viewModelScope.launch {
            savedMealRepository.delete(meal)
        }
    }

    fun renameMeal(meal: SavedMeal, newName: String) {
        viewModelScope.launch {
            savedMealRepository.update(meal.copy(name = newName))
        }
    }

    fun removeItemFromMeal(meal: SavedMeal, itemIndex: Int) {
        viewModelScope.launch {
            val items = savedMealRepository.parseItems(meal.itemsJson).toMutableList()
            if (itemIndex in items.indices) {
                items.removeAt(itemIndex)
                savedMealRepository.update(
                    meal.copy(itemsJson = savedMealRepository.toJson(items))
                )
            }
        }
    }
}
