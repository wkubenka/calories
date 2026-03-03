package com.astute.calories.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.calories.data.local.entity.CachedFood
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.repository.DailyLogRepository
import com.astute.calories.data.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

sealed class ScanResult {
    data object Idle : ScanResult()
    data object Loading : ScanResult()
    data class Found(val food: CachedFood) : ScanResult()
    data class NotFound(val barcode: String) : ScanResult()
}

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val dailyLogRepository: DailyLogRepository
) : ViewModel() {

    private val _scanResult = MutableStateFlow<ScanResult>(ScanResult.Idle)
    val scanResult: StateFlow<ScanResult> = _scanResult.asStateFlow()

    private var lastScannedBarcode: String? = null

    fun onBarcodeDetected(barcode: String) {
        val current = _scanResult.value
        if (current is ScanResult.Loading || current is ScanResult.Found) return
        lastScannedBarcode = barcode

        viewModelScope.launch {
            _scanResult.value = ScanResult.Loading
            val food = foodRepository.lookupByBarcode(barcode)
            _scanResult.value = if (food != null) {
                ScanResult.Found(food)
            } else {
                ScanResult.NotFound(barcode)
            }
        }
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

    fun resetScan() {
        lastScannedBarcode = null
        _scanResult.value = ScanResult.Idle
    }
}
