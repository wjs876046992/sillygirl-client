package com.sillygirl.client.ui.screens.fenyong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.model.*
import com.sillygirl.client.data.repository.FenyongRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FenyongUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val dashboard: FenyongDashboardResponse? = null,
    val orders: List<FenyongOrder> = emptyList(),
)

class FenyongViewModel : ViewModel() {
    private val repository = FenyongRepository()
    private val _uiState = MutableStateFlow(FenyongUiState())
    val uiState: StateFlow<FenyongUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val dashboardDeferred = async { repository.getDashboard() }
            val ordersDeferred = async { repository.getOrders() }

            dashboardDeferred.await().fold(
                onSuccess = { dashboard ->
                    ordersDeferred.await().fold(
                        onSuccess = { response ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                dashboard = dashboard,
                                orders = response.data,
                            )
                        },
                        onFailure = { e ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = e.message ?: "加载失败",
                                dashboard = dashboard,
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
        }
    }
}
