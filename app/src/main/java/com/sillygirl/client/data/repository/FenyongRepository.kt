package com.sillygirl.client.data.repository

import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.*

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
     * 获取 tongji 统计（12 项指标 + 用户列表）
     */
    suspend fun getTongji(
        startTime: Long? = null,
        endTime: Long? = null,
        site: String? = null,
        user: String? = null,
    ): Result<FenyongTongjiData> {
        return try {
            val response = RetrofitClient.api.getFenyongTongji(
                startTime = startTime?.toString(),
                endTime = endTime?.toString(),
                site = site,
                user = user,
            )
            if (response.success && response.data != null) {
                Result.success(response.data!!)
            } else {
                Result.success(FenyongTongjiData()) // 空数据也算成功
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取订单列表
     */
    suspend fun getOrders(
        tab: String? = null,
        site: String? = null,
        user: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<FenyongOrderResponse> {
        return try {
            val response = RetrofitClient.api.getFenyongOrders(
                activeKey = tab,
                site = site,
                user = user,
                startTime = startTime?.toString(),
                endTime = endTime?.toString(),
                page = page,
                pageSize = pageSize,
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存用户配置（时间范围、用户选择等）
     */
    suspend fun saveStorage(keys: List<String>, data: Map<String, Any>): Result<Unit> {
        return try {
            val serialized = data.mapValues { (_, v) ->
                if (v is List<*>) v.joinToString(";;")
                else v.toString()
            }
            RetrofitClient.api.saveStorage(
                uuid = "fenyong_config",
                body = serialized,
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取用户配置
     */
    suspend fun getStorage(keys: List<String>): Result<Map<String, String>> {
        return try {
            val response = RetrofitClient.api.getStorage(
                keys = keys.joinToString(","),
            )
            if (response.success && response.data != null) {
                @Suppress("UNCHECKED_CAST")
                Result.success(response.data as Map<String, String>)
            } else {
                Result.success(emptyMap())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
