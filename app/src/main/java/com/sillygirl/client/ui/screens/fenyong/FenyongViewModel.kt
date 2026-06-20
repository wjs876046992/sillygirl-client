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
    val tabs: List<FenyongTab> = emptyList(),
    val activeTab: String = "all",
)

class FenyongViewModel : ViewModel() {
    private val repository = FenyongRepository()
    private val _uiState = MutableStateFlow(FenyongUiState())
    val uiState: StateFlow<FenyongUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(activeKey: String = "all") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, activeTab = activeKey)

            val dashboardDeferred = async { repository.getDashboard() }
            val ordersDeferred = async {
                repository.getOrders(
                    tab = if (activeKey == "all") null else activeKey,
                )
            }

            dashboardDeferred.await().fold(
                onSuccess = { dashboard ->
                    ordersDeferred.await().fold(
                        onSuccess = { response ->
                            val tabs = buildTabs(response.total)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                dashboard = dashboard,
                                orders = response.data,
                                tabs = tabs,
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

    fun switchTab(tabKey: String) {
        loadData(activeKey = tabKey)
    }

    private fun buildTabs(totalOrders: Int): List<FenyongTab> {
        return listOf(
            FenyongTab("all", "全部", totalOrders.toString()),
            FenyongTab("tab1", "待结算", ""),
            FenyongTab("tab3", "未绑定", ""),
            FenyongTab("tab4", "未到账", ""),
            FenyongTab("tab5", "已到账", ""),
            FenyongTab("tab6", "已过期", ""),
        )
    }
}
