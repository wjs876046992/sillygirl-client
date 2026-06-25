package com.sillygirl.client.data.api

import com.sillygirl.client.data.model.*
import retrofit2.http.*

interface SillyGirlApi {

    // ===== Auth =====
    @POST("api/login/account")
    @Headers("Content-Type: application/json")
    suspend fun login(@Body body: LoginRequest): LoginResponse

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
        @Query("class") classFilter: String? = null,
        @Query("origin[]") origins: List<String>? = null,
        @Query("keyword") keyword: String? = null,
    ): PluginListResponse

    @POST("api/plugins/run")
    @Headers("Content-Type: application/json")
    suspend fun runPlugin(@Body body: PluginRequest): ApiResponse<Any>

    @POST("api/plugins/stop")
    @Headers("Content-Type: application/json")
    suspend fun stopPlugin(@Body body: PluginRequest): ApiResponse<Any>

    @POST("api/plugins/install")
    @Headers("Content-Type: application/json")
    suspend fun installPlugin(@Body body: PluginRequest): ApiResponse<Any>

    @POST("api/plugins/uninstall")
    @Headers("Content-Type: application/json")
    suspend fun uninstallPlugin(@Body body: PluginRequest): ApiResponse<Any>

    // ===== Fenyong =====
    // GET /api/fenyong/dashboard — returns { success, by_time: {today, last7days, lastMonth, total}, by_site: {jd, tb, pdd, total} }
    @GET("api/fenyong/dashboard")
    suspend fun getFenyongDashboard(): FenyongDashboardResponse

    // GET /api/fenyong/orders — returns { success, data: [...], page, total }
    @GET("api/fenyong/orders")
    suspend fun getFenyongOrders(
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): FenyongOrderResponse

    // ===== Masters =====
    @GET("api/master/list")
    suspend fun getMasters(): MastersResponse

    @POST("api/master/add")
    @Headers("Content-Type: application/json")
    suspend fun addMaster(@Body body: MasterAddRequest): ApiResponse<Any>

    @POST("api/master/del")
    @Headers("Content-Type: application/json")
    suspend fun delMaster(@Body body: MasterDelRequest): ApiResponse<Any>

    // ===== Tasks =====
    @GET("api/tasks")
    suspend fun getTasks(
        @Query("current") current: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): TaskResponse

    @POST("api/tasks")
    @Headers("Content-Type: application/json")
    suspend fun saveTask(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Any>

    @HTTP(method = "DELETE", path = "api/tasks", hasBody = true)
    @Headers("Content-Type: application/json")
    suspend fun deleteTask(@Body body: Map<String, String>): ApiResponse<Any>

    @GET("api/tasks/run")
    suspend fun runTask(@Query("task_id") taskId: String): ApiResponse<Any>

    @GET("api/task/selects")
    suspend fun getTaskSelects(@Query("task_id") taskId: String = ""): TaskSelectsResponse

    // ===== Storage =====
    @PUT("api/storage")
    @Headers("Content-Type: application/json")
    suspend fun saveStorage(
        @Query("uuid") uuid: String? = null,
        @Body body: Map<String, String>,
    ): ApiResponse<Any>

    @GET("api/storage")
    suspend fun getStorage(
        @Query("keys") keys: String,
    ): ApiResponse<Any>

    @GET("api/storage")
    suspend fun searchStorage(
        @Query("search") search: String,
    ): ApiResponse<List<StorageBucket>>
}
