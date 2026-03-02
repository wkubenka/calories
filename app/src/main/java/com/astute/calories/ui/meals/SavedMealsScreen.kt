package com.astute.calories.ui.meals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.local.entity.SavedMeal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SavedMealsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavedMealsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingMeal by rememberSaveable { mutableStateOf<SavedMeal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Meals") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.mealsByCategory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No saved meals yet.\nSave a meal from the home screen.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                MealCategory.entries.forEach { category ->
                    val meals = uiState.mealsByCategory[category]
                    if (!meals.isNullOrEmpty()) {
                        item {
                            Text(
                                text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(meals, key = { it.meal.id }) { mealWithItems ->
                            var expanded by rememberSaveable { mutableStateOf(false) }

                            Card(
                                modifier = Modifier.animateItem().fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = { expanded = !expanded },
                                                onLongClick = { editingMeal = mealWithItems.meal }
                                            )
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mealWithItems.meal.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            val totalCals = mealWithItems.items.sumOf { it.calories }
                                            Text(
                                                text = "${mealWithItems.items.size} items  •  $totalCals kcal",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row {
                                            IconButton(onClick = { viewModel.deleteMeal(mealWithItems.meal) }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            Icon(
                                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                                                else Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (expanded) "Collapse" else "Expand"
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = expandVertically(),
                                        exit = shrinkVertically()
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                            mealWithItems.items.forEachIndexed { index, item ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = item.foodName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = "${item.calories} kcal",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Rename dialog on long-press
    editingMeal?.let { meal ->
        var newName by rememberSaveable { mutableStateOf(meal.name) }
        AlertDialog(
            onDismissRequest = { editingMeal = null },
            title = { Text("Rename Meal") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Meal name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.renameMeal(meal, newName.trim())
                    }
                    editingMeal = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMeal = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
