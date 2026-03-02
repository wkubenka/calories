package com.astute.calories.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.ui.entry.ManualEntrySheet
import com.astute.calories.ui.entry.ServingSizeSheet
import com.astute.calories.ui.home.components.CalorieProgressRing
import com.astute.calories.ui.home.components.MacroSummary
import com.astute.calories.ui.home.components.MealCategoryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToSavedMeals: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showManualEntry by rememberSaveable { mutableStateOf(false) }
    var editingEntry by rememberSaveable { mutableStateOf<LogEntry?>(null) }
    var savingCategory by rememberSaveable { mutableStateOf<MealCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calories") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(onClick = { showManualEntry = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Quick add")
                }
                FloatingActionButton(onClick = onNavigateToSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Search foods")
                }
                FloatingActionButton(onClick = onNavigateToScanner) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CalorieProgressRing(
                    consumed = uiState.totalCalories,
                    goal = uiState.calorieGoal
                )
            }

            item {
                MacroSummary(
                    proteinG = uiState.totalProtein,
                    carbsG = uiState.totalCarbs,
                    fatG = uiState.totalFat
                )
            }

            item {
                TextButton(onClick = { viewModel.copyYesterday() }) {
                    Text("Copy yesterday's meals")
                }
            }

            items(MealCategory.entries, key = { it.name }) { category ->
                MealCategoryCard(
                    modifier = Modifier.animateItem(),
                    category = category,
                    entries = uiState.entriesByCategory[category] ?: emptyList(),
                    savedMeals = uiState.savedMealsByCategory[category] ?: emptyList(),
                    onRemoveEntry = { viewModel.removeEntry(it) },
                    onEditEntry = { editingEntry = it },
                    onSaveMeal = { savingCategory = it },
                    onLoadSavedMeal = { viewModel.loadSavedMeal(it) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
        }
    }

    if (showManualEntry) {
        ManualEntrySheet(
            onDismiss = { showManualEntry = false },
            onConfirm = { name, cals, protein, carbs, fat, category ->
                viewModel.addQuickEntry(name, cals, protein, carbs, fat, category)
                showManualEntry = false
            }
        )
    }

    editingEntry?.let { entry ->
        ServingSizeSheet(
            entry = entry,
            onDismiss = { editingEntry = null },
            onConfirm = { updated ->
                viewModel.updateEntry(updated)
                editingEntry = null
            }
        )
    }

    // Save meal dialog
    savingCategory?.let { category ->
        var mealName by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { savingCategory = null },
            title = { Text("Save Meal") },
            text = {
                OutlinedTextField(
                    value = mealName,
                    onValueChange = { mealName = it },
                    label = { Text("Meal name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (mealName.isNotBlank()) {
                            viewModel.saveCurrentMeal(category, mealName.trim())
                            savingCategory = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { savingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
