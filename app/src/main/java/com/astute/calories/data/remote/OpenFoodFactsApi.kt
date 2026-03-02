package com.astute.calories.data.remote

import com.astute.calories.data.remote.dto.ProductLookupResponse
import com.astute.calories.data.remote.dto.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {

    @GET("api/v2/search")
    suspend fun searchByName(
        @Query("search_terms") query: String,
        @Query("page_size") pageSize: Int = 20,
        @Query("fields") fields: String = "code,product_name,nutriments,serving_size,image_url"
    ): SearchResponse

    @GET("api/v0/product/{barcode}.json")
    suspend fun getByBarcode(
        @Path("barcode") barcode: String
    ): ProductLookupResponse
}
