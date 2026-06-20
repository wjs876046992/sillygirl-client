package com.sillygirl.client.data.repository

import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.*

class FenyongRepository {
    suspend fun getStats(
        init: Boolean = true,
        activeKey: String? = null,
        current: Int = 1,
        pageSize: Int = 20,
    ): Result<FenyongStatResponse> {
        return try {
            val response = RetrofitClient.api.getFenyong(
                init = init,
                activeKey = activeKey,
                current = current,
                pageSize = pageSize,
            )
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception("获取分佣数据失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
