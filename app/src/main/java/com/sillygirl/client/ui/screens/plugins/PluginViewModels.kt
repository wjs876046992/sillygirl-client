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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyPluginsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val plugins: List<PluginRoute> = emptyList(),
    val snackbarMessage: String? = null,
    // 分页相关
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val pageSize: Int = 20,
    val isLoadingMore: Boolean = false,
) {
    val hasNextPage: Boolean get() = currentPage < totalPages
    val hasPrevPage: Boolean get() = currentPage > 1
}

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

    // 存储所有插件（不分页的完整列表）
    private var allPlugins: List<PluginRoute> = emptyList()

    /**
     * 加载已安装插件列表（从 currentUser 获取，客户端分页）
     * @param page 页码（从1开始）
     * @param showRefreshHint 是否显示刷新提示
     */
    fun loadPlugins(page: Int = 1, showRefreshHint: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = RetrofitClient.api.getCurrentUser()
                if (response.success && response.data != null) {
                    allPlugins = response.data.plugins

                    // 客户端分页
                    val pageSize = _uiState.value.pageSize
                    val total = allPlugins.size
                    val totalPages = if (total > 0) (total + pageSize - 1) / pageSize else 1
                    val startIndex = (page - 1) * pageSize
                    val endIndex = minOf(startIndex + pageSize, total)
                    val pagedPlugins = if (startIndex < total) allPlugins.subList(startIndex, endIndex) else emptyList()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        plugins = pagedPlugins,
                        currentPage = page,
                        totalPages = totalPages,
                        total = total,
                        snackbarMessage = if (showRefreshHint) "已刷新" else null,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "获取插件列表失败",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败",
                )
            }
        }
    }

    /**
     * 加载下一页
     */
    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.hasNextPage) {
            loadPlugins(page = currentState.currentPage + 1)
        }
    }

    /**
     * 加载上一页
     */
    fun loadPrevPage() {
        val currentState = _uiState.value
        if (currentState.hasPrevPage) {
            loadPlugins(page = currentState.currentPage - 1)
        }
    }

    /**
     * 跳转到指定页
     */
    fun goToPage(page: Int) {
        if (page in 1.._uiState.value.totalPages) {
            loadPlugins(page = page)
        }
    }

    fun loadPluginContent(uuid: String) {
        viewModelScope.launch {
            _detailState.value = PluginDetailUiState(isLoading = true)

            try {
                val contentResult = repo.getPluginContent(uuid)

                contentResult.fold(
                    onSuccess = { content ->
                        // 从插件代码中解析表单字段
                        val formFields = try {
                            repo.parseFormFromCode(content)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse form fields", e)
                            emptyList()
                        }

                        Log.d(TAG, "Parsed ${formFields.size} form fields from code: ${formFields.map { it.key }}")

                        // 使用实际的表单字段 key 从 storage 获取值
                        val fieldKeys = formFields.map { it.key }
                        val formValues = if (fieldKeys.isNotEmpty()) {
                            repo.getPluginFormValuesByKeys(fieldKeys).getOrNull() ?: emptyMap()
                        } else {
                            emptyMap()
                        }

                        Log.d(TAG, "Loaded ${formValues.size} form values from storage: $formValues")

                        // 将存储的值合并到表单字段中
                        val mergedFields = formFields.map { field ->
                            val storedValue = formValues[field.key]
                            field.copy(value = storedValue ?: field.value)
                        }

                        _detailState.value = _detailState.value.copy(
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

    fun reloadPlugin(uuid: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            repo.reloadPlugin(uuid).fold(
                onSuccess = {
                    _detailState.value = _detailState.value.copy(snackbarMessage = "重载成功")
                    onSuccess?.invoke()
                },
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
                    // 卸载后重新加载 currentUser 和插件列表
                    loadPlugins(page = 1)
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
                    Log.d(TAG, "Plugin detail loaded: running=${plugin.running}, disable=${plugin.disable}, debug=${plugin.debug}, hasForm=${plugin.hasForm}")
                    // 只更新 pluginDetail，不覆盖已解析的 formFields
                    _detailState.value = _detailState.value.copy(
                        pluginDetail = plugin,
                    )
                },
                onFailure = { e ->
                    // 如果获取详情失败，不影响主流程
                    Log.e(TAG, "Failed to load plugin detail: ${e.message}")
                },
            )
        }
    }

    fun savePluginForm(uuid: String, formData: Map<String, Any?>) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isSaving = true)
            repo.savePluginFormValues(uuid, formData).fold(
                onSuccess = {
                    // 保存成功后，重新获取表单值以确保同步
                    val fieldKeys = _detailState.value.formFields.map { it.key }
                    val updatedValues = if (fieldKeys.isNotEmpty()) {
                        repo.getPluginFormValuesByKeys(fieldKeys).getOrNull() ?: formData
                    } else {
                        formData
                    }

                    // 更新表单字段的值
                    val updatedFields = _detailState.value.formFields.map { field ->
                        val newValue = updatedValues[field.key]
                        field.copy(value = newValue ?: field.value)
                    }

                    _detailState.value = _detailState.value.copy(
                        isSaving = false,
                        formValues = updatedValues,
                        formFields = updatedFields,
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
    val installedPlugins: List<PluginRoute> = emptyList(),
    val snackbarMessage: String? = null,
    // Tab
    val activeTab: String = "tab2", // tab1=已安装, tab2=未安装, tab3=可升级
    // 分页相关
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val pageSize: Int = 20,
    val isLoadingMore: Boolean = false,
    // 各 tab 的总数
    val tab1Count: Int = 0,
    val tab2Count: Int = 0,
    val tab3Count: Int = 0,
    // 异常消息弹窗
    val messagesDialogPlugin: PluginInfo? = null,
) {
    val hasNextPage: Boolean get() = currentPage < totalPages
    val hasPrevPage: Boolean get() = currentPage > 1
}

class PluginMarketViewModel : ViewModel() {
    private companion object {
        const val TAG = "PluginMarketVM"
    }

    private val repo = PluginRepository()
    private val _uiState = MutableStateFlow(PluginMarketUiState())
    val uiState: StateFlow<PluginMarketUiState> = _uiState.asStateFlow()

    // 当前搜索关键词
    private var currentKeyword: String? = null

    init {
        load()
        loadInstalledPlugins()
        loadTabCounts()
    }

    /**
     * 加载插件列表（分页）
     * @param page 页码（从1开始）
     * @param isRefresh 是否是刷新操作（true=替换列表，false=追加到列表）
     * @param showRefreshHint 是否显示刷新提示
     * @param activeKey tab 标识（tab1=已安装, tab2=未安装, tab3=可升级）
     * @param keyword 搜索关键词
     */
    fun load(
        page: Int = 1,
        isRefresh: Boolean = true,
        showRefreshHint: Boolean = false,
        activeKey: String = _uiState.value.activeTab,
        keyword: String? = currentKeyword,
    ) {
        currentKeyword = keyword

        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null, activeTab = activeKey)
            } else {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
            }

            repo.getAvailablePlugins(
                page = page,
                pageSize = _uiState.value.pageSize,
                activeKey = activeKey,
                keyword = keyword,
            ).fold(
                onSuccess = { result ->
                    val allPlugins = if (isRefresh) result.plugins else _uiState.value.plugins + result.plugins
                    val isUnfiltered = keyword == null
                    val prev = _uiState.value
                    _uiState.value = prev.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        plugins = allPlugins,
                        currentPage = result.page,
                        totalPages = result.totalPages,
                        total = result.total,
                        snackbarMessage = if (showRefreshHint) "已刷新" else null,
                        tab1Count = if (isUnfiltered) result.tab1 else prev.tab1Count,
                        tab2Count = if (isUnfiltered) result.tab2 else prev.tab2Count,
                        tab3Count = if (isUnfiltered) result.tab3 else prev.tab3Count,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.message ?: "加载失败",
                    )
                },
            )
        }
    }

    /**
     * 切换 Tab（重置搜索）
     */
    fun switchTab(activeKey: String) {
        if (activeKey == _uiState.value.activeTab) return
        currentKeyword = null
        load(page = 1, isRefresh = true, activeKey = activeKey, keyword = null)
    }

    /**
     * 使用关键词重新加载（供 UI 搜索时调用）
     */
    fun loadWithKeyword(keyword: String? = currentKeyword) {
        load(page = 1, isRefresh = true, keyword = keyword)
    }

    /**
     * 加载各 Tab 的总数
     */
    private fun loadTabCounts() {
        viewModelScope.launch {
            // 通过请求 tab2 获取所有 tab 的计数
            repo.getAvailablePlugins(page = 1, pageSize = 1, activeKey = "tab2").fold(
                onSuccess = { result ->
                    _uiState.value = _uiState.value.copy(
                        tab1Count = result.tab1,
                        tab2Count = result.tab2,
                        tab3Count = result.tab3,
                    )
                },
                onFailure = { /* 忽略 */ },
            )
        }
    }

    /**
     * 加载下一页（保持当前筛选条件）
     */
    fun loadNextPage() {
        val currentState = _uiState.value
        if (!currentState.isLoadingMore && currentState.hasNextPage) {
            load(page = currentState.currentPage + 1, isRefresh = false)
        }
    }

    /**
     * 跳转到指定页（保持当前筛选条件）
     */
    fun goToPage(page: Int) {
        if (page in 1.._uiState.value.totalPages) {
            load(page = page, isRefresh = true)
        }
    }

    fun installPlugin(id: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val body = mapOf("plugins.$id" to "install")
                val response = RetrofitClient.api.saveStorage(id, body)
                Log.d(TAG, "install response: success=${response.success}, errors=${response.errorMessage}")
                load(page = _uiState.value.currentPage, showRefreshHint = false)
                loadInstalledPlugins()
                _uiState.value = _uiState.value.copy(snackbarMessage = "安装成功")
                onSuccess?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "install failed", e)
                _uiState.value = _uiState.value.copy(snackbarMessage = "安装失败：${e.message}")
            }
        }
    }

    fun uninstallPlugin(id: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                // 卸载时不传 uuid 参数，避免后端触发 deferred reload 导致插件被重新创建
                val body = mapOf("plugins.$id" to "uninstall")
                val response = RetrofitClient.api.saveStorage(body = body)
                Log.d(TAG, "uninstall response: success=${response.success}, errors=${response.errorMessage}")
                load(page = _uiState.value.currentPage, showRefreshHint = false)
                loadInstalledPlugins()
                _uiState.value = _uiState.value.copy(snackbarMessage = "卸载成功")
                onSuccess?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "uninstall failed", e)
                _uiState.value = _uiState.value.copy(snackbarMessage = "卸载失败：${e.message}")
            }
        }
    }

    fun toggleDebug(id: String, debug: Boolean) {
        viewModelScope.launch {
            try {
                val body = mapOf("plugin_debug.$id" to debug.toString())
                RetrofitClient.api.saveStorage(id, body)
                loadInstalledPlugins()
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = if (debug) "已开启调试" else "已关闭调试"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "操作失败：${e.message}")
            }
        }
    }

    fun toggleDisable(id: String, disable: Boolean) {
        viewModelScope.launch {
            try {
                val body = mapOf("plugin_disable.$id" to disable.toString())
                RetrofitClient.api.saveStorage(id, body)
                loadInstalledPlugins()
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = if (disable) "已禁用插件" else "已启用插件"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "操作失败：${e.message}")
            }
        }
    }

    fun toggleRunning(id: String, running: Boolean) {
        viewModelScope.launch {
            try {
                val api = RetrofitClient.api
                if (running) {
                    api.runPlugin(com.sillygirl.client.data.model.PluginRequest(name = id))
                } else {
                    api.stopPlugin(com.sillygirl.client.data.model.PluginRequest(name = id))
                }
                loadInstalledPlugins()
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = if (running) "已启动插件" else "已停止插件"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "操作失败：${e.message}")
            }
        }
    }

    /**
     * 加载已安装插件列表
     */
    fun loadInstalledPlugins() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getCurrentUser()
                if (response.success && response.data != null) {
                    _uiState.value = _uiState.value.copy(installedPlugins = response.data.plugins)
                }
            } catch (_: Exception) {
                // 忽略错误
            }
        }
    }

    /**
     * 检查插件是否已安装
     */
    fun isInstalled(pluginId: String): Boolean {
        return _uiState.value.installedPlugins.any { it.path.contains(pluginId) }
    }

    /**
     * 获取已安装插件信息
     */
    fun getInstalledPlugin(pluginId: String): PluginRoute? {
        return _uiState.value.installedPlugins.find { it.path.contains(pluginId) }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    /**
     * 显示插件异常消息弹窗
     */
    fun showMessagesDialog(plugin: PluginInfo) {
        _uiState.value = _uiState.value.copy(messagesDialogPlugin = plugin)
    }

    /**
     * 关闭异常消息弹窗
     */
    fun dismissMessagesDialog() {
        _uiState.value = _uiState.value.copy(messagesDialogPlugin = null)
    }

    /**
     * 清空插件异常消息
     */
    fun clearPluginMessages(pluginId: String) {
        viewModelScope.launch {
            try {
                val body = mapOf("plugin_messages.$pluginId" to "")
                RetrofitClient.api.saveStorage(pluginId, body)
                // 清空后刷新列表
                load(page = _uiState.value.currentPage, showRefreshHint = false)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "已清空异常消息",
                    messagesDialogPlugin = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "操作失败：${e.message}"
                )
            }
        }
    }
}
