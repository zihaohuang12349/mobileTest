package com.example.mobiletest.data.model

data class ListShippingRequest(val userId: String)

data class CreateByValidityRequest(
    val userId: String,
    val validitySeconds: Long
)

data class UpdateShippingRequest(
    val userId: String,
    val expiryTime: String
)

data class DeleteShippingRequest(val userId: String)
