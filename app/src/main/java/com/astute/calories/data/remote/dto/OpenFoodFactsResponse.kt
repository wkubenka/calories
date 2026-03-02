package com.astute.calories.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponse(
    @Json(name = "products") val products: List<ProductDto>?
)

@JsonClass(generateAdapter = true)
data class ProductLookupResponse(
    @Json(name = "status") val status: Int?,
    @Json(name = "product") val product: ProductDto?
)

@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "code") val code: String?,
    @Json(name = "product_name") val productName: String?,
    @Json(name = "nutriments") val nutriments: NutrimentsDto?,
    @Json(name = "serving_size") val servingSize: String?,
    @Json(name = "image_url") val imageUrl: String?
)

@JsonClass(generateAdapter = true)
data class NutrimentsDto(
    @Json(name = "energy-kcal_100g") val energyKcal100g: Float?,
    @Json(name = "proteins_100g") val proteins100g: Float?,
    @Json(name = "carbohydrates_100g") val carbohydrates100g: Float?,
    @Json(name = "fat_100g") val fat100g: Float?
)
