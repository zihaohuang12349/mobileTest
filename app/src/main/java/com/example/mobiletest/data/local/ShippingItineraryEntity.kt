package com.example.mobiletest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mobiletest.data.model.LocationInfo
import com.example.mobiletest.data.model.OriginAndDestinationPair
import com.example.mobiletest.data.model.Segment
import com.example.mobiletest.data.model.ShippingItinerary

@Entity(tableName = "shipping")
data class ShippingEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val shipReference: String?,
    val shipToken: String?,
    val canIssueTicketChecking: Boolean,
    val expiryTime: String?,
    val duration: Long
)

@Entity(tableName = "segment", primaryKeys = ["itineraryId", "segmentId"])
data class SegmentEntity(
    val itineraryId: String,
    val userId: String,
    val segmentId: Int,
    val originCode: String?,
    val originDisplayName: String?,
    val originUrl: String?,
    val originCity: String?,
    val destinationCode: String?,
    val destinationDisplayName: String?,
    val destinationUrl: String?,
    val destinationCity: String?
)

fun SegmentEntity.toModel(): Segment {
    return Segment(
        id = segmentId,
        originAndDestinationPair = OriginAndDestinationPair(
            origin = if (originCode != null || originDisplayName != null || originUrl != null) {
                LocationInfo(originCode, originDisplayName, originUrl)
            } else null,
            originCity = originCity,
            destination = if (destinationCode != null || destinationDisplayName != null || destinationUrl != null) {
                LocationInfo(destinationCode, destinationDisplayName, destinationUrl)
            } else null,
            destinationCity = destinationCity
        )
    )
}

fun ShippingItinerary.toEntity(): ShippingEntity {
    return ShippingEntity(
        id = id ?: "",
        userId = userId ?: "",
        shipReference = shipReference,
        shipToken = shipToken,
        canIssueTicketChecking = canIssueTicketChecking,
        expiryTime = expiryTime,
        duration = duration
    )
}

fun Segment.toEntity(itineraryId: String, userId: String): SegmentEntity {
    return SegmentEntity(
        itineraryId = itineraryId,
        userId = userId,
        segmentId = id,
        originCode = originAndDestinationPair?.origin?.code,
        originDisplayName = originAndDestinationPair?.origin?.displayName,
        originUrl = originAndDestinationPair?.origin?.url,
        originCity = originAndDestinationPair?.originCity,
        destinationCode = originAndDestinationPair?.destination?.code,
        destinationDisplayName = originAndDestinationPair?.destination?.displayName,
        destinationUrl = originAndDestinationPair?.destination?.url,
        destinationCity = originAndDestinationPair?.destinationCity
    )
}
