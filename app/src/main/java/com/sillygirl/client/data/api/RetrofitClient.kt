package com.sillygirl.client.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {
    var token: String? = null

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
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
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
                .addConverterFactory(
                    retrofit2.converter.kotlinx.serialization.asConverterFactory(
                        json,
                        okhttp3.MediaType.get("application/json")
                    )
                )
                .build()
                .create(SillyGirlApi::class.java)
        }
}

private fun String.ensureTrailingSlash() = if (endsWith("/")) this else "$this/"
