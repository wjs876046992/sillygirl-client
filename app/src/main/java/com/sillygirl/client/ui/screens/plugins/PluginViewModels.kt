package com.sillygirl.client.ui.screens.plugins

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.PluginInfo
import com.sillygirl.client.data.model.PluginFormField
import com.sillygirl.client.data.model.PluginRequest
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
    val isToggling: Boolean = false,
    val snackbarMessage: String? = null,
    val pluginDetail: PluginRoute? = null,
    val formFields: List<PluginFormField> = emptyList(),
    val formValues: Map<String, Any?> = emptyMap(),
)

class MyPluginsViewModel : ViewModel() {
    private companion object {
        const val TAG = "MyPluginsViewModel"
    }

    private val repo = PluginRepository()
    private val _uiState = MutableStateFlow(MyPluginsUiState())
    val uiState: StateFlow<MyPluginsUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(PluginDetailUiState())
    val detailState: StateFlow<PluginDetailUiState> = _detailState.asStateFlow()

    fun loadPlugins(plugins: List<PluginRoute>, showRefreshHint: Boolean = false) {
        _uiState.value = MyPluginsUiState(isLoading = false, plugins = plugins, snackbarMessage = if (showRefreshHint) "已刷新" else null)
    }

    fun loadPluginContent(uuid: String) {
        viewModelScope.launch {
            _detailState.value = PluginDetailUiState(isLoading = true)

            try {
                // 并行加载插件内容和表单值
                val contentDeferred = async { repo.getPluginContent(uuid) }
                val formValuesDeferred = async { repo.getPluginFormValues(uuid) }

                val contentResult = contentDeferred.await()
                val formValuesResult = formValuesDeferred.await()

                contentResult.fold(
                    onSuccess = { content ->
                        // 从插件代码中解析表单字段
                        val formFields = try {
                            repo.parseFormFromCode(content)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse form fields", e)
                            emptyList()
                        }

                        val formValues = formValuesResult.getOrNull() ?: emptyMap()

                        // 将存储的值合并到表单字段中
                        val mergedFields = formFields.map { field ->
                            val storedValue = formValues[field.key]
                            field.copy(value = storedValue ?: field.value)
                        }

                        Log.d(TAG, "Parsed ${formFields.size} form fields from code")
                        Log.d(TAG, "Loaded ${formValues.size} form values from storage")

                        _detailState.value = PluginDetailUiState(
                            isLoading = false,
                            content = content,
                            formFields = mergedFields,
                            formValues = formValues,
                        )
                    },
                    onFailure = { e ->
                        _detailState.value = PluginDetailUiState(isLoading = false, error = e.message)
                    },
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load plugin content", e)
                _detailState.value = PluginDetailUiState(isLoading = false, error = e.message ?: "加载失败")
            }
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
            _detailState.value = _detailState.value.copy(isToggling = true)
            repo.togglePluginDebug(uuid, debug).fold(
                onSuccess = {
                    _detailState.value = _detailState.value.copy(
                        isToggling = false,
                        pluginDetail = _detailState.value.pluginDetail?.copy(debug = debug),
                        snackbarMessage = if (debug) "已开启调试模式" else "已关闭调试模式"
                    )
                },
                onFailure = {
                    _detailState.value = _detailState.value.copy(
                        isToggling = false,
                        snackbarMessage = "操作失败：${it.message}"
                    )
                },
            )
        }
    }

    fun toggleDisable(uuid: String, disable: Boolean) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isToggling = true)
            repo.togglePluginDisable(uuid, disable).fold(
                onSuccess = {
                    _detailState.value = _detailState.value.copy(
                        isToggling = false,
                        pluginDetail = _detailState.value.pluginDetail?.copy(disable = disable),
                        snackbarMessage = if (disable) "已禁用插件" else "已启用插件"
                    )
                },
                onFailure = {
                    _detailState.value = _detailState.value.copy(
                        isToggling = false,
                        snackbarMessage = "操作失败：${it.message}"
                    )
                },
            )
        }
    }

    fun uninstallPlugin(uuid: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isSaving = true)
            repo.uninstallPlugin(uuid).fold(
                onSuccess = {
                    _detailState.value = _detailState.value.copy(isSaving = false, snackbarMessage = "插件已卸载")
                    onSuccess()
                },
                onFailure = {
                    _detailState.value = _detailState.value.copy(isSaving = false, snackbarMessage = "卸载失败：${it.message}")
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
            repo.savePluginFormValues(uuid, formData).fold(
                onSuccess = {
                    _detailState.value = _detailState.value.copy(
                        isSaving = false,
                        formValues = formData,
                        snackbarMessage = "表单配置已保存"
                    )
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

    fun load(showRefreshHint: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.getAvailablePlugins().fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, plugins = it, snackbarMessage = if (showRefreshHint) "已刷新" else null) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) },
            )
        }
    }

    fun installPlugin(id: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.installPlugin(PluginRequest(name = id))
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
