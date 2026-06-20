package com.sillygirl.client.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.repository.AuthRepository
import com.sillygirl.client.data.repository.MasterRepository
import com.sillygirl.client.data.repository.TaskRepository
import com.sillygirl.client.data.repository.FenyongRepository
import com.sillygirl.client.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

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
                val userDeferred = async { authRepo.getCurrentUserInfo() }
                val mastersDeferred = async { masterRepo.getMasters() }
                val tasksDeferred = async { taskRepo.getTasks() }
                val fenyongDeferred = async { fenyongRepo.getStats(init = true) }

                val userResult = userDeferred.await()
                userResult.onSuccess { user ->
                    val plugins = user.plugins.size
                    val masters = mastersDeferred.await().getOrDefault(emptyList()).size
                    val tasks = tasksDeferred.await().getOrDefault(emptyList()).count { it.enable }
                    val fenyongStats = fenyongDeferred.await().getOrNull()?.tongji

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userName = user.name.ifBlank { "管理员" },
                        avatar = user.avatar,
                        installedPlugins = plugins,
                        masterCount = masters,
                        activeTaskCount = tasks,
                        fenyongStats = fenyongStats,
                    )
                }
                userResult.onFailure { e ->
                    val masters = mastersDeferred.await().getOrDefault(emptyList()).size
                    val tasks = tasksDeferred.await().getOrDefault(emptyList()).count { it.enable }
                    val fenyongStats = fenyongDeferred.await().getOrNull()?.tongji
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userName = "管理员",
                        masterCount = masters,
                        activeTaskCount = tasks,
                        fenyongStats = fenyongStats,
                        error = e.message ?: "加载失败",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载异常",
                )
            }
        }
    }
}
