package com.astute.calories.data.remote

import com.astute.calories.data.remote.dto.FdaSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface FdaFoodApi {

    @GET("foods/search")
    suspend fun searchFoods(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("pageSize") pageSize: Int = 10,
        @Query("dataType") dataType: String = "Foundation,SR Legacy,Branded"
    ): FdaSearchResponse
}
