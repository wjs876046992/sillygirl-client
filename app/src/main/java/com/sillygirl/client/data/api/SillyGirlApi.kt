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
    // GET /api/fenyong/dashboard — returns { success, today, yesterday, last7days, lastMonth, platforms, total_settled, total_unsettled, total_orders }
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

    @POST("api/tasks/add")
    @Headers("Content-Type: application/json")
    suspend fun addTask(@Body body: TaskAddRequest): ApiResponse<Any>

    @POST("api/tasks/edit")
    @Headers("Content-Type: application/json")
    suspend fun editTask(@Body body: TaskEditRequest): ApiResponse<Any>

    @POST("api/tasks/del")
    @Headers("Content-Type: application/json")
    suspend fun delTask(@Body body: TaskActionRequest): ApiResponse<Any>

    @POST("api/tasks/setEnable")
    @Headers("Content-Type: application/json")
    suspend fun setTaskEnable(@Body body: TaskSetEnableRequest): ApiResponse<Any>

    @GET("api/tasks/run")
    suspend fun runTask(@Query("task_id") taskId: String): ApiResponse<Any>

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
