package com.sillygirl.client.ui.screens.fenyong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.model.FenyongStatData
import com.sillygirl.client.data.model.FenyongOrder
import com.sillygirl.client.data.model.FenyongTab
import com.sillygirl.client.data.repository.FenyongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FenyongUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val stats: FenyongStatData? = null,
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
            val result = repository.getStats(init = true, activeKey = activeKey)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        stats = response.tongji,
                        orders = response.data,
                        tabs = response.tabs,
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
}
