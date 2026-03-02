package com.astute.calories.data.remote

import com.astute.calories.data.remote.dto.ProductLookupResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {

    @GET("api/v0/product/{barcode}.json")
    suspend fun getByBarcode(
        @Path("barcode") barcode: String
    ): ProductLookupResponse
}
