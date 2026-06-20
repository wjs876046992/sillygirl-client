package com.sillygirl.client.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.repository.AuthRepository
import com.sillygirl.client.data.repository.PluginRepository
import com.sillygirl.client.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userName: String = "",
    val installedPlugins: Int = 0,
    val taskCount: Int = 0,
)

class DashboardViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val pluginRepo = PluginRepository()
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val userResult = authRepo.getCurrentUserInfo()
                val pluginsResult = pluginRepo.getInstalledPlugins()

                userResult.fold(
                    onSuccess = { user ->
                        pluginsResult.fold(
                            onSuccess = { plugins ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    userName = user.name.ifBlank { "管理员" },
                                    installedPlugins = plugins.size,
                                )
                            },
                            onFailure = { e ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    userName = user.name.ifBlank { "管理员" },
                                    error = "获取插件信息失败",
                                )
                            }
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "加载失败",
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
