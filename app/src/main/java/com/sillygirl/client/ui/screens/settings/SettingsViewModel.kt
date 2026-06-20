package com.sillygirl.client.ui.screens.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.sillygirl.client.data.api.ApiConfig
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.repository.AuthRepository

data class SettingsUiState(
    val serverUrl: String = "",
    val isLoggingOut: Boolean = false,
)

class SettingsViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val _uiState = MutableStateFlow(SettingsUiState(
        serverUrl = ApiConfig.serverBaseUrl.ifBlank { "未设置" },
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun logout(onDone: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoggingOut = true)
        authRepo.logout()
        _uiState.value = _uiState.value.copy(isLoggingOut = false)
        onDone()
    }
}
