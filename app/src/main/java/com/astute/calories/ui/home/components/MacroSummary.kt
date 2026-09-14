package com.astute.calories.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MacroSummary(
    proteinG: Float,
    carbsG: Float,
    fatG: Float,
    proteinGoalG: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroItem(label = "Fat", grams = fatG)
        MacroItem(label = "Carbs", grams = carbsG)
        MacroItem(label = "Protein", grams = proteinG, goalGrams = proteinGoalG)
    }
}

@Composable
private fun MacroItem(label: String, grams: Float, goalGrams: Int? = null) {
    val valueText = if (goalGrams != null) "${grams.toInt()}g / ${goalGrams}g" else "${grams.toInt()}g"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = if (goalGrams != null) {
                "$label ${grams.toInt()} of $goalGrams grams"
            } else {
                "$label ${grams.toInt()} grams"
            }
        }
    ) {
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
