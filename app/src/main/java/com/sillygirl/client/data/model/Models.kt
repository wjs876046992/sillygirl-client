package com.sillygirl.client.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val status: String? = null,
)

data class LoginResponse(
    val status: String? = null,
    val currentAuthority: String? = null,
    val token: String? = null,
)

data class CurrentUserResponse(
    val success: Boolean = false,
    val data: UserData? = null,
)

data class UserData(
    val name: String = "",
    val avatar: String = "",
    val plugins: List<PluginRoute> = emptyList(),
)

data class PluginRoute(
    val path: String = "",
    val name: String = "",
    val component: String = "",
    @SerializedName("create_at") val createAt: String? = null,
)

data class PluginListResponse(
    val success: Boolean = false,
    val data: List<PluginInfo> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
)

data class PluginInfo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val version: String = "",
    val author: String = "",
    val running: Boolean = false,
    val disable: Boolean = false,
    val classes: List<String> = emptyList(),
    val downloads: Int = 0,
    val icon: String = "",
    val debug: Boolean = false,
)

data class FenyongStatResponse(
    val success: Boolean = false,
    val tongji: FenyongStatData? = null,
    val tabs: List<FenyongTab> = emptyList(),
    val data: List<FenyongOrder> = emptyList(),
    val total: Int = 0,
)

data class FenyongStatData(
    @SerializedName("order_num") val orderNum: Int = 0,
    @SerializedName("user_num") val userNum: Int = 0,
    @SerializedName("total_actual") val totalActual: Double = 0.0,
    @SerializedName("total_estimate") val totalEstimate: Double = 0.0,
    @SerializedName("total_rake_actual") val totalRakeActual: Double = 0.0,
    @SerializedName("total_rake_estimate") val totalRakeEstimate: Double = 0.0,
    @SerializedName("total_irake_actual") val totalIrakeActual: Double = 0.0,
    @SerializedName("total_irake_estimate") val totalIrakeEstimate: Double = 0.0,
)

data class FenyongTab(
    val key: String = "",
    val title: String = "",
    val value: String = "",
)

data class FenyongOrder(
    val name: String = "",
    val image: String = "",
    @SerializedName("sku_name") val skuName: String = "",
    val status: String = "",
)

data class MastersResponse(
    val success: Boolean = false,
    val data: List<MasterInfo> = emptyList(),
    val platforms: List<PlatformOption> = emptyList(),
)

data class MasterInfo(
    val id: Int = 0,
    val platform: String = "",
    val nickname: String = "",
    val number: String = "",
    val unix: Long = 0,
)

data class PlatformOption(
    val label: String = "",
    val value: String = "",
)

data class TaskResponse(
    val success: Boolean = false,
    val data: List<TaskInfo> = emptyList(),
)

data class TaskInfo(
    val id: Int = 0,
    @SerializedName("task_id") val taskId: String = "",
    val title: String = "",
    val schedule: String = "",
    val command: String = "",
    val enable: Boolean = false,
    @SerializedName("created_at") val createdAt: Long = 0,
    val remark: String = "",
)
