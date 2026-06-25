package com.sillygirl.client.data.repository

import android.util.Log
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.LoginRequest
import com.sillygirl.client.data.model.UserData

class AuthRepository {
    private val TAG = "AuthRepository"

    suspend fun login(serverUrl: String, username: String, password: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Logging in to $serverUrl with username=$username")
            RetrofitClient.setServer(serverUrl)
            val response = RetrofitClient.api.login(
                LoginRequest(username = username, password = password)
            )
            Log.d(TAG, "Login response: status=${response.status}, authority=${response.currentAuthority}, token=${response.token?.take(10)}...")
            if (response.status == "ok" && response.currentAuthority == "admin") {
                if (!response.token.isNullOrBlank()) {
                    RetrofitClient.token = response.token
                }
                Result.success(true)
            } else {
                Result.failure(Exception("登录失败：用户名密码错误或非管理员"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            Result.failure(Exception("连接失败：${e.message}"))
        }
    }

    suspend fun verifySession(): Result<Boolean> {
        return try {
            val response = RetrofitClient.api.getCurrentUser()
            Log.d(TAG, "Verify session: success=${response.success}, data=${response.data != null}")
            if (response.success && response.data != null) {
                Result.success(true)
            } else {
                Result.failure(Exception("会话已过期"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Verify session failed", e)
            Result.failure(Exception("连接异常：${e.message}"))
        }
    }

    suspend fun getCurrentUserInfo(): Result<UserData> {
        return try {
            val response = RetrofitClient.api.getCurrentUser()
            Log.d(TAG, "getCurrentUser: success=${response.success}, data=${response.data}")
            response.data?.let {
                Log.d(TAG, "User data: name='${it.name}', avatar='${it.avatar}', plugins=${it.plugins.size}")
            }
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("获取用户信息失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUserInfo failed", e)
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try {
            RetrofitClient.api.logout()
        } catch (_: Exception) { }
        RetrofitClient.token = null
    }
}
