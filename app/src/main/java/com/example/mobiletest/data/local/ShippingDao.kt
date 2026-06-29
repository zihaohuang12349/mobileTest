package com.example.mobiletest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ShippingDao {

    @Query("SELECT * FROM shipping WHERE userId = :userId ORDER BY expiryTime DESC")
    fun shippingByUserId(userId: String): Flow<List<ShippingEntity>>

    @Query("SELECT * FROM segment WHERE userId = :userId ORDER BY itineraryId ASC, segmentId ASC")
    fun segmentsByUserId(userId: String): Flow<List<SegmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipping(items: List<ShippingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(items: List<SegmentEntity>)

    @Transaction
    suspend fun syncShipping(userId: String, items: List<ShippingEntity>) {
        val newIds = items.map { it.id }.toSet()
        if (newIds.isEmpty()) {
            deleteAllShipping(userId)
        } else {
            deleteStaleShipping(userId, newIds)
            insertShipping(items)
        }
    }

    @Transaction
    suspend fun syncSegments(userId: String, itineraryIds: Set<String>, items: List<SegmentEntity>) {
        if (itineraryIds.isEmpty()) {
            deleteAllSegments(userId)
        } else {
            deleteStaleSegments(userId, itineraryIds)
            if (items.isNotEmpty()) {
                insertSegments(items)
            }
        }
    }

    @Query("DELETE FROM shipping WHERE userId = :userId AND id NOT IN (:ids)")
    suspend fun deleteStaleShipping(userId: String, ids: Set<String>)

    @Query("DELETE FROM segment WHERE userId = :userId AND itineraryId NOT IN (:ids)")
    suspend fun deleteStaleSegments(userId: String, ids: Set<String>)

    @Query("DELETE FROM shipping WHERE userId = :userId")
    suspend fun deleteAllShipping(userId: String)

    @Query("DELETE FROM segment WHERE userId = :userId")
    suspend fun deleteAllSegments(userId: String)

    @Transaction
    suspend fun remove(id: String) {
        deleteSegments(id)
        deleteShipping(id)
    }

    @Query("DELETE FROM shipping WHERE id = :id")
    suspend fun deleteShipping(id: String)

    @Query("DELETE FROM segment WHERE itineraryId = :itineraryId")
    suspend fun deleteSegments(itineraryId: String)
}
