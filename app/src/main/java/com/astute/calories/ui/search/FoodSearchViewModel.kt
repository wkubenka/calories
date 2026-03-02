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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
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

    val uiState: StateFlow<FoodSearchUiState> = _query
        .debounce(350)
        .distinctUntilChanged()
        .mapLatest { query ->
            if (query.length < 2) {
                FoodSearchUiState(query = query)
            } else {
                when (val result = foodRepository.searchFoods(query)) {
                    is SearchResult.Success -> FoodSearchUiState(
                        query = query,
                        results = result.foods
                    )
                    is SearchResult.Error -> FoodSearchUiState(
                        query = query,
                        errorMessage = result.message
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FoodSearchUiState()
        )

    fun onQueryChanged(query: String) {
        _query.value = query
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
