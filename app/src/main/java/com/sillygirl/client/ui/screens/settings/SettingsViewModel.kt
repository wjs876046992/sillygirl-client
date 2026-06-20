package com.sillygirl.client.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.repository.AuthRepository
import com.sillygirl.client.data.repository.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val serverUrl: String = "",
    val isLoggingOut: Boolean = false,
)

class SettingsViewModel(
    private val serverConfig: ServerConfig,
) : ViewModel() {
    private val authRepo = AuthRepository()
    private val _uiState = MutableStateFlow(SettingsUiState(
        serverUrl = serverConfig.getDefaultServer()?.url ?: "未设置",
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun logout(onDone: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoggingOut = true)
        try {
            kotlinx.coroutines.runBlocking {
                RetrofitClient.api.logout()
            }
        } catch (_: Exception) { }
        RetrofitClient.reset()
        serverConfig.clearToken()
        _uiState.value = _uiState.value.copy(isLoggingOut = false)
        onDone()
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
