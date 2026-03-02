package com.astute.calories.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "cached_foods")
data class CachedFood(
    @PrimaryKey val barcode: String,
    val name: String,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val servingSizeG: Float?,
    val servingSizeLabel: String? = null,
    val imageUrl: String?,
    val lastAccessed: Instant
)
