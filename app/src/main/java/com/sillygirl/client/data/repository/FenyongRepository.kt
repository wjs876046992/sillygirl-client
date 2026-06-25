package com.sillygirl.client.data.repository

import android.util.Log
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.FenyongDashboardResponse
import com.sillygirl.client.data.model.FenyongOrderResponse

class FenyongRepository {
    private val TAG = "FenyongRepository"

    /**
     * 获取 dashboard 统计数据（今日/昨日/7天/30天 + 平台/结算状态）
     */
    suspend fun getDashboard(): Result<FenyongDashboardResponse> {
        return try {
            val response = RetrofitClient.api.getFenyongDashboard()
            Log.d(TAG, "Dashboard loaded: success=${response.success}, byTime=${response.byTime.keys}, bySite=${response.bySite.keys}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Dashboard failed", e)
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
            Log.d(TAG, "Orders loaded: total=${response.total}, page=${response.page}, count=${response.data.size}")
            response.data.forEachIndexed { i, order ->
                Log.d(TAG, "Order[$i]: name=${order.name}, sku=${order.skuName}, image='${order.image}', site='${order.site}', content=${order.content.size}")
            }
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Orders failed", e)
            Result.failure(e)
        }
    }
}
