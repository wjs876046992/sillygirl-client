package com.sillygirl.client.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.model.ChatSelectsData
import com.sillygirl.client.data.model.NameLabel
import com.sillygirl.client.data.model.SendMessageData
import com.sillygirl.client.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val isLoading: Boolean = true,
    val selects: ChatSelectsData? = null,
    val selectedPlatform: String? = null,
    val selectedBot: String? = null,
    val selectedUserId: String? = null,
    val selectedChatId: String? = null,
    val isGroup: Boolean = false,
    val message: String = "",
    val isSending: Boolean = false,
    val lastResult: SendMessageData? = null,
    val snackbarMessage: String? = null,
    val filteredUsers: List<NameLabel> = emptyList(),
    val filteredGroups: List<NameLabel> = emptyList(),
)

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadSelects()
    }

    /**
     * 加载聊天选项
     * @param platform 平台筛选（可选）
     * @param botId 机器人筛选（可选）
     */
    private fun loadSelects(platform: String? = null, botId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getChatSelects(platform, botId).fold(
                onSuccess = { data ->
                    // 调试日志
                    android.util.Log.d("ChatViewModel", "=== Chat Selects Loaded ===")
                    android.util.Log.d("ChatViewModel", "Platform filter: $platform")
                    android.util.Log.d("ChatViewModel", "User names count: ${data.userNames.size}")
                    android.util.Log.d("ChatViewModel", "Group names count: ${data.groupNames.size}")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selects = data,
                            filteredUsers = data.userNames,  // 后端已过滤，直接使用
                            filteredGroups = data.groupNames,  // 后端已过滤，直接使用
                        )
                    }
                },
                onFailure = { e ->
                    android.util.Log.e("ChatViewModel", "Failed to load selects", e)
                    _uiState.update {
                        it.copy(isLoading = false, snackbarMessage = "加载失败: ${e.message}")
                    }
                }
            )
        }
    }

    fun selectPlatform(platform: String) {
        _uiState.update {
            it.copy(
                selectedPlatform = platform,
                selectedBot = null,
                selectedUserId = null,
                selectedChatId = null,
            )
        }
        // 重新请求 API，按平台过滤
        loadSelects(platform = platform)
    }

    fun selectBot(botId: String) {
        _uiState.update { it.copy(selectedBot = botId) }
    }

    fun selectUserId(userId: String) {
        _uiState.update { it.copy(selectedUserId = userId.ifBlank { null }) }
    }

    fun selectChatId(chatId: String) {
        _uiState.update { it.copy(selectedChatId = chatId.ifBlank { null }) }
    }

    fun setTargetType(isGroup: Boolean) {
        _uiState.update {
            it.copy(
                isGroup = isGroup,
                selectedUserId = null,
                selectedChatId = null,
            )
        }
    }

    fun updateMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val platform = state.selectedPlatform ?: return
        val content = state.message.trim()
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, lastResult = null) }
            repository.sendMessage(
                platform = platform,
                botId = state.selectedBot ?: "",
                userId = if (!state.isGroup) (state.selectedUserId ?: "") else "",
                chatId = if (state.isGroup) (state.selectedChatId ?: "") else "",
                content = content,
            ).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            lastResult = result,
                            message = "",
                            snackbarMessage = "发送成功",
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            snackbarMessage = "发送失败: ${e.message}",
                        )
                    }
                }
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
