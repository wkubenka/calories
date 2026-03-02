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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MacroSummary(
    proteinG: Float,
    carbsG: Float,
    fatG: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroItem(label = "Protein", grams = proteinG)
        MacroItem(label = "Carbs", grams = carbsG)
        MacroItem(label = "Fat", grams = fatG)
    }
}

@Composable
private fun MacroItem(label: String, grams: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${grams.toInt()}g",
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
