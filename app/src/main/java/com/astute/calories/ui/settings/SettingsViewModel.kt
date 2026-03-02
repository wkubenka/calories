package com.astute.calories.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.calories.data.local.UserPreferences
import com.astute.calories.worker.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val calorieGoal: Int = 2000,
    val resetHour: Int = 0,
    val reminderHour: Int = 20,
    val reminderEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferences.calorieGoal,
        userPreferences.resetHour,
        userPreferences.reminderHour,
        userPreferences.reminderEnabled
    ) { goal, resetHour, reminderHour, reminderEnabled ->
        SettingsUiState(
            calorieGoal = goal,
            resetHour = resetHour,
            reminderHour = reminderHour,
            reminderEnabled = reminderEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setCalorieGoal(goal: Int) {
        viewModelScope.launch { userPreferences.setCalorieGoal(goal) }
    }

    fun setResetHour(hour: Int) {
        viewModelScope.launch {
            userPreferences.setResetHour(hour)
            WorkScheduler.scheduleDailyReset(context, hour)
        }
    }

    fun setReminderHour(hour: Int) {
        viewModelScope.launch {
            userPreferences.setReminderHour(hour)
            if (uiState.value.reminderEnabled) {
                WorkScheduler.scheduleReminder(context, hour)
            }
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setReminderEnabled(enabled)
            if (enabled) {
                WorkScheduler.scheduleReminder(context, uiState.value.reminderHour)
            } else {
                WorkScheduler.cancelReminder(context)
            }
        }
    }
}
