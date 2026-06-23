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

    suspend fun getPluginContent(uuid: String): Result<String> {
        return try {
            val keys = "plugins.$uuid"
            val response = RetrofitClient.api.getStorage(keys)
            if (response.success) {
                val data = response.data as? Map<*, *>
                val content = data?.get(keys) as? String ?: ""
                Result.success(content)
            } else {
                Result.failure(Exception("获取插件内容失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePluginContent(uuid: String, content: String): Result<Unit> {
        return try {
            val body = mapOf("plugins.$uuid" to content)
            val response = RetrofitClient.api.saveStorage(uuid, body)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("保存插件内容失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadPlugin(uuid: String): Result<Unit> {
        return try {
            val body = mapOf("plugins.$uuid" to "reload")
            val response = RetrofitClient.api.saveStorage(uuid, body)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("重载插件失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePluginDebug(uuid: String, debug: Boolean): Result<Unit> {
        return try {
            val body = mapOf("plugin_debug.$uuid" to debug.toString())
            val response = RetrofitClient.api.saveStorage(uuid, body)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("切换调试模式失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePluginForm(uuid: String, formData: Map<String, Any?>): Result<Unit> {
        return try {
            val body = formData.mapValues { it.value?.toString() ?: "" }
            val response = RetrofitClient.api.saveStorage(uuid, body)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("保存表单配置失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
