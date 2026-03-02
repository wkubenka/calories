package com.astute.calories.data.local.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SavedMealItem(
    val foodName: String,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val servingSize: Float,
    val quantity: Float,
    val barcode: String? = null
)
