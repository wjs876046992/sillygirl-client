package com.sillygirl.client.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.sillygirl.client.data.api.RetrofitClient

/**
 * ServerConfig 管理傻妞服务器配置，持久化到 SharedPreferences
 * 支持多服务器列表、默认服务器选择
 */
class ServerConfig(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "sillygirl_servers"
        private const val KEY_SERVERS = "servers"
        private const val KEY_DEFAULT_INDEX = "default_index"
        private const val KEY_TOKEN = "token"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class ServerInfo(
        val url: String,
        val username: String = "",
        val password: String = "",
        val alias: String = "",
        val requiresAuth: Boolean = true, // 是否需要认证（true=需要账号密码，false=无需认证）
    ) {
        val displayName: String = alias.ifBlank { url }
    }

    /** 获取所有已保存的服务器列表 */
    fun getServers(): List<ServerInfo> {
        val json = prefs.getString(KEY_SERVERS, null) ?: return emptyList()
        return try {
            parseServers(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveServers(servers: List<ServerInfo>) {
        val entries = servers.map { s ->
            encodeField(s.url) + "|" +
            encodeField(s.username) + "|" +
            encodeField(s.password) + "|" +
            encodeField(s.alias) + "|" +
            (if (s.requiresAuth) "1" else "0")
        }
        prefs.edit().putString(KEY_SERVERS, entries.joinToString(";;")).apply()
    }

    /** 添加新服务器 */
    fun addServer(server: ServerInfo) {
        val servers = getServers()
        if (servers.any { it.url == server.url }) {
            throw IllegalArgumentException("服务器地址已存在")
        }
        val newServers = servers + server
        saveServers(newServers)
        prefs.edit().putInt(KEY_DEFAULT_INDEX, newServers.size - 1).apply()
    }

    /** 更新服务器 */
    fun updateServer(index: Int, server: ServerInfo) {
        val servers = getServers().toMutableList()
        if (index !in servers.indices) throw IndexOutOfBoundsException()
        servers[index] = server
        saveServers(servers)
    }

    /** 删除服务器 */
    fun removeServer(index: Int) {
        val servers = getServers().toMutableList()
        if (index !in servers.indices) throw IndexOutOfBoundsException()
        servers.removeAt(index)
        saveServers(servers)
        val defaultIdx = prefs.getInt(KEY_DEFAULT_INDEX, 0)
        if (servers.isEmpty()) {
            prefs.edit().remove(KEY_DEFAULT_INDEX).apply()
        } else if (defaultIdx >= servers.size) {
            prefs.edit().putInt(KEY_DEFAULT_INDEX, servers.size - 1).apply()
        }
    }

    /** 获取默认服务器索引 */
    fun getDefaultIndex(): Int {
        return prefs.getInt(KEY_DEFAULT_INDEX, -1)
    }

    /** 设置默认服务器 */
    fun setDefaultIndex(index: Int) {
        prefs.edit().putInt(KEY_DEFAULT_INDEX, index).apply()
    }

    /** 获取当前默认服务器 */
    fun getDefaultServer(): ServerInfo? {
        val index = getDefaultIndex()
        val servers = getServers()
        return if (index in servers.indices) servers[index] else null
    }

    /** 保存 token */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
        RetrofitClient.token = token
    }

    /** 获取保存的 token */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    /** 清除 token（登出） */
    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
        RetrofitClient.token = null
    }

    private fun parseServers(json: String): List<ServerInfo> {
        return json.split(";;").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 4) {
                ServerInfo(
                    url = decodeField(parts[0]),
                    username = decodeField(parts[1]),
                    password = decodeField(parts[2]),
                    alias = decodeField(parts[3]),
                    requiresAuth = if (parts.size >= 5) parts[4] == "1" else true,
                )
            } else null
        }
    }

    private fun encodeField(s: String): String =
        s.replace("|", "||").replace(";", ";;")

    private fun decodeField(s: String): String =
        s.replace(";;", ";").replace("||", "|")
}
