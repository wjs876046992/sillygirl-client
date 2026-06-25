package com.sillygirl.client.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.repository.AuthRepository
import com.sillygirl.client.data.repository.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
)

class LoginViewModel(
    private val serverConfig: ServerConfig,
) : ViewModel() {
    private val repository = AuthRepository()
    private val _uiState = MutableStateFlow(
        LoginUiState(
            serverUrl = serverConfig.getDefaultServer()?.url ?: "",
            username = serverConfig.getDefaultServer()?.username ?: "",
            password = serverConfig.getDefaultServer()?.password ?: "",
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, error = null)
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "请填写服务器地址、用户名和密码")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.login(state.serverUrl, state.username, state.password)
            result.fold(
                onSuccess = {
                    // 保存 token 到 SharedPreferences，确保下次启动可自动登录
                    RetrofitClient.token?.let { token ->
                        serverConfig.saveToken(token)
                    }
                    // 如果是从服务器列表选择的，保存登录凭证
                    val existingServers = serverConfig.getServers()
                    val matchingIndex = existingServers.indexOfFirst { it.url == state.serverUrl }
                    if (matchingIndex >= 0) {
                        serverConfig.updateServer(matchingIndex, existingServers[matchingIndex].copy(
                            username = state.username,
                            password = state.password,
                        ))
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "登录失败"
                    )
                }
            )
        }
    }
}

/** ViewModel Factory */
class LoginViewModelFactory(
    private val serverConfig: ServerConfig,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return LoginViewModel(serverConfig) as T
    }
}
