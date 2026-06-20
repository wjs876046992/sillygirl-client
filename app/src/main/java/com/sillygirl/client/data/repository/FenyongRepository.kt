package com.sillygirl.client.data.repository

import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.*

class FenyongRepository {
    suspend fun getDashboard(): Result<FenyongDashboardResponse> {
        return try {
            val response = RetrofitClient.api.getFenyongDashboard()
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception("获取分佣数据失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrders(
        tab: String? = null,
        site: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<FenyongOrderResponse> {
        return try {
            val response = RetrofitClient.api.getFenyongOrders(
                tab = tab,
                site = site,
                page = page,
                pageSize = pageSize,
            )
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception("获取订单数据失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
