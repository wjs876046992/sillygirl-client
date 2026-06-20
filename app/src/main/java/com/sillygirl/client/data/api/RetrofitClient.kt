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
    val gson = GsonBuilder()
        .setLenient()
        .create()

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

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .cookieJar(JavaNetCookieJar(cookieManager))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    val api: SillyGirlApi
        get() {
            val baseUrl = ApiConfig.serverBaseUrl
            require(baseUrl.isNotBlank()) { "Server URL not set. Please login first." }
            return Retrofit.Builder()
                .baseUrl(baseUrl.ensureTrailingSlash())
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(SillyGirlApi::class.java)
        }
}

private fun String.ensureTrailingSlash() = if (endsWith("/")) this else "$this/"
