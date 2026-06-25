package com.sillygirl.client.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.repository.AuthRepository
import com.sillygirl.client.data.repository.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val isLoggingOut: Boolean = false,
    val logoutDone: Boolean = false,
    val isLoading: Boolean = false,
    val configData: Map<String, String> = emptyMap(),
    val editingConfig: MutableMap<String, String> = mutableMapOf(),
    val snackbarMessage: String? = null,
    val isUpgrading: Boolean = false,
    val isRestarting: Boolean = false,
)

class SettingsViewModel(
    private val serverConfig: ServerConfig,
) : ViewModel() {
    private val authRepo = AuthRepository()
    private val _uiState = MutableStateFlow(SettingsUiState(
        serverUrl = serverConfig.getDefaultServer()?.url ?: "未设置",
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadBasicConfig()
    }

    private fun loadBasicConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val keys = listOf(
                    "app.name",
                    "app.port",
                    "app.password",
                    "app.machine_id",
                    "app.started_at",
                    "app.compiled_at",
                    "app.version",
                    "app.storage",
                    "app.redis_addr",
                    "app.redis_password",
                ).joinToString(",")

                val response = RetrofitClient.api.getStorage(keys)
                if (response.success && response.data != null) {
                    val data = response.data
                    val configMap = when (data) {
                        is Map<*, *> -> data.mapKeys { it.key.toString() }.mapValues { it.value?.toString() ?: "" }
                        else -> emptyMap()
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        configData = configMap,
                        editingConfig = configMap.toMutableMap(),
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        snackbarMessage = "加载配置失败: ${response.errorMessage ?: "未知错误"}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "加载配置失败: ${e.message}"
                )
            }
        }
    }

    fun updateConfigValue(key: String, value: String) {
        _uiState.value = _uiState.value.copy(
            editingConfig = _uiState.value.editingConfig.toMutableMap().apply {
                put(key, value)
            }
        )
    }

    fun saveConfig(key: String) {
        viewModelScope.launch {
            val value = _uiState.value.editingConfig[key] ?: return@launch
            try {
                val body = mapOf(key to value)
                val response = RetrofitClient.api.saveStorage(null, body)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        configData = _uiState.value.configData.toMutableMap().apply {
                            put(key, value)
                        },
                        snackbarMessage = "保存成功"
                    )
                    // 如果修改了端口，显示提示
                    if (key == "app.port") {
                        _uiState.value = _uiState.value.copy(
                            snackbarMessage = "端口修改成功，即将跳转页面！"
                        )
                    }
                } else {
                    val errorMsg = response.errorMessage ?: "保存失败"
                    if (errorMsg != "unchanged") {
                        _uiState.value = _uiState.value.copy(
                            snackbarMessage = "保存失败: $errorMsg"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    fun upgrade() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpgrading = true, snackbarMessage = "升级中...")
            try {
                val randomValue = (1..10).map { ('a'..'z') + ('0'..'9') }.flatten().random().toString()
                val body = mapOf("app.compiled_at" to randomValue)
                val response = RetrofitClient.api.saveStorage(null, body)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "升级指令已发送，请等待重启完成"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "升级失败: ${response.errorMessage ?: "未知错误"}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "升级失败: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isUpgrading = false)
            }
        }
    }

    fun restart() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestarting = true, snackbarMessage = "重启中...")
            try {
                val randomValue = (1..10).map { ('a'..'z') + ('0'..'9') }.flatten().random().toString()
                val body = mapOf("app.started_at" to randomValue)
                val response = RetrofitClient.api.saveStorage(null, body)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "重启指令已发送，请等待重启完成"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "重启失败: ${response.errorMessage ?: "未知错误"}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "重启失败: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isRestarting = false)
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun logout(onDone: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoggingOut = true, logoutDone = false)
        viewModelScope.launch {
            try {
                RetrofitClient.api.logout()
            } catch (_: Exception) { }
            RetrofitClient.reset()
            serverConfig.clearToken()
            _uiState.value = _uiState.value.copy(isLoggingOut = false, logoutDone = true)
        }
    }
}

class SettingsViewModelFactory(
    private val serverConfig: ServerConfig,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(serverConfig) as T
    }
}
