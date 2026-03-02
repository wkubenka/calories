package com.astute.calories.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.calories.data.local.entity.CachedFood
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.repository.DailyLogRepository
import com.astute.calories.data.repository.FoodRepository
import com.astute.calories.data.repository.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class FoodSearchUiState(
    val query: String = "",
    val results: List<CachedFood> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val dailyLogRepository: DailyLogRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    private val _uiState = MutableStateFlow(FoodSearchUiState())
    val uiState: StateFlow<FoodSearchUiState> = _uiState

    init {
        viewModelScope.launch {
            _query
                .debounce(350)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length < 2) {
                        _uiState.value = _uiState.value.copy(
                            results = emptyList(),
                            isLoading = false,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                        val newState = when (val result = foodRepository.searchFoods(query)) {
                            is SearchResult.Success -> _uiState.value.copy(
                                results = result.foods,
                                isLoading = false
                            )
                            is SearchResult.Error -> _uiState.value.copy(
                                errorMessage = result.message,
                                isLoading = false
                            )
                        }
                        _uiState.value = newState
                    }
                }
        }
    }

    fun onQueryChanged(query: String) {
        _query.value = query
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun addFoodToLog(
        food: CachedFood,
        servingSize: Float,
        quantity: Float,
        category: MealCategory
    ) {
        viewModelScope.launch {
            val scaleFactor = if (food.calories > 0) (servingSize * quantity) / 100f else 1f
            dailyLogRepository.addEntry(
                LogEntry(
                    date = LocalDate.now(),
                    mealCategory = category,
                    foodName = food.name,
                    calories = (food.calories * scaleFactor).toInt(),
                    proteinG = food.proteinG * scaleFactor,
                    carbsG = food.carbsG * scaleFactor,
                    fatG = food.fatG * scaleFactor,
                    servingSize = servingSize,
                    quantity = quantity,
                    barcode = food.barcode,
                    addedAt = Instant.now()
                )
            )
        }
    }
}
