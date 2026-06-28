package com.example.mobiletest.data.model

data class ShippingItinerary(
    val id: String?,
    val userId: String?,
    val shipReference: String?,
    val shipToken: String?,
    val canIssueTicketChecking: Boolean,
    val expiryTime: String?,
    val duration: Long,
    val segments: List<Segment>?
)
