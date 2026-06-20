package com.sillygirl.client.data.repository

import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.MasterInfo

class MasterRepository {
    suspend fun getMasters(): Result<List<MasterInfo>> {
        return try {
            val response = RetrofitClient.api.getMasters()
            if (response.success) Result.success(response.data)
            else Result.failure(Exception("获取管理员列表失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
