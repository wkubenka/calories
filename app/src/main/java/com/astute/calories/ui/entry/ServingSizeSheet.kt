package com.astute.calories.ui.entry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import com.astute.calories.data.local.entity.LogEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServingSizeSheet(
    entry: LogEntry,
    onDismiss: () -> Unit,
    onConfirm: (LogEntry) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var quantity by rememberSaveable { mutableStateOf(entry.quantity.toString()) }
    var calories by rememberSaveable { mutableStateOf(entry.calories.toString()) }

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
                text = "Edit: ${entry.foodName}",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        val newQuantity = quantity.toFloatOrNull() ?: entry.quantity
                        val newCalories = calories.toIntOrNull() ?: entry.calories
                        onConfirm(
                            entry.copy(
                                quantity = newQuantity,
                                calories = newCalories
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}
