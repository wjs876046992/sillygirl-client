package com.sillygirl.client.ui.screens.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.PluginInfo
import com.sillygirl.client.data.repository.PluginRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyPluginsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val plugins: List<PluginInfo> = emptyList(),
    val snackbarMessage: String? = null,
)

class MyPluginsViewModel : ViewModel() {
    private val repo = PluginRepository()
    private val _uiState = MutableStateFlow(MyPluginsUiState())
    val uiState: StateFlow<MyPluginsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MyPluginsUiState(isLoading = true)
            repo.getInstalledPlugins().fold(
                onSuccess = { _uiState.value = MyPluginsUiState(isLoading = false, plugins = it) },
                onFailure = { _uiState.value = MyPluginsUiState(isLoading = false, error = it.message) },
            )
        }
    }

    fun togglePlugin(plugin: PluginInfo) {
        viewModelScope.launch {
            try {
                val body = mapOf("name" to plugin.id)
                if (plugin.running) {
                    RetrofitClient.api.stopPlugin(body)
                } else {
                    RetrofitClient.api.runPlugin(body)
                }
                load()
                _uiState.value = _uiState.value.copy(snackbarMessage = if (plugin.running) "已停止" else "已启动")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "操作失败：${e.message}")
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}

data class PluginMarketUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val plugins: List<PluginInfo> = emptyList(),
    val snackbarMessage: String? = null,
)

class PluginMarketViewModel : ViewModel() {
    private val repo = PluginRepository()
    private val _uiState = MutableStateFlow(PluginMarketUiState())
    val uiState: StateFlow<PluginMarketUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = PluginMarketUiState(isLoading = true)
            repo.getAvailablePlugins().fold(
                onSuccess = { _uiState.value = PluginMarketUiState(isLoading = false, plugins = it) },
                onFailure = { _uiState.value = PluginMarketUiState(isLoading = false, error = it.message) },
            )
        }
    }

    fun installPlugin(id: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.installPlugin(mapOf("name" to id))
                load()
                _uiState.value = _uiState.value.copy(snackbarMessage = "安装成功")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "安装失败：${e.message}")
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
