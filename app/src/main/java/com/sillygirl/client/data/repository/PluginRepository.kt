package com.sillygirl.client.data.repository

import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.*

class PluginRepository {
    suspend fun getInstalledPlugins(): Result<List<PluginInfo>> {
        return try {
            val response = RetrofitClient.api.getPluginList(activeKey = "tab1")
            if (response.success) Result.success(response.data)
            else Result.failure(Exception("获取插件列表失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailablePlugins(): Result<List<PluginInfo>> {
        return try {
            val response = RetrofitClient.api.getPluginList(activeKey = "tab2")
            if (response.success) Result.success(response.data)
            else Result.failure(Exception("获取可用插件失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
