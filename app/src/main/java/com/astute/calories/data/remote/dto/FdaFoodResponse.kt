package com.astute.calories.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FdaSearchResponse(
    @Json(name = "foods") val foods: List<FdaFoodDto>?,
    @Json(name = "totalHits") val totalHits: Int?,
    @Json(name = "currentPage") val currentPage: Int?,
    @Json(name = "totalPages") val totalPages: Int?
)

@JsonClass(generateAdapter = true)
data class FdaFoodDto(
    @Json(name = "fdcId") val fdcId: Int,
    @Json(name = "description") val description: String?,
    @Json(name = "dataType") val dataType: String?,
    @Json(name = "gtinUpc") val gtinUpc: String?,
    @Json(name = "brandOwner") val brandOwner: String?,
    @Json(name = "servingSize") val servingSize: Float?,
    @Json(name = "servingSizeUnit") val servingSizeUnit: String?,
    @Json(name = "householdServingFullText") val householdServingFullText: String?,
    @Json(name = "foodNutrients") val foodNutrients: List<FdaNutrientDto>?
)

@JsonClass(generateAdapter = true)
data class FdaNutrientDto(
    @Json(name = "nutrientNumber") val nutrientNumber: String?,
    @Json(name = "nutrientName") val nutrientName: String?,
    @Json(name = "unitName") val unitName: String?,
    @Json(name = "value") val value: Float?
)
