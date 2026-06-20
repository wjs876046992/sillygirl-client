package com.sillygirl.client.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.repository.AuthRepository
import com.sillygirl.client.data.repository.PluginRepository
import com.sillygirl.client.data.repository.MasterRepository
import com.sillygirl.client.data.repository.TaskRepository
import com.sillygirl.client.data.repository.FenyongRepository
import com.sillygirl.client.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userName: String = "",
    val avatar: String = "",
    val installedPlugins: Int = 0,
    val masterCount: Int = 0,
    val activeTaskCount: Int = 0,
    val fenyongStats: FenyongStatData? = null,
)

class DashboardViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val pluginRepo = PluginRepository()
    private val masterRepo = MasterRepository()
    private val taskRepo = TaskRepository()
    private val fenyongRepo = FenyongRepository()
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                var plugins = 0
                var masters = 0
                var tasks = 0
                var fenyongStats: FenyongStatData? = null

                pluginRepo.getInstalledPlugins().onSuccess { plugins = it.size }
                masterRepo.getMasters().onSuccess { masters = it.size }
                taskRepo.getTasks().onSuccess { tasks = it.count { t -> t.enable } }
                fenyongRepo.getStats(init = true).onSuccess { fenyongStats = it.tongji }

                val userResult = authRepo.getCurrentUserInfo()
                userResult.fold(
                    onSuccess = { user ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            userName = user.name.ifBlank { "管理员" },
                            avatar = user.avatar,
                            installedPlugins = plugins,
                            masterCount = masters,
                            activeTaskCount = tasks,
                            fenyongStats = fenyongStats,
                        )
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            userName = "管理员",
                            installedPlugins = plugins,
                            masterCount = masters,
                            activeTaskCount = tasks,
                            fenyongStats = fenyongStats,
                            error = "用户信息加载失败",
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载异常",
                )
            }
        }
    }
}
