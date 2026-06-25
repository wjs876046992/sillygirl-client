package com.sillygirl.client.data.api

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "SillyGirlAPI"

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

    /** 从 Set-Cookie 头中提取 token 值 */
    fun extractTokenFromCookie(setCookie: String?): String? {
        if (setCookie == null) return null
        // Set-Cookie 格式: token=xxx; Path=/; ...; Max-Age=86400
        val parts = setCookie.split(";").map { it.trim() }
        val tokenPart = parts.firstOrNull { it.startsWith("token=") } ?: return null
        return tokenPart.removePrefix("token=").takeIf { it.isNotBlank() }
    }

    /** 请求拦截器 — 添加 Cookie token 和日志 */
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val newRequest = if (token != null) {
            request.newBuilder()
                .header("Cookie", "token=${token!!}")
                .build()
        } else {
            request
        }

        // 打印请求信息
        val url = newRequest.url
        val method = newRequest.method
        val body = newRequest.body
        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "▶ REQUEST: $method $url")
        if (body != null) {
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                val bodyStr = buffer.readUtf8()
                Log.d(TAG, "▶ BODY: $bodyStr")
            } catch (e: Exception) {
                Log.d(TAG, "▶ BODY: (无法读取)")
            }
        }
        Log.d(TAG, "▶ HEADERS: ${newRequest.headers}")

        chain.proceed(newRequest)
    }

    /** 响应拦截器 — 检测会话过期 + 登录时提取 token + 打印响应 */
    private val sessionInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())

        // 打印响应信息
        val url = response.request.url
        val code = response.code
        Log.d(TAG, "◀ RESPONSE: $code $url")
        try {
            val bodyStr = response.peekBody(1024 * 1024).string() // 最多读取 1MB
            Log.d(TAG, "◀ BODY: $bodyStr")
        } catch (e: Exception) {
            Log.d(TAG, "◀ BODY: (无法读取)")
        }
        Log.d(TAG, "═══════════════════════════════════════════")

        // 登录响应：从 Set-Cookie 提取 token（跟 Web 端一致）
        if (url.encodedPath.endsWith("/api/login/account") && response.code == 200) {
            val setCookie = response.header("Set-Cookie")
            val extracted = extractTokenFromCookie(setCookie)
            if (extracted != null) {
                token = extracted
                Log.d(TAG, "▶ LOGIN TOKEN: ${extracted.take(8)}...")
            }
        }

        // 会话过期：清除 token
        if (response.code == 401 || response.code == 403) {
            token = null
            onSessionExpired?.invoke()
        }
        response
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE // 禁用内置日志，使用自定义日志
    }

    private val okHttpClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(sessionInterceptor)
            .addInterceptor(logging)
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
