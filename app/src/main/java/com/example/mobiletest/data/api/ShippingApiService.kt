package com.example.mobiletest.data.api

import com.example.mobiletest.data.model.ApiResponse
import com.example.mobiletest.data.model.CreateByValidityRequest
import com.example.mobiletest.data.model.DeleteShippingRequest
import com.example.mobiletest.data.model.ListShippingRequest
import com.example.mobiletest.data.model.ShippingItinerary
import com.example.mobiletest.data.model.UpdateShippingRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ShippingApiService {

    @POST("api/shipping/list")
    suspend fun listAll(
        @Body request: ListShippingRequest
    ): ApiResponse<List<ShippingItinerary>>

    @POST("api/shipping/create-by-validity")
    suspend fun createByValidity(
        @Body request: CreateByValidityRequest
    ): ApiResponse<ShippingItinerary>

    @POST("api/shipping/update/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body request: UpdateShippingRequest
    ): ApiResponse<ShippingItinerary>

    @POST("api/shipping/delete/{id}")
    suspend fun delete(
        @Path("id") id: String,
        @Body request: DeleteShippingRequest
    ): ApiResponse<String>
}
