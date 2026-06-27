package com.sillygirl.client.ui.screens.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.PluginLogEntry
import com.sillygirl.client.data.model.PluginLogStats
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
)

class PluginLogsViewModel : ViewModel() {
    private companion object {
        const val TAG = "PluginLogsVM"
    }

    private val _uiState = MutableStateFlow(PluginLogsUiState())
    val uiState: StateFlow<PluginLogsUiState> = _uiState.asStateFlow()

    private var currentUuid: String = ""

    /**
     * 加载插件日志
     * @param uuid 插件 UUID
     * @param level 日志级别过滤
     * @param since 起始时间戳（秒）
     */
    fun loadLogs(uuid: String, level: String? = null, since: Long? = null) {
        currentUuid = uuid
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = RetrofitClient.api.getPluginLogs(
                    uuid = uuid,
                    level = level,
                    since = since,
                    limit = 200,
                )

                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        logs = response.data,
                        selectedLevel = level,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "获取日志失败",
                    )
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
     * 加载日志统计
     */
    fun loadStats(uuid: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getPluginLogStats(uuid)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(stats = response.data)
                }
            } catch (_: Exception) {
                // 忽略统计加载错误
            }
        }
    }

    /**
     * 切换日志级别过滤
     */
    fun setLevel(level: String?) {
        _uiState.value = _uiState.value.copy(selectedLevel = level)
        loadLogs(currentUuid, level)
    }

    /**
     * 设置搜索关键词（客户端过滤）
     */
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    /**
     * 刷新日志
     */
    fun refresh() {
        loadLogs(currentUuid, _uiState.value.selectedLevel)
        loadStats(currentUuid)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}