package com.astute.calories.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.local.entity.SavedMeal

@Composable
fun MealCategoryCard(
    category: MealCategory,
    entries: List<LogEntry>,
    savedMeals: List<SavedMeal>,
    onRemoveEntry: (LogEntry) -> Unit,
    onEditEntry: (LogEntry) -> Unit,
    onSaveMeal: (MealCategory) -> Unit,
    onLoadSavedMeal: (SavedMeal) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var showLoadMenu by rememberSaveable { mutableStateOf(false) }
    val totalCals = entries.sumOf { it.calories }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$totalCals kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { onSaveMeal(category) }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Save meal",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            // Items
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    entries.forEach { entry ->
                        SwipeableLogItem(
                            entry = entry,
                            onRemove = { onRemoveEntry(entry) },
                            onTap = { onEditEntry(entry) }
                        )
                    }

                    // Load saved meal button
                    if (savedMeals.isNotEmpty()) {
                        Row(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
                            TextButton(onClick = { showLoadMenu = true }) {
                                Text("Load saved meal")
                            }
                            DropdownMenu(
                                expanded = showLoadMenu,
                                onDismissRequest = { showLoadMenu = false }
                            ) {
                                savedMeals.forEach { meal ->
                                    DropdownMenuItem(
                                        text = { Text(meal.name) },
                                        onClick = {
                                            onLoadSavedMeal(meal)
                                            showLoadMenu = false
                                        }
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

private fun MealCategory.displayName(): String = when (this) {
    MealCategory.BREAKFAST -> "Breakfast"
    MealCategory.LUNCH -> "Lunch"
    MealCategory.DINNER -> "Dinner"
    MealCategory.SNACKS -> "Snacks"
}
