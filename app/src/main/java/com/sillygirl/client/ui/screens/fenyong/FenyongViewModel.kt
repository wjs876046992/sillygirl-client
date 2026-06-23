package com.sillygirl.client.ui.screens.fenyong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.model.FenyongOrder
import com.sillygirl.client.data.repository.FenyongRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FenyongUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val keyword: String = "",
    val orders: List<FenyongOrder> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val filterActualGtZero: Boolean = false,
)

class FenyongViewModel : ViewModel() {
    private val repository = FenyongRepository()
    private val _uiState = MutableStateFlow(FenyongUiState())
    val uiState: StateFlow<FenyongUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadOrders(1)
    }

    /**
     * 加载订单列表（带搜索和分页）
     */
    fun loadOrders(page: Int = 1) {
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
                    val orders = result.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        orders = orders,
                        page = result.page,
                        total = result.total,
                        error = null,
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载订单失败",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载订单失败",
                )
            }
        }
    }

    fun setKeyword(keyword: String) {
        _uiState.value = _uiState.value.copy(keyword = keyword)
    }

    fun loadData() {
        loadOrders(1)
    }

    fun toggleFilterActualGtZero() {
        _uiState.value = _uiState.value.copy(
            filterActualGtZero = !_uiState.value.filterActualGtZero
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
