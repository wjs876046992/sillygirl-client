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
            try {
                _uiState.update { it.copy(isLoading = true) }
                repository.getChatSelects(platform, botId).fold(
                    onSuccess = { data ->
                        // Gson 反序列化时 null 会绕过 Kotlin 默认值，需要防御性处理
                        val users = data.userNames ?: emptyList()
                        val groups = data.groupNames ?: emptyList()
                        val platforms = data.platforms ?: emptyMap()

                        // 调试日志
                        android.util.Log.d("ChatViewModel", "=== Chat Selects Loaded ===")
                        android.util.Log.d("ChatViewModel", "Platform filter: $platform")
                        android.util.Log.d("ChatViewModel", "User names count: ${users.size}")
                        android.util.Log.d("ChatViewModel", "Group names count: ${groups.size}")

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                selects = data.copy(userNames = users, groupNames = groups, platforms = platforms),
                                filteredUsers = users,
                                filteredGroups = groups,
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
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "loadSelects exception", e)
                _uiState.update {
                    it.copy(isLoading = false, snackbarMessage = "加载异常: ${e.message}")
                }
            }
        }
    }

    fun selectPlatform(platform: String) {
        try {
            // 先重置选中状态，再请求新数据
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
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "selectPlatform failed: $platform", e)
            _uiState.update {
                it.copy(
                    selectedPlatform = platform,
                    isLoading = false,
                    snackbarMessage = "切换平台失败: ${e.message}",
                )
            }
        }
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

        // 获取选中用户的信息
        val selectedUser = state.filteredUsers.find { it.value == state.selectedUserId }
        val isGroupFriend = selectedUser?.src == "group"
        val sourceChats = selectedUser?.src_chats ?: emptyList()

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, lastResult = null) }
            repository.sendMessage(
                platform = platform,
                botId = state.selectedBot ?: "",
                userId = if (!state.isGroup) (state.selectedUserId ?: "") else "",
                chatId = if (state.isGroup) {
                    state.selectedChatId ?: ""
                } else if (isGroupFriend && sourceChats.isNotEmpty()) {
                    // 群好友：使用第一个来源群ID发送临时会话
                    // TODO: 如果有多个群，可以让用户选择
                    sourceChats.first()
                } else {
                    ""
                },
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
