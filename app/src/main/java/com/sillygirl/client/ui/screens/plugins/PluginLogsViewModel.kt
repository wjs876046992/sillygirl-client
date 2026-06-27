package com.sillygirl.client.ui.screens.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.PluginLogEntry
import com.sillygirl.client.data.model.PluginLogStats
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PluginLogsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val logs: List<PluginLogEntry> = emptyList(),
    val stats: PluginLogStats? = null,
    val selectedLevel: String? = null, // null=全部, info/debug/warn/error/log
    val searchQuery: String = "",
    val snackbarMessage: String? = null,
    // 分页状态
    val currentPage: Int = 1,
    val total: Int = 0,
    val pageSize: Int = 20,
) {
    val totalPages: Int get() = if (total > 0) (total + pageSize - 1) / pageSize else 1
    val hasNextPage: Boolean get() = currentPage < totalPages
    val hasPrevPage: Boolean get() = currentPage > 1
}

class PluginLogsViewModel : ViewModel() {
    private companion object {
        const val TAG = "PluginLogsVM"
    }

    private val _uiState = MutableStateFlow(PluginLogsUiState())
    val uiState: StateFlow<PluginLogsUiState> = _uiState.asStateFlow()

    private var currentUuid: String = ""

    /**
     * 加载插件日志和统计（并发请求，一次更新状态，避免竞态覆盖）
     * @param uuid 插件 UUID
     * @param level 日志级别过滤
     * @param since 起始时间戳（秒）
     * @param page 页码（从1开始）
     */
    fun loadLogs(uuid: String, level: String? = null, since: Long? = null, page: Int = 1) {
        currentUuid = uuid
        val pageSize = _uiState.value.pageSize
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                coroutineScope {
                    val logsDeferred = async {
                        RetrofitClient.api.getPluginLogs(
                            uuid = uuid,
                            level = level,
                            since = since,
                            page = page,
                            pageSize = pageSize,
                        )
                    }
                    val statsDeferred = async {
                        try {
                            RetrofitClient.api.getPluginLogStats(uuid)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    val logsResponse = logsDeferred.await()
                    val statsResponse = statsDeferred.await()

                    if (logsResponse.success) {
                        val logs = logsResponse.data ?: emptyList()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            logs = logs,
                            selectedLevel = level,
                            currentPage = logsResponse.page,
                            total = logsResponse.total,
                            pageSize = logsResponse.pageSize,
                            stats = statsResponse?.takeIf { it.success }?.data
                                ?: _uiState.value.stats,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "获取日志失败",
                            stats = statsResponse?.takeIf { it.success }?.data
                                ?: _uiState.value.stats,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败",
                )
            }
        }
    }

    /**
     * 跳转到指定页
     */
    fun loadPage(page: Int) {
        val state = _uiState.value
        if (page < 1 || page > state.totalPages) return
        loadLogs(currentUuid, state.selectedLevel, page = page)
    }

    /**
     * 切换日志级别过滤（回到第1页）
     */
    fun setLevel(level: String?) {
        loadLogs(currentUuid, level, page = 1)
    }

    /**
     * 设置搜索关键词（客户端过滤）
     */
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    /**
     * 刷新日志（回到第1页）
     */
    fun refresh() {
        loadLogs(currentUuid, _uiState.value.selectedLevel, page = 1)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}