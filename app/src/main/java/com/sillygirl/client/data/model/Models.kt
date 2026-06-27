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
    val tab1: Int = 0,
    val tab2: Int = 0,
    val tab3: Int = 0,
    val tab: String = "",
    val classes: Map<String, Int> = emptyMap(),
    val origins: Map<String, String> = emptyMap(),
)

/**
 * 插件列表分页结果
 */
data class PluginListResult(
    val plugins: List<PluginInfo> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val tab1: Int = 0,
    val tab2: Int = 0,
    val tab3: Int = 0,
    val classes: Map<String, Int> = emptyMap(),
    val origins: Map<String, String> = emptyMap(),
) {
    val totalPages: Int get() = if (total > 0) (total + pageSize - 1) / pageSize else 1
    val hasNextPage: Boolean get() = page < totalPages
    val hasPrevPage: Boolean get() = page > 1
}

data class PluginInfo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val version: String = "",
    val author: String = "",
    val origin: String = "",
    val organization: String = "",
    val identified: Boolean = false,
    val running: Boolean = false,
    val disable: Boolean = false,
    val status: Int = 0, // 0=未安装, 1=可升级, 2=已安装, 6=原创
    val classes: List<String> = emptyList(),
    val downloads: Int = 0,
    val icon: String = "",
    val debug: Boolean = false,
    @SerializedName("has_form") val hasForm: Boolean = false,
    val encrypt: Boolean = false,
    val module: Boolean = false,
    val messages: List<PluginMessage> = emptyList(),
    @SerializedName("create_at") val createAt: String = "",
)

data class PluginMessage(
    val unix: Long = 0,
    @SerializedName("class") val messageClass: String = "", // "warn" or "error"
    val version: String = "",
    val content: String = "",
)

// ===== Fenyong =====

/**
 * 统一的佣金统计数据（by_time / by_site 各项共用）
 */
data class FenyongStats(
    val orders: Int = 0,
    val estimate: Double = 0.0,
    val actual: Double = 0.0,
)

/**
 * site × 时段 交叉统计项
 */
data class FenyongCrossItem(
    val site: String = "",
    val period: String = "",
    val orders: Int = 0,
    val estimate: Double = 0.0,
    val actual: Double = 0.0,
)

/**
 * dashboard API 返回（/api/fenyong/dashboard）
 * by_time: today / last7days / lastMonth / total
 * by_site: jd / tb / pdd / total
 * cross: site × period 交叉明细
 */
data class FenyongDashboardResponse(
    val success: Boolean = false,
    @SerializedName("by_time") val byTime: Map<String, FenyongStats> = emptyMap(),
    @SerializedName("by_site") val bySite: Map<String, FenyongStats> = emptyMap(),
    val cross: List<FenyongCrossItem> = emptyList(),
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
    @SerializedName("user_name") val userName: String = "",
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
    val page: Int = 1,
    val total: Int = 0,
)

data class TaskInfo(
    val id: Int = 0,
    @SerializedName("task_id") val taskId: String = "",
    val title: String = "",
    val schedule: String = "",
    val command: String = "",
    val scripts: List<String> = emptyList(),
    val senders: List<TaskSender> = emptyList(),
    val enable: Boolean = false,
    @SerializedName("created_at") val createdAt: Long = 0,
    val remark: String = "",
    val icons: List<TaskIcon> = emptyList(),
)

data class TaskSender(
    @SerializedName("chat_id") val chatId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("platform") val platform: String = "",
    @SerializedName("bot_id") val botId: String = "",
)

data class TaskIcon(
    val link: String = "",
    val title: String = "",
)

/**
 * /api/task/selects 返回的可选项
 */
data class TaskSelectsResponse(
    val success: Boolean = false,
    val data: TaskSelectsData? = null,
)

data class TaskSelectsData(
    val scripts: Map<String, String> = emptyMap(),
    val platforms: Map<String, List<String>> = emptyMap(),
    @SerializedName("user_names") val userNames: List<NameOption> = emptyList(),
    @SerializedName("group_names") val groupNames: List<NameOption> = emptyList(),
)

data class NameOption(
    val label: String = "",
    val value: String = "",
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
 * 任务删除请求
 */
data class TaskDeleteRequest(
    @SerializedName("task_id") val taskId: String,
)

// ===== Storage =====

/**
 * Storage bucket 搜索结果
 */
data class StorageBucket(
    val text: String = "",
    val value: String = "",
)

// ===== Chat / Send =====

data class ChatSelectsResponse(
    val success: Boolean = false,
    val data: ChatSelectsData? = null,
)

data class ChatSelectsData(
    val platforms: Map<String, List<String>> = emptyMap(),
    @SerializedName("user_names") val userNames: List<NameLabel> = emptyList(),
    @SerializedName("group_names") val groupNames: List<NameLabel> = emptyList(),
)

data class NameLabel(
    val label: String = "",
    val value: String = "",
    val platform: String = "",
    val src: String = "",           // 来源: private(私聊), group(群聊)
    val src_chats: List<String> = emptyList(), // 来源群ID列表（group时有值）
)

data class SendMessageRequest(
    val platform: String,
    @SerializedName("bot_id") val botId: String,
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("chat_id") val chatId: String = "",
    val content: String,
)

data class SendMessageResponse(
    val success: Boolean = false,
    val data: SendMessageData? = null,
    val errorMessage: String? = null,
)

data class SendMessageData(
    val platform: String = "",
    @SerializedName("bot_id") val botId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("chat_id") val chatId: String = "",
    @SerializedName("message_id") val messageId: String = "",
)

// ===== Plugin Logs =====

/**
 * 插件日志条目
 */
data class PluginLogEntry(
    val uuid: String = "",
    val level: String = "",
    val content: String = "",
    val unix: Long = 0,
    val version: String = "",
    @SerializedName("plugin_name") val pluginName: String = "",
)

/**
 * 插件日志 API 返回（/api/plugin/logs）
 */
data class PluginLogsResponse(
    val success: Boolean = false,
    val data: List<PluginLogEntry> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 50,
)

/**
 * 插件日志统计
 */
data class PluginLogStats(
    val total: Int = 0,
    @SerializedName("by_level") val byLevel: Map<String, Int> = emptyMap(),
    @SerializedName("oldest_time") val oldestTime: Long = 0,
    @SerializedName("newest_time") val newestTime: Long = 0,
)

/**
 * 插件日志统计 API 返回（/api/plugin/logs/stats）
 */
data class PluginLogStatsResponse(
    val success: Boolean = false,
    val data: PluginLogStats? = null,
)
