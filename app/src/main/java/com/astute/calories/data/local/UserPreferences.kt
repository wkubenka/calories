package com.astute.calories.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val calorieGoal: Flow<Int> = dataStore.data.map { prefs ->
        prefs[CALORIE_GOAL_KEY] ?: DEFAULT_CALORIE_GOAL
    }

    val resetHour: Flow<Int> = dataStore.data.map { prefs ->
        prefs[RESET_HOUR_KEY] ?: DEFAULT_RESET_HOUR
    }

    val reminderHour: Flow<Int> = dataStore.data.map { prefs ->
        prefs[REMINDER_HOUR_KEY] ?: DEFAULT_REMINDER_HOUR
    }

    val reminderEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[REMINDER_ENABLED_KEY] ?: false
    }

    suspend fun setCalorieGoal(goal: Int) {
        dataStore.edit { it[CALORIE_GOAL_KEY] = goal }
    }

    suspend fun setResetHour(hour: Int) {
        dataStore.edit { it[RESET_HOUR_KEY] = hour }
    }

    suspend fun setReminderHour(hour: Int) {
        dataStore.edit { it[REMINDER_HOUR_KEY] = hour }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDER_ENABLED_KEY] = enabled }
    }

    companion object {
        val CALORIE_GOAL_KEY = intPreferencesKey("calorie_goal")
        val RESET_HOUR_KEY = intPreferencesKey("reset_hour")
        val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
        val REMINDER_ENABLED_KEY = booleanPreferencesKey("reminder_enabled")
        const val DEFAULT_CALORIE_GOAL = 2000
        const val DEFAULT_RESET_HOUR = 0
        const val DEFAULT_REMINDER_HOUR = 20
    }
}
