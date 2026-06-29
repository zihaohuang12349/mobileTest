package com.example.mobiletest.data

import android.util.Log
import com.example.mobiletest.data.api.RetrofitClient
import com.example.mobiletest.data.local.AppDatabase
import com.example.mobiletest.data.local.toEntity
import com.example.mobiletest.data.local.toModel
import com.example.mobiletest.data.model.CreateByValidityRequest
import com.example.mobiletest.data.model.DeleteShippingRequest
import com.example.mobiletest.data.model.ListShippingRequest
import com.example.mobiletest.data.model.ShippingItinerary
import com.example.mobiletest.data.model.UpdateShippingRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class ShippingRepository(context: android.content.Context) {

    private val dao = AppDatabase.getInstance(context).shippingDao()
    private val api = RetrofitClient.shippingApi

    fun observe(userId: String): Flow<List<ShippingItinerary>> {
        return combine(
            dao.shippingByUserId(userId),
            dao.segmentsByUserId(userId)
        ) { shipping, segments ->
            val byItinerary = segments.groupBy { it.itineraryId }
            shipping.map { ship ->
                val segs = byItinerary[ship.id].orEmpty()
                ShippingItinerary(
                    id = ship.id,
                    userId = ship.userId,
                    shipReference = ship.shipReference,
                    shipToken = ship.shipToken,
                    canIssueTicketChecking = ship.canIssueTicketChecking,
                    expiryTime = ship.expiryTime,
                    duration = ship.duration,
                    segments = segs.map { it.toModel() }.ifEmpty { null }
                )
            }
        }
    }

    suspend fun refresh(userId: String): Result<List<ShippingItinerary>> = call {
        val rsp = api.listAll(ListShippingRequest(userId))
        if (rsp.code != 0) throw Exception(rsp.msg)
        val data = rsp.data ?: emptyList()
        dao.syncShipping(userId, data.map { it.toEntity() })
        dao.syncSegments(
            userId,
            itineraryIds = data.mapNotNull { it.id }.toSet(),
            items = data.flatMap { item ->
                item.segments?.map { it.toEntity(item.id ?: "", item.userId ?: userId) } ?: emptyList()
            }
        )
        Log.d("ShippingRepository", "refresh: ${data.size} items")
        data
    }

    suspend fun create(userId: String, validitySeconds: Long): Result<ShippingItinerary> = call {
        val rsp = api.createByValidity(CreateByValidityRequest(userId, validitySeconds))
        if (rsp.code != 0) throw Exception(rsp.msg)
        val data = rsp.data ?: throw Exception("empty response")
        save(data)
        data
    }

    suspend fun update(id: String, userId: String, expiry: String): Result<ShippingItinerary> = call {
        val rsp = api.update(id, UpdateShippingRequest(userId, expiry))
        if (rsp.code != 0) throw Exception(rsp.msg)
        val data = rsp.data ?: throw Exception("empty response")
        save(data)
        data
    }

    suspend fun delete(id: String, userId: String): Result<Unit> = call {
        val rsp = api.delete(id, DeleteShippingRequest(userId))
        if (rsp.code != 0) throw Exception(rsp.msg)
        dao.remove(id)
    }

    private suspend fun save(data: ShippingItinerary) {
        dao.insertShipping(listOf(data.toEntity()))
        dao.deleteSegments(data.id ?: "")
        data.segments?.let { s ->
            dao.insertSegments(s.map { it.toEntity(data.id ?: "", data.userId ?: "") })
        }
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> {
        return try {
            Result.success(withContext(Dispatchers.IO) { block() })
        } catch (e: Exception) {
            Log.e("ShippingRepository", "failed", e)
            Result.failure(e)
        }
    }
}
