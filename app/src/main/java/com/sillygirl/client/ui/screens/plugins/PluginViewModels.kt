package com.sillygirl.client.ui.screens.plugins

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.PluginInfo
import com.sillygirl.client.data.model.PluginFormField
import com.sillygirl.client.data.model.PluginRoute
import com.sillygirl.client.data.repository.PluginRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyPluginsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val plugins: List<PluginRoute> = emptyList(),
    val snackbarMessage: String? = null,
)

data class PluginDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val content: String = "",
    val isSaving: Boolean = false,
    val snackbarMessage: String? = null,
    val pluginDetail: PluginRoute? = null,
    val formFields: List<PluginFormField> = emptyList(),
)

class MyPluginsViewModel : ViewModel() {
    private val repo = PluginRepository()
    private val _uiState = MutableStateFlow(MyPluginsUiState())
    val uiState: StateFlow<MyPluginsUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(PluginDetailUiState())
    val detailState: StateFlow<PluginDetailUiState> = _detailState.asStateFlow()

    fun loadPlugins(plugins: List<PluginRoute>) {
        _uiState.value = MyPluginsUiState(isLoading = false, plugins = plugins)
    }

    fun loadPluginContent(uuid: String) {
        viewModelScope.launch {
            _detailState.value = PluginDetailUiState(isLoading = true)
            repo.getPluginContent(uuid).fold(
                onSuccess = { content ->
                    _detailState.value = PluginDetailUiState(isLoading = false, content = content)
                },
                onFailure = { e ->
                    _detailState.value = PluginDetailUiState(isLoading = false, error = e.message)
                },
            )
        }
    }

    fun updatePluginContent(uuid: String, content: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isSaving = true)
            repo.updatePluginContent(uuid, content).fold(
                onSuccess = {
                    _detailState.value = _detailState.value.copy(isSaving = false, snackbarMessage = "保存成功")
                },
                onFailure = {
                    _detailState.value = _detailState.value.copy(isSaving = false, snackbarMessage = "保存失败：${it.message}")
                },
            )
        }
    }

    fun reloadPlugin(uuid: String) {
        viewModelScope.launch {
            repo.reloadPlugin(uuid).fold(
                onSuccess = { _detailState.value = _detailState.value.copy(snackbarMessage = "重载成功") },
                onFailure = { _detailState.value = _detailState.value.copy(snackbarMessage = "重载失败：${it.message}") },
            )
        }
    }

    fun toggleDebug(uuid: String, debug: Boolean) {
        viewModelScope.launch {
            repo.togglePluginDebug(uuid, debug).fold(
                onSuccess = {
                    _detailState.value = _detailState.value.copy(
                        pluginDetail = _detailState.value.pluginDetail?.copy(debug = debug),
                        snackbarMessage = if (debug) "已开启调试模式" else "已关闭调试模式"
                    )
                },
                onFailure = {
                    _detailState.value = _detailState.value.copy(snackbarMessage = "操作失败：${it.message}")
                },
            )
        }
    }

    fun toggleDisable(uuid: String, disable: Boolean) {
        viewModelScope.launch {
            repo.togglePluginDisable(uuid, disable).fold(
                onSuccess = {
                    _detailState.value = _detailState.value.copy(
                        pluginDetail = _detailState.value.pluginDetail?.copy(disable = disable),
                        snackbarMessage = if (disable) "已禁用插件" else "已启用插件"
                    )
                },
                onFailure = {
                    _detailState.value = _detailState.value.copy(snackbarMessage = "操作失败：${it.message}")
                },
            )
        }
    }

    fun loadPluginDetail(uuid: String) {
        viewModelScope.launch {
            repo.getPluginDetail(uuid).fold(
                onSuccess = { plugin ->
                    _detailState.value = _detailState.value.copy(
                        pluginDetail = plugin,
                        formFields = plugin.formFields
                    )
                },
                onFailure = { e ->
                    // 如果获取详情失败，不影响主流程
                    Log.e("MyPluginsViewModel", "Failed to load plugin detail", e)
                },
            )
        }
    }

    fun savePluginForm(uuid: String, formData: Map<String, Any?>) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isSaving = true)
            repo.savePluginForm(uuid, formData).fold(
                onSuccess = {
                    _detailState.value = _detailState.value.copy(isSaving = false, snackbarMessage = "表单配置已保存")
                },
                onFailure = {
                    _detailState.value = _detailState.value.copy(isSaving = false, snackbarMessage = "保存失败：${it.message}")
                },
            )
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun clearDetailSnackbar() {
        _detailState.value = _detailState.value.copy(snackbarMessage = null)
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
