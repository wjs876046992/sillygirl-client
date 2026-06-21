package com.sillygirl.client.ui.screens.fenyong

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.model.FenyongDashboardResponse
import com.sillygirl.client.data.model.FenyongOrder
import com.sillygirl.client.data.model.FenyongOrderResponse
import com.sillygirl.client.data.repository.FenyongRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val TAG = "FenyongViewModel"

/**
 * 分佣页面 UI 状态
 */
data class FenyongUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val keyword: String = "",
    val dashboard: FenyongDashboardResponse? = null,
    val orders: List<FenyongOrder> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
)

class FenyongViewModel : ViewModel() {
    private val repository = FenyongRepository()
    private val _uiState = MutableStateFlow(FenyongUiState())
    val uiState: StateFlow<FenyongUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var dashboardJob: Job? = null

    init {
        Log.d(TAG, "ViewModel init, token=${com.sillygirl.client.data.api.RetrofitClient.token?.take(8)}...")
        loadData()
        loadDashboard()
    }

    /**
     * 加载订单列表（带搜索和分页）
     */
    fun loadOrders(page: Int = 1) {
        Log.d(TAG, "loadOrders: page=$page, keyword='${_uiState.value.keyword}'")
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val keyword = _uiState.value.keyword

            try {
                val response = repository.getOrders(
                    keyword = if (keyword.isNotBlank()) keyword else null,
                    page = page,
                    pageSize = 20,
                )

                response.onSuccess { result ->
                    Log.d(TAG, "Orders success: total=${result.total}, page=${result.page}, count=${result.data.size}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        orders = result.data,
                        page = result.page,
                        total = result.total,
                        error = null,
                    )
                }.onFailure { e ->
                    Log.e(TAG, "Orders failed: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载订单失败",
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Orders exception", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载订单失败",
                )
            }
        }
    }

    /**
     * 加载 dashboard 数据
     */
    fun loadDashboard() {
        Log.d(TAG, "loadDashboard called")
        dashboardJob?.cancel()
        dashboardJob = viewModelScope.launch {
            try {
                val response = repository.getDashboard()
                response.onSuccess { dashboard ->
                    Log.d(TAG, "Dashboard loaded: success=${dashboard.success}, today=${dashboard.today}")
                    _uiState.value = _uiState.value.copy(
                        dashboard = dashboard,
                    )
                }.onFailure { e ->
                    Log.e(TAG, "Dashboard failed: ${e.message}", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Dashboard exception", e)
            }
        }
    }

    fun setKeyword(keyword: String) {
        _uiState.value = _uiState.value.copy(keyword = keyword)
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(keyword = "")
        loadOrders(1)
    }

    fun loadData() {
        Log.d(TAG, "loadData called")
        loadDashboard()
        loadOrders(1)
    }
}
