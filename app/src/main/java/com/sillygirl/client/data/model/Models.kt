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
    val title: String = "",
    val description: String = "",
    val icon: String = "",
    val origin: String = "",
    val version: String = "v1.0.0",
    val author: String = "",
    val running: Boolean = false,
    val disable: Boolean = false,
    val debug: Boolean = false,
    @SerializedName("has_form") val hasForm: Boolean = false,
    val classes: List<String> = emptyList(),
    val formFields: List<PluginFormField> = emptyList(),
)

data class PluginDetailResponse(
    val success: Boolean = false,
    val data: PluginDetail? = null,
)

data class PluginDetail(
    val uuid: String = "",
    val content: String = "",
    val form: List<PluginFormField> = emptyList(),
    val debug: Boolean = false,
    val disable: Boolean = false,
)

data class PluginFormField(
    val key: String = "",
    val label: String = "",
    val type: String = "text", // text, number, switch, select
    val value: Any? = null,
    val options: List<PluginFormOption> = emptyList(),
    val tooltip: String = "",
    val required: Boolean = false,
)

data class PluginFormOption(
    val label: String = "",
    val value: String = "",
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
    @SerializedName("has_form") val hasForm: Boolean = false,
)

// ===== Fenyong =====

/**
 * 统计周期数据（dashboard 中 today/yesterday/last7days/lastMonth 的结构）
 */
data class FenyongPeriodStats(
    val orders: Int = 0,
    val estimate: Double = 0.0,
    val actual: Double = 0.0,
)

/**
 * 平台统计（dashboard 中 platforms 的结构）
 */
data class FenyongPlatformStats(
    val orders: Int = 0,
    val estimate: Double = 0.0,
    val actual: Double = 0.0,
)

/**
 * dashboard API 返回（/api/fenyong/dashboard）
 */
data class FenyongDashboardResponse(
    val success: Boolean = false,
    val today: FenyongPeriodStats = FenyongPeriodStats(),
    val yesterday: FenyongPeriodStats = FenyongPeriodStats(),
    val last7days: FenyongPeriodStats = FenyongPeriodStats(),
    val lastMonth: FenyongPeriodStats = FenyongPeriodStats(),
    @SerializedName("platforms") val platforms: Map<String, FenyongPlatformStats> = emptyMap(),
    @SerializedName("total_settled") val totalSettled: Double = 0.0,
    @SerializedName("total_unsettled") val totalUnsettled: Double = 0.0,
    @SerializedName("total_orders") val totalOrders: Int = 0,
)

/**
 * tongji API 返回（/api/fenyong/tongji）- 12 项统计指标 + 用户列表
 */
data class FenyongTongjiResponse(
    val success: Boolean = false,
    val data: FenyongTongjiData? = null,
)

data class FenyongTongjiData(
    @SerializedName("order_num") val orderNum: Int = 0,
    @SerializedName("user_num") val userNum: Int = 0,
    @SerializedName("total_actual") val totalActual: Double = 0.0,
    @SerializedName("total_estimate") val totalEstimate: Double = 0.0,
    @SerializedName("total_rake_actual") val totalRakeActual: Double = 0.0,
    @SerializedName("total_rake_estimate") val totalRakeEstimate: Double = 0.0,
    @SerializedName("total_irake_actual") val totalIrakeActual: Double = 0.0,
    @SerializedName("total_irake_estimate") val totalIrakeEstimate: Double = 0.0,
    @SerializedName("total_irake_actual_pct") val totalIrakeActualPct: Double = 0.0,
    @SerializedName("total_irake_estimate_pct") val totalIrakeEstimatePct: Double = 0.0,
    @SerializedName("total_iactual") val totalIactual: Double = 0.0,
    @SerializedName("total_iestimate") val totalIestimate: Double = 0.0,
    val results: List<FenyongUserItem> = emptyList(),
)

data class FenyongUserItem(
    val label: String = "",
    val value: String = "",
    val count: Int = 0,
)

/**
 * 订单列表 API 返回（/api/fanyong）
 */
data class FenyongOrderResponse(
    val success: Boolean = false,
    val data: List<FenyongOrder> = emptyList(),
    val tongji: FenyongTongjiData? = null,
    val tabs: List<FenyongTab> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
)

data class FenyongOrder(
    val name: String = "",
    val image: String = "",
    @SerializedName("sku_name") val skuName: String = "",
    val status: String = "",
    @SerializedName("created_time") val createdTime: Long = 0,
    val site: String = "",
    @SerializedName("sku_id") val skuId: String = "",
    @SerializedName("order_id") val orderId: String = "",
    @SerializedName("estimate") val estimate: Double = 0.0,
    @SerializedName("actual") val actual: Double = 0.0,
    @SerializedName("content") val content: List<FenyongOrderContent> = emptyList(),
    val bind: FenyongBind? = null,
)

data class FenyongOrderContent(
    val label: String = "",
    val value: Any? = null,
    val status: String = "",
)

data class FenyongBind(
    val platform: String = "",
    @SerializedName("user_id") val userId: String = "",
)

data class FenyongTab(
    val key: String = "",
    val title: String = "",
    val value: String = "",
)

/**
 * 时间范围常量
 */
data object FenyongTimeRange {
    const val ALL = 0       // 全部
    const val TODAY = 1     // 今天
    const val YESTERDAY = 2 // 昨天
    const val LAST_7D = 7   // 7天内
    const val LAST_MONTH = 30 // 一个月内
    const val LAST_MONTH_2 = 60 // 上个月
    const val LAST_YEAR = 365 // 一年内
    const val LAST_YEAR_2 = 730 // 去年
    const val CUSTOM = -1   // 自定义

    val options = listOf(
        TimeRangeOption(0, "全部", "全部订单"),
        TimeRangeOption(1, "今天", "今天订单"),
        TimeRangeOption(2, "昨天", "昨天订单"),
        TimeRangeOption(7, "7天内", "最近7天订单"),
        TimeRangeOption(30, "一个月内", "最近一个月订单"),
        TimeRangeOption(60, "上个月", "上一个月订单"),
        TimeRangeOption(365, "一年内", "最近一年订单"),
        TimeRangeOption(730, "去年", "去年订单"),
        TimeRangeOption(-1, "自定义", "自定义时间范围"),
    )
}

data class TimeRangeOption(
    val value: Int,
    val label: String,
    val description: String,
)

// ===== Masters =====

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

// ===== Tasks =====

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
    val icons: List<TaskIcon> = emptyList(),
)

data class TaskIcon(
    val link: String = "",
    val title: String = "",
)

// ===== Request Models (类型安全的请求数据类) =====

/**
 * 登录请求
 */
data class LoginRequest(
    val username: String,
    val password: String,
)

/**
 * 插件操作请求 (run/stop/install/uninstall)
 */
data class PluginRequest(
    val name: String,
)

/**
 * 管理员添加请求
 */
data class MasterAddRequest(
    val platform: String,
    val number: String,
)

/**
 * 管理员删除请求
 */
data class MasterDelRequest(
    val id: String,
)

/**
 * 任务添加/编辑请求
 */
data class TaskAddRequest(
    val title: String,
    val schedule: String,
    val command: String,
    val remark: String = "",
)

data class TaskEditRequest(
    val id: String,
    val title: String,
    val schedule: String,
    val command: String,
    val remark: String = "",
)

/**
 * 任务删除/运行请求
 */
data class TaskActionRequest(
    val id: String,
)

/**
 * 任务启用/禁用请求
 */
data class TaskSetEnableRequest(
    val id: String,
    val enable: Boolean,
)
