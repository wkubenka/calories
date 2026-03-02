package com.astute.calories.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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

    suspend fun setCalorieGoal(goal: Int) {
        dataStore.edit { prefs ->
            prefs[CALORIE_GOAL_KEY] = goal
        }
    }

    companion object {
        val CALORIE_GOAL_KEY = intPreferencesKey("calorie_goal")
        const val DEFAULT_CALORIE_GOAL = 2000
    }
}
