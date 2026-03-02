package com.astute.calories.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.astute.calories.data.local.entity.MealCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntrySheet(
    onDismiss: () -> Unit,
    onConfirm: (
        foodName: String,
        calories: Int,
        proteinG: Float,
        carbsG: Float,
        fatG: Float,
        category: MealCategory
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var foodName by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(MealCategory.SNACKS) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Quick Add",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it },
                label = { Text("Food name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it.filter { c -> c.isDigit() } },
                label = { Text("Calories") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Protein (g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Carbs (g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Fat (g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Meal",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MealCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val cal = calories.toIntOrNull() ?: 0
                    if (foodName.isNotBlank() && cal > 0) {
                        onConfirm(
                            foodName.trim(),
                            cal,
                            protein.toFloatOrNull() ?: 0f,
                            carbs.toFloatOrNull() ?: 0f,
                            fat.toFloatOrNull() ?: 0f,
                            selectedCategory
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = foodName.isNotBlank() && (calories.toIntOrNull() ?: 0) > 0
            ) {
                Text("Add Entry")
            }
        }
    }
}
