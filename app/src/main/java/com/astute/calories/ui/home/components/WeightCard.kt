package com.astute.calories.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.astute.calories.data.local.entity.WeightEntry
import java.util.Locale

@Composable
fun WeightCard(
    todayWeight: WeightEntry?,
    onLogWeight: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { showDialog = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weight",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = todayWeight
                    ?.let { "%.1f lbs".format(Locale.US, it.weightLbs) }
                    ?: "Log today's weight",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }

    if (showDialog) {
        WeightLogDialog(
            initialWeight = todayWeight?.weightLbs,
            onDismiss = { showDialog = false },
            onConfirm = { lbs ->
                onLogWeight(lbs)
                showDialog = false
            }
        )
    }
}

@Composable
private fun WeightLogDialog(
    initialWeight: Float?,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var text by remember {
        mutableStateOf(initialWeight?.let { "%.1f".format(Locale.US, it) } ?: "")
    }
    val parsed = text.trim().toFloatOrNull()
    // Guard against non-finite (Infinity/NaN slip through toFloatOrNull) and
    // implausible values; 1500 lbs is well beyond any real bodyweight.
    val isValid = parsed != null && parsed.isFinite() && parsed > 0f && parsed <= 1500f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log weight") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Weight (lbs)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
