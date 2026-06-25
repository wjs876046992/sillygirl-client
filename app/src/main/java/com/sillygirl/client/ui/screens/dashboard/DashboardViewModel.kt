package com.sillygirl.client.ui.screens.dashboard

import android.util.Log
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
    val snackbarMessage: String? = null,
)

class DashboardViewModel : ViewModel() {
    private val TAG = "DashboardViewModel"
    private val masterRepo = MasterRepository()
    private val taskRepo = TaskRepository()
    private val fenyongRepo = FenyongRepository()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "DashboardViewModel initialized, loading dashboard...")
        loadDashboard()
    }

    fun loadDashboard(forceRefresh: Boolean = false, showRefreshHint: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !forceRefresh,
                error = null,
                snackbarMessage = if (showRefreshHint) "已刷新" else null,
            )
            try {
                var masters = 0
                var tasks = 0
                var fenyongDashboard: FenyongDashboardResponse? = null
                val errors = mutableListOf<String>()

                try {
                    val mastersResult = masterRepo.getMasters()
                    mastersResult.fold(
                        onSuccess = { masters = it.size },
                        onFailure = { e ->
                            Log.e(TAG, "Failed to load masters", e)
                            errors.add("管理员: ${e.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception loading masters", e)
                    errors.add("管理员: ${e.message}")
                }

                try {
                    val tasksResult = taskRepo.getTasks()
                    tasksResult.fold(
                        onSuccess = { tasks = it.count { task -> task.enable } },
                        onFailure = { e ->
                            Log.e(TAG, "Failed to load tasks", e)
                            errors.add("任务: ${e.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception loading tasks", e)
                    errors.add("任务: ${e.message}")
                }

                try {
                    val fenyongResult = fenyongRepo.getDashboard()
                    fenyongResult.fold(
                        onSuccess = { fenyongDashboard = it },
                        onFailure = { e ->
                            Log.e(TAG, "Failed to load fenyong dashboard", e)
                            errors.add("分佣: ${e.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception loading fenyong dashboard", e)
                    errors.add("分佣: ${e.message}")
                }

                Log.d(TAG, "Dashboard loaded: masters=$masters, tasks=$tasks, fenyong=${fenyongDashboard != null}")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    masterCount = masters,
                    activeTaskCount = tasks,
                    fenyongDashboard = fenyongDashboard,
                    error = if (errors.isNotEmpty()) errors.joinToString("\n") else null,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading dashboard", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载异常",
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
