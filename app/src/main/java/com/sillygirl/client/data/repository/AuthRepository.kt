package com.sillygirl.client.data.repository

import com.sillygirl.client.data.api.ApiConfig
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.UserData

class AuthRepository {
    suspend fun login(serverUrl: String, username: String, password: String): Result<Boolean> {
        return try {
            ApiConfig.setServer(serverUrl)
            val response = RetrofitClient.api.login(
                mapOf("username" to username, "password" to password)
            )
            if (response.status == "ok" && response.currentAuthority == "admin") {
                if (!response.token.isNullOrBlank()) {
                    RetrofitClient.token = response.token
                }
                Result.success(true)
            } else {
                Result.failure(Exception("登录失败：用户名密码错误或非管理员"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("连接失败：${e.message}"))
        }
    }

    suspend fun verifySession(): Result<Boolean> {
        return try {
            val response = RetrofitClient.api.getCurrentUser()
            if (response.success && response.data != null) {
                Result.success(true)
            } else {
                Result.failure(Exception("会话已过期"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("连接异常：${e.message}"))
        }
    }

    suspend fun getCurrentUserInfo(): Result<UserData> {
        return try {
            val response = RetrofitClient.api.getCurrentUser()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("获取用户信息失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            kotlinx.coroutines.runBlocking {
                RetrofitClient.api.logout()
            }
        } catch (_: Exception) { }
        RetrofitClient.token = null
    }
}
