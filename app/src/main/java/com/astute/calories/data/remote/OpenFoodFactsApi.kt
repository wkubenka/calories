package com.astute.calories.data.remote

import com.astute.calories.data.remote.dto.ProductLookupResponse
import com.astute.calories.data.remote.dto.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {

    @GET("cgi/search.pl")
    suspend fun searchByName(
        @Query("search_terms") query: String,
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): SearchResponse

    @GET("api/v0/product/{barcode}.json")
    suspend fun getByBarcode(
        @Path("barcode") barcode: String
    ): ProductLookupResponse
}
