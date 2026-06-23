package com.sillygirl.client.data.api

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

object RetrofitClient {
    var token: String? = null
    private var _api: SillyGirlApi? = null
    private var _currentServer: String = ""

    /** 会话过期回调 — 由 AppNavGraph 设置 */
    var onSessionExpired: (() -> Unit)? = null

    val gson = GsonBuilder().create()

    /** 设置服务器地址并重建 Retrofit 实例 */
    fun setServer(baseUrl: String) {
        _currentServer = baseUrl.trimEnd('/')
        _api = null // invalidate cached instance
        ApiConfig.setServer(baseUrl)
    }

    /** 获取当前服务器地址 */
    fun currentServerUrl(): String? = _currentServer.ifBlank { null }

    /** 重置所有状态（登出） */
    fun reset() {
        _currentServer = ""
        _api = null
        token = null
    }

    // Cookie manager persists tokens between requests
    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val newRequest = if (token != null) {
            request.newBuilder()
                .header("X-Token", token!!)
                .build()
        } else {
            request
        }
        chain.proceed(newRequest)
    }

    /** 响应拦截器 — 检测会话过期 */
    private val sessionInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        // 后端 401 或返回 success=false 且消息含"未授权"或"登录"时触发
        if (response.code == 401 || response.code == 403) {
            token = null
            onSessionExpired?.invoke()
        }
        response
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(sessionInterceptor)
            .addInterceptor(logging)
            .cookieJar(JavaNetCookieJar(cookieManager))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    val api: SillyGirlApi
        get() {
            _api?.let { return it }
            val baseUrl = requireNotNull(_currentServer) { "Server URL not set" }
            _api = Retrofit.Builder()
                .baseUrl(baseUrl.ensureTrailingSlash())
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(SillyGirlApi::class.java)
            return _api!!
        }
}

private fun String.ensureTrailingSlash() = if (endsWith("/")) this else "$this/"
