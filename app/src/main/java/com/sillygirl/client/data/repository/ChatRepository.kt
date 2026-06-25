package com.sillygirl.client.data.repository

import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.ChatSelectsData
import com.sillygirl.client.data.model.SendMessageRequest
import com.sillygirl.client.data.model.SendMessageData

class ChatRepository {

    suspend fun getChatSelects(platform: String? = null, botId: String? = null): Result<ChatSelectsData> {
        return try {
            val response = RetrofitClient.api.getChatSelects(platform, botId)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("获取聊天选项失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        platform: String,
        botId: String,
        userId: String,
        chatId: String,
        content: String,
    ): Result<SendMessageData> {
        return try {
            val response = RetrofitClient.api.sendMessage(
                SendMessageRequest(
                    platform = platform,
                    botId = botId,
                    userId = userId,
                    chatId = chatId,
                    content = content,
                )
            )
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.errorMessage ?: "发送失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
