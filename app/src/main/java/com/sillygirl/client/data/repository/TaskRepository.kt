package com.sillygirl.client.data.repository

import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.TaskInfo

class TaskRepository {
    suspend fun getTasks(): Result<List<TaskInfo>> {
        return try {
            val response = RetrofitClient.api.getTasks()
            if (response.success) Result.success(response.data)
            else Result.failure(Exception("获取任务列表失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
