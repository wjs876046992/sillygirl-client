package com.sillygirl.client.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val fenyongDashboard: FenyongDashboardResponse? = null,
)

class DashboardViewModel : ViewModel() {
    private val masterRepo = MasterRepository()
    private val taskRepo = TaskRepository()
    private val fenyongRepo = FenyongRepository()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = !forceRefresh, error = null)
            try {
                var masters = 0
                var tasks = 0
                var fenyongDashboard: FenyongDashboardResponse? = null

                try { masters = masterRepo.getMasters().getOrThrow().size } catch (_: Exception) {}
                try { tasks = taskRepo.getTasks().getOrThrow().count { it.enable } } catch (_: Exception) {}
                try { fenyongDashboard = fenyongRepo.getDashboard().getOrThrow() } catch (_: Exception) {}

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    masterCount = masters,
                    activeTaskCount = tasks,
                    fenyongDashboard = fenyongDashboard,
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
