package com.example.mobiletest.data.model

data class ApiResponse<T>(
    val code: Int,
    val data: T?,
    val msg: String
)
