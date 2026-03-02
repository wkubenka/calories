package com.astute.calories.ui.search.components

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
import com.astute.calories.data.local.entity.CachedFood
import com.astute.calories.data.local.entity.MealCategory

private enum class ServingMode { PER_SERVING, PER_100G }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodSheet(
    food: CachedFood,
    onDismiss: () -> Unit,
    onConfirm: (servingSize: Float, quantity: Float, category: MealCategory) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasServing = food.servingSizeG != null && food.servingSizeG > 0f
    var servingMode by rememberSaveable {
        mutableStateOf(if (hasServing) ServingMode.PER_SERVING else ServingMode.PER_100G)
    }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var customGrams by rememberSaveable { mutableStateOf("100") }
    var selectedCategory by rememberSaveable { mutableStateOf(MealCategory.SNACKS) }

    val qty = quantity.toFloatOrNull() ?: 1f
    val effectiveGrams = when (servingMode) {
        ServingMode.PER_SERVING -> (food.servingSizeG ?: 100f) * qty
        ServingMode.PER_100G -> (customGrams.toFloatOrNull() ?: 100f) * qty
    }
    val estimatedCals = ((food.calories * effectiveGrams) / 100f).toInt()

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
                text = food.name,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${food.calories} kcal / 100g",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Serving mode selector
            if (hasServing) {
                Text(text = "Portion", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val servingLabel = food.servingSizeLabel ?: "${food.servingSizeG?.toInt()}g"
                    FilterChip(
                        selected = servingMode == ServingMode.PER_SERVING,
                        onClick = { servingMode = ServingMode.PER_SERVING },
                        label = { Text(servingLabel) }
                    )
                    FilterChip(
                        selected = servingMode == ServingMode.PER_100G,
                        onClick = { servingMode = ServingMode.PER_100G },
                        label = { Text("Custom (g)") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (servingMode == ServingMode.PER_100G) {
                    OutlinedTextField(
                        value = customGrams,
                        onValueChange = { customGrams = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Grams") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } },
                    label = {
                        Text(
                            if (servingMode == ServingMode.PER_SERVING) "Servings"
                            else "Qty"
                        )
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Estimated: $estimatedCals kcal",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Meal", style = MaterialTheme.typography.labelLarge)

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

            val servingSizeForLog = when (servingMode) {
                ServingMode.PER_SERVING -> food.servingSizeG ?: 100f
                ServingMode.PER_100G -> customGrams.toFloatOrNull() ?: 100f
            }

            Button(
                onClick = {
                    onConfirm(servingSizeForLog, qty, selectedCategory)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to Log")
            }
        }
    }
}
