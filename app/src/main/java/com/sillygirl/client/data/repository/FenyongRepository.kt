package com.sillygirl.client.data.repository

import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.FenyongDashboardResponse
import com.sillygirl.client.data.model.FenyongOrderResponse

class FenyongRepository {

    /**
     * 获取 dashboard 统计数据（今日/昨日/7天/30天 + 平台/结算状态）
     */
    suspend fun getDashboard(): Result<FenyongDashboardResponse> {
        return try {
            val response = RetrofitClient.api.getFenyongDashboard()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取订单列表（带 keyword 搜索和分页）
     */
    suspend fun getOrders(
        keyword: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<FenyongOrderResponse> {
        return try {
            val response = RetrofitClient.api.getFenyongOrders(
                keyword = keyword,
                page = page,
                pageSize = pageSize,
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
