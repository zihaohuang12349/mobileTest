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
    suspend fun remove(id: String) {
        deleteSegments(id)
        deleteShipping(id)
    }

    @Query("DELETE FROM shipping WHERE id = :id")
    suspend fun deleteShipping(id: String)

    @Query("DELETE FROM segment WHERE itineraryId = :itineraryId")
    suspend fun deleteSegments(itineraryId: String)
}
