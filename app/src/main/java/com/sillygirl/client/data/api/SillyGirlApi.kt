package com.sillygirl.client.data.api

import com.sillygirl.client.data.model.*
import retrofit2.http.*

interface SillyGirlApi {

    // ===== Auth =====

    @POST("api/login/account")
    @Headers("Content-Type: application/json")
    suspend fun login(@Body body: Map<String, String>): LoginResponse

    @GET("api/currentUser")
    suspend fun getCurrentUser(): CurrentUserResponse

    @POST("api/login/outLogin")
    suspend fun logout(): Any

    // ===== Plugins =====

    @GET("api/plugins/list.json")
    suspend fun getPluginList(
        @Query("current") current: Int = 1,
        @Query("pageSize") pageSize: Int = 100,
        @Query("activeKey") activeKey: String = "tab1",
    ): PluginListResponse

    // ===== Fenyong =====

    @GET("api/fenyong")
    suspend fun getFenyong(
        @Query("init") init: Boolean? = null,
        @Query("user") user: String? = null,
        @Query("startTime") startTime: Long? = null,
        @Query("endTime") endTime: Long? = null,
        @Query("activeKey") activeKey: String? = null,
        @Query("current") current: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
    ): FenyongStatResponse

    // ===== Masters =====

    @GET("api/master/list")
    suspend fun getMasters(): MastersResponse

    // ===== Tasks =====

    @GET("api/tasks")
    suspend fun getTasks(
        @Query("current") current: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): TaskResponse

    // ===== Storage (用于插件管理) =====

    @PUT("api/storage")
    @Headers("Content-Type: application/json")
    suspend fun saveStorage(
        @Query("uuid") uuid: String,
        @Body body: Map<String, String>,
    ): ApiResponse<Any>

    @GET("api/storage")
    suspend fun getStorage(
        @Query("keys") keys: String,
    ): ApiResponse<Any>
}
