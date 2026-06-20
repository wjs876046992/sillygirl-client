package com.sillygirl.client.data.api

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val status: String? = null,
)

@Serializable
data class LoginResponse(
    val status: String? = null,
    val currentAuthority: String? = null,
    val token: String? = null,
)

@Serializable
data class CurrentUserResponse(
    val success: Boolean = false,
    val data: UserData? = null,
)

@Serializable
data class UserData(
    val name: String = "",
    val avatar: String = "",
    val plugins: List<PluginRoute> = emptyList(),
)

@Serializable
data class PluginRoute(
    val path: String = "",
    val name: String = "",
    val component: String = "",
    val create_at: String? = null,
)

@Serializable
data class DashboardStats(
    val today: PeriodStat = PeriodStat(),
    val yesterday: PeriodStat = PeriodStat(),
    val last7days: PeriodStat = PeriodStat(),
    val lastMonth: PeriodStat = PeriodStat(),
    val pluginsInstalled: Int = 0,
    val taskCount: Int = 0,
)

@Serializable
data class PeriodStat(
    val orders: Int = 0,
    val estimate: Double = 0.0,
    val actual: Double = 0.0,
)

@Serializable
data class PluginListResponse(
    val success: Boolean = false,
    val data: List<PluginInfo> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
)

@Serializable
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

@Serializable
data class FenyongResponse(
    val success: Boolean = false,
    val data: List<FenyongOrder> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class FenyongOrder(
    val name: String = "",
    val image: String = "",
    val sku_name: String = "",
    val status: String = "",
)

@Serializable
data class FenyongStatResponse(
    val success: Boolean = false,
    val tongji: FenyongStatData? = null,
    val tabs: List<FenyongTab> = emptyList(),
    val data: List<FenyongOrder> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class FenyongStatData(
    val order_num: Int = 0,
    val user_num: Int = 0,
    val total_actual: Double = 0.0,
    val total_estimate: Double = 0.0,
    val total_rake_actual: Double = 0.0,
    val total_rake_estimate: Double = 0.0,
    val total_irake_actual: Double = 0.0,
    val total_irake_estimate: Double = 0.0,
)

@Serializable
data class FenyongTab(
    val key: String = "",
    val title: String = "",
    val value: String = "",
)

@Serializable
data class MastersResponse(
    val success: Boolean = false,
    val data: List<MasterInfo> = emptyList(),
    val platforms: List<PlatformOption> = emptyList(),
)

@Serializable
data class MasterInfo(
    val id: Int = 0,
    val platform: String = "",
    val nickname: String = "",
    val number: String = "",
    val unix: Long = 0,
)

@Serializable
data class PlatformOption(
    val label: String = "",
    val value: String = "",
)

@Serializable
data class TaskResponse(
    val success: Boolean = false,
    val data: List<TaskInfo> = emptyList(),
)

@Serializable
data class TaskInfo(
    val id: Int = 0,
    val task_id: String = "",
    val title: String = "",
    val schedule: String = "",
    val command: String = "",
    val enable: Boolean = false,
    val created_at: Long = 0,
    val remark: String = "",
)
