package com.sillygirl.client.ui.screens.serverlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.sillygirl.client.LocalServerConfig
import com.sillygirl.client.data.repository.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ServerListUiState(
    val servers: List<com.sillygirl.client.data.repository.ServerConfig.ServerInfo> = emptyList(),
    val defaultIndex: Int = -1,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ServerListViewModel(
    private val serverConfig: ServerConfig,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServerListUiState())
    val uiState: StateFlow<ServerListUiState> = _uiState.asStateFlow()

    init {
        loadServers()
    }

    fun loadServers() {
        _uiState.value = ServerListUiState(
            servers = serverConfig.getServers(),
            defaultIndex = serverConfig.getDefaultIndex(),
        )
    }

    fun addServer(info: com.sillygirl.client.data.repository.ServerConfig.ServerInfo) {
        try {
            serverConfig.addServer(info)
            loadServers()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }

    fun removeServer(index: Int) {
        try {
            serverConfig.removeServer(index)
            loadServers()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }

    fun setDefault(index: Int) {
        try {
            serverConfig.setDefaultIndex(index)
            loadServers()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }
}

/** Factory that injects ServerConfig from CompositionLocal */
class ServerListViewModelFactory(
    private val serverConfig: ServerConfig,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return ServerListViewModel(serverConfig) as T
    }
}
