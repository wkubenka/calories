package com.astute.calories.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CalorieProgressRing(
    consumed: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (consumed.toFloat() / goal).coerceIn(0f, 1.5f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "ring_progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val overColor = MaterialTheme.colorScheme.error

    val percent = if (goal > 0) (consumed * 100 / goal) else 0
    val description = "$consumed of $goal calories, $percent percent"

    Box(
        modifier = modifier
            .size(180.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 14.dp.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )

            // Progress
            val sweepAngle = (animatedProgress.coerceAtMost(1f) * 360f)
            drawArc(
                color = if (animatedProgress > 1f) overColor else primaryColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = stroke
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$consumed",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "/ $goal kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
