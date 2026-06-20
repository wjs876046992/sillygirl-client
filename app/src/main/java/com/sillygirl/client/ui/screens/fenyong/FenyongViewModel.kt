package com.sillygirl.client.ui.screens.fenyong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.model.*
import com.sillygirl.client.data.repository.FenyongRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 分佣页面 UI 状态
 */
data class FenyongUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // 筛选
    val activeTab: String = "tab1",  // tab1=正常 tab3=已结算 tab4=未结算 tab5=无佣金
    val activeSite: String = "all",  // all, jd, tb, pdd
    val activeUser: String = "#",    // #全部 #已绑 #未绑 或 platform#user_id
    val timeRange: Int = FenyongTimeRange.ALL,
    val startTimestamp: Long? = null,
    val endTimestamp: Long? = null,

    // 数据
    val tongji: FenyongTongjiData? = null,
    val dashboard: FenyongDashboardResponse? = null,
    val orders: List<FenyongOrder> = emptyList(),
    val tabs: List<FenyongTab> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val hasMore: Boolean = true,
)

class FenyongViewModel : ViewModel() {
    private val repository = FenyongRepository()
    private val _uiState = MutableStateFlow(FenyongUiState())
    val uiState: StateFlow<FenyongUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var tongjiJob: Job? = null

    init {
        loadData()
        loadTongji()
    }

    // ===== 数据加载 =====

    /**
     * 加载订单列表 + dashboard
     */
    fun loadData(page: Int = 1, refresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (refresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val s = _uiState.value
            val startTs = s.startTimestamp
            val endTs = s.endTimestamp

            val dashboardDeferred = async { repository.getDashboard() }
            val ordersDeferred = async {
                repository.getOrders(
                    tab = s.activeTab,
                    site = if (s.activeSite == "all") null else s.activeSite,
                    user = if (s.activeUser == "#") null else s.activeUser,
                    startTime = startTs,
                    endTime = endTs,
                    page = page,
                    pageSize = 20,
                )
            }

            val dashResult = dashboardDeferred.await()
            val ordersResult = ordersDeferred.await()

            _uiState.update {
                copy(
                    isLoading = false,
                    isRefreshing = false,
                    dashboard = dashResult.getOrNull(),
                    orders = ordersResult.getOrNull()?.data ?: emptyList(),
                    tabs = ordersResult.getOrNull()?.tabs ?: emptyList(),
                    page = ordersResult.getOrNull()?.page ?: 1,
                    total = ordersResult.getOrNull()?.total ?: 0,
                    hasMore = (page) * 20 < (ordersResult.getOrNull()?.total ?: 0),
                    error = ordersResult.exceptionOrNull()?.message,
                )
            }
        }
    }

    /**
     * 加载 tongji 统计（用户列表 + 12项指标）
     */
    fun loadTongji() {
        tongjiJob?.cancel()
        tongjiJob = viewModelScope.launch {
            val s = _uiState.value
            repository.getTongji(
                startTime = s.startTimestamp,
                endTime = s.endTimestamp,
                site = if (s.activeSite == "all") null else s.activeSite,
                user = if (s.activeUser == "#") null else s.activeUser,
            ).onSuccess { tongji ->
                _uiState.update { it.copy(tongji = tongji) }
            }
        }
    }

    // ===== 筛选操作 =====

    fun changeTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab, page = 1) }
        loadData(1, refresh = true)
        loadTongji()
    }

    fun changeSite(site: String) {
        _uiState.update { it.copy(activeSite = site, activeUser = "#", page = 1) }
        loadData(1, refresh = true)
        loadTongji()
    }

    fun changeUser(user: String) {
        _uiState.update { it.copy(activeUser = user, page = 1) }
        loadData(1, refresh = true)
        loadTongji()
    }

    fun changeTimeRange(range: Int) {
        val startTs: Long?
        val endTs: Long?
        val now = System.currentTimeMillis() / 1000

        when (range) {
            FenyongTimeRange.ALL -> { startTs = null; endTs = null }
            FenyongTimeRange.TODAY -> {
                val today = getTodayStart()
                startTs = today
                endTs = now
            }
            FenyongTimeRange.YESTERDAY -> {
                val yesterday = getTodayStart() - 86400
                startTs = yesterday
                endTs = getTodayStart()
            }
            FenyongTimeRange.LAST_7D -> {
                startTs = now - 7 * 86400
                endTs = now
            }
            FenyongTimeRange.LAST_MONTH -> {
                startTs = now - 30 * 86400
                endTs = now
            }
            FenyongTimeRange.LAST_MONTH_2 -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.add(java.util.Calendar.MONTH, -1)
                val firstDay = cal.timeInMillis / 1000
                cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                val lastDay = cal.timeInMillis / 1000
                startTs = firstDay
                endTs = lastDay + 86400 - 1
            }
            FenyongTimeRange.LAST_YEAR -> {
                startTs = now - 365 * 86400
                endTs = now
            }
            FenyongTimeRange.LAST_YEAR_2 -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.YEAR, cal.get(java.util.Calendar.YEAR) - 1)
                cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                val lastYearStart = cal.timeInMillis / 1000
                cal.add(java.util.Calendar.YEAR, 1)
                cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                val thisYearStart = cal.timeInMillis / 1000
                startTs = lastYearStart
                endTs = thisYearStart - 1
            }
            FenyongTimeRange.CUSTOM -> { startTs = null; endTs = null } // TODO: 自定义日期选择器
            else -> { startTs = null; endTs = null }
        }

        _uiState.update {
            it.copy(
                timeRange = range,
                startTimestamp = startTs,
                endTimestamp = endTs,
                page = 1,
            )
        }
        loadData(1, refresh = true)
        loadTongji()
    }

    fun loadMore() {
        val s = _uiState.value
        if (s.hasMore) {
            loadData(s.page + 1)
        }
    }

    fun refreshAll() {
        loadData(1, refresh = true)
        loadTongji()
    }

    private fun getTodayStart(): Long {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Shanghai"))
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 1000
    }

    /**
     * Tab 列表
     */
    val tabOptions = listOf(
        FenyongTab("tab1", "正常", "tab1"),
        FenyongTab("tab3", "已结算", "tab3"),
        FenyongTab("tab4", "未结算", "tab4"),
        FenyongTab("tab5", "无佣金", "tab5"),
    )
}
