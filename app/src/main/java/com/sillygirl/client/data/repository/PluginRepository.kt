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

    suspend fun getPluginDetail(uuid: String): Result<PluginDetail> {
        return try {
            val response = RetrofitClient.api.getPluginDetail(uuid)
            if (response.success && response.data != null) Result.success(response.data)
            else Result.failure(Exception("获取插件详情失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePluginContent(uuid: String, content: String): Result<Unit> {
        return try {
            val response = RetrofitClient.api.updatePluginContent(uuid, mapOf("content" to content))
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("保存插件内容失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadPlugin(uuid: String): Result<Unit> {
        return try {
            val response = RetrofitClient.api.reloadPlugin(mapOf("uuid" to uuid))
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("重载插件失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePluginDebug(uuid: String, debug: Boolean): Result<Unit> {
        return try {
            val response = RetrofitClient.api.togglePluginDebug(mapOf("uuid" to uuid, "debug" to debug))
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("切换调试模式失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePluginForm(uuid: String, formData: Map<String, Any?>): Result<Unit> {
        return try {
            val response = RetrofitClient.api.saveStorage(uuid, formData.mapValues { it.value?.toString() ?: "" })
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("保存表单配置失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
