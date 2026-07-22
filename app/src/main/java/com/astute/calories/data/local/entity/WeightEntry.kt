package com.astute.calories.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey val date: LocalDate,   // one row per day; re-logging overwrites
    val weightLbs: Float,
    val recordedAt: Instant
)
