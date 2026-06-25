package com.sillygirl.client.data.repository

import android.util.Log
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.*

class PluginRepository {

    private companion object {
        const val TAG = "PluginRepository"
    }

    /** 统一将 path（如 /script/uuid）转为纯 UUID */
    private fun String.asPluginId(): String = removePrefix("/script/")

    /**
     * 获取已安装插件列表（分页）
     * @param page 页码（从1开始）
     * @param pageSize 每页数量
     * @param classFilter 分类筛选（单选，null=全部）
     * @param origins 来源筛选（多选，null=全部）
     * @param keyword 搜索关键词
     */
    suspend fun getInstalledPlugins(
        page: Int = 1,
        pageSize: Int = 20,
        classFilter: String? = null,
        origins: List<String>? = null,
        keyword: String? = null,
    ): Result<PluginListResult> {
        return try {
            val response = RetrofitClient.api.getPluginList(
                current = page,
                pageSize = pageSize,
                activeKey = "tab1",
                classFilter = classFilter,
                origins = origins,
                keyword = keyword,
            )
            if (response.success) {
                Result.success(PluginListResult(
                    plugins = response.data,
                    total = response.total,
                    page = response.page,
                    pageSize = pageSize,
                    classes = response.classes,
                    origins = response.origins,
                ))
            } else {
                Result.failure(Exception("获取插件列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取可用插件列表（分页）
     * @param page 页码（从1开始）
     * @param pageSize 每页数量
     * @param activeKey tab 标识（tab1=已安装, tab2=未安装, tab3=可升级）
     * @param keyword 搜索关键词
     */
    suspend fun getAvailablePlugins(
        page: Int = 1,
        pageSize: Int = 20,
        activeKey: String = "tab2",
        keyword: String? = null,
    ): Result<PluginListResult> {
        return try {
            val response = RetrofitClient.api.getPluginList(
                current = page,
                pageSize = pageSize,
                activeKey = activeKey,
                keyword = keyword,
            )
            if (response.success) {
                Result.success(PluginListResult(
                    plugins = response.data,
                    total = response.total,
                    page = response.page,
                    pageSize = pageSize,
                    tab1 = response.tab1,
                    tab2 = response.tab2,
                    tab3 = response.tab3,
                    classes = response.classes,
                    origins = response.origins,
                ))
            } else {
                Result.failure(Exception("获取可用插件失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPluginContent(uuid: String): Result<String> {
        return try {
            val pluginId = uuid.asPluginId()
            val keys = "plugins.$pluginId"
            val response = RetrofitClient.api.getStorage(keys)
            if (response.success) {
                val data = response.data as? Map<*, *>
                val content = data?.get(keys) as? String ?: ""
                Result.success(content)
            } else {
                Result.failure(Exception("获取插件内容失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePluginContent(uuid: String, content: String): Result<Unit> {
        return try {
            val pluginId = uuid.asPluginId()
            val body = mapOf("plugins.$pluginId" to content)
            val response = RetrofitClient.api.saveStorage(pluginId, body)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("保存插件内容失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadPlugin(uuid: String): Result<Unit> {
        return try {
            val pluginId = uuid.asPluginId()
            val body = mapOf("plugins.$pluginId" to "reload")
            val response = RetrofitClient.api.saveStorage(pluginId, body)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("重载插件失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePluginDebug(uuid: String, debug: Boolean): Result<Unit> {
        return try {
            val pluginId = uuid.asPluginId()
            val body = mapOf("plugin_debug.$pluginId" to debug.toString())
            val response = RetrofitClient.api.saveStorage(pluginId, body)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("切换调试模式失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePluginDisable(uuid: String, disable: Boolean): Result<Unit> {
        return try {
            val pluginId = uuid.asPluginId()
            val body = mapOf("plugin_disable.$pluginId" to disable.toString())
            val response = RetrofitClient.api.saveStorage(pluginId, body)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("切换禁用模式失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPluginDetail(uuid: String): Result<PluginRoute> {
        return try {
            val pluginId = uuid.asPluginId()
            val response = RetrofitClient.api.getPluginList(activeKey = "tab1")
            if (response.success) {
                val plugin = response.data.find { it.id == pluginId }
                if (plugin != null) {
                    Result.success(PluginRoute(
                        path = "/script/${plugin.id}",
                        name = plugin.id,
                        title = plugin.title,
                        description = plugin.description,
                        icon = plugin.icon,
                        version = plugin.version,
                        author = plugin.author,
                        running = plugin.running,
                        disable = plugin.disable,
                        debug = plugin.debug,
                        hasForm = plugin.hasForm,
                        classes = plugin.classes,
                    ))
                } else {
                    Result.failure(Exception("插件不存在"))
                }
            } else {
                Result.failure(Exception("获取插件列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uninstallPlugin(uuid: String): Result<Unit> {
        return try {
            val pluginId = uuid.asPluginId()
            // 卸载时不传 uuid 参数，避免后端触发 deferred reload 导致插件被重新创建
            // 与 web 端行为一致：web 端卸载也不传 uuid 参数
            val body = mapOf("plugins.$pluginId" to "uninstall")
            val response = RetrofitClient.api.saveStorage(body = body)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("卸载插件失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePluginForm(uuid: String, formData: Map<String, Any?>): Result<Unit> {
        return try {
            val pluginId = uuid.asPluginId()
            val body = formData.mapValues { it.value?.toString() ?: "" }
            val response = RetrofitClient.api.saveStorage(pluginId, body)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception("保存表单配置失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从插件代码中解析 @form 注释，提取表单字段定义
     * 格式: @form {key: "xxx", title: "xxx", tooltip: "xxx", valueType: "text", required: true}
     */
    fun parseFormFromCode(code: String): List<PluginFormField> {
        if (code.isBlank()) return emptyList()

        return try {
            val fields = mutableListOf<PluginFormField>()
            // 匹配 @form {key: "xxx", title: "xxx", ...} 格式
            val regex = """@\s*form\s*\{([^}]+)\}""".toRegex()
            val matches = regex.findAll(code)

            for (match in matches) {
                val content = match.groupValues[1]
                try {
                    val field = parseFormField(content)
                    if (field != null) {
                        fields.add(field)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse @form: $content", e)
                }
            }
            fields
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse form from code", e)
            emptyList()
        }
    }

    /**
     * 解析单个表单字段的属性
     */
    private fun parseFormField(content: String): PluginFormField? {
        // 提取 key-value 对
        val props = mutableMapOf<String, String>()
        // 匹配 key: "value" 或 key: 'value' 或 key: value 格式
        val propRegex = """(\w+)\s*:\s*(?:"([^"]*)"|'([^']*)'|(\w+))""".toRegex()
        val propMatches = propRegex.findAll(content)

        for (match in propMatches) {
            val key = match.groupValues[1]
            val value = match.groupValues[2].ifEmpty { match.groupValues[3].ifEmpty { match.groupValues[4] } }
            props[key] = value
        }

        val key = props["key"] ?: return null
        val title = props["title"] ?: key
        val tooltip = props["tooltip"] ?: ""
        val valueType = props["valueType"] ?: props["type"] ?: "text"
        val required = props["required"]?.toBooleanStrictOrNull() ?: false
        val defaultValue = props["default"] ?: ""

        // 解析 options（用于 select 类型）
        val options = mutableListOf<PluginFormOption>()
        val optionsStr = props["options"]
        if (optionsStr != null) {
            // 格式: "option1,option2" 或 "option1:value1,option2:value2"
            optionsStr.split(",").forEach { option ->
                val parts = option.trim().split(":")
                if (parts.size == 2) {
                    options.add(PluginFormOption(label = parts[0].trim(), value = parts[1].trim()))
                } else {
                    val label = parts[0].trim()
                    options.add(PluginFormOption(label = label, value = label))
                }
            }
        }

        return PluginFormField(
            key = key,
            label = title,
            type = mapValueType(valueType),
            value = defaultValue,
            tooltip = tooltip,
            required = required,
            options = options,
        )
    }

    /**
     * 映射表单字段类型
     */
    private fun mapValueType(valueType: String): String {
        return when (valueType.lowercase()) {
            "switch", "boolean", "bool" -> "switch"
            "number", "int", "integer", "float", "double" -> "number"
            "select", "dropdown", "choice" -> "select"
            "textarea", "text", "string" -> "text"
            else -> "text"
        }
    }

    /**
     * 获取插件表单字段的值
     * 通过 storage API 使用实际的表单字段 key 获取值
     * 例如 keys=onebot.platform,onebot.token
     */
    suspend fun getPluginFormValuesByKeys(fieldKeys: List<String>): Result<Map<String, Any?>> {
        if (fieldKeys.isEmpty()) return Result.success(emptyMap())
        return try {
            val keysParam = fieldKeys.joinToString(",")
            val response = RetrofitClient.api.getStorage(keysParam)
            if (response.success) {
                val data = response.data as? Map<*, *> ?: emptyMap<String, Any?>()
                val result = mutableMapOf<String, Any?>()
                fieldKeys.forEach { key ->
                    val value = data[key]
                    result[key] = when {
                        value is Boolean -> value
                        value is Number -> value
                        value is String -> {
                            // 尝试解析字符串值
                            when {
                                value.equals("true", ignoreCase = true) -> true
                                value.equals("false", ignoreCase = true) -> false
                                value.toDoubleOrNull() != null -> value.toDouble()
                                else -> value
                            }
                        }
                        else -> value
                    }
                }
                Log.d(TAG, "Loaded form values by keys: $result")
                Result.success(result)
            } else {
                Log.w(TAG, "Failed to get storage for keys: $keysParam")
                Result.success(emptyMap())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get form values by keys", e)
            Result.success(emptyMap())
        }
    }

    /**
     * 保存插件表单配置值
     * 直接使用实际的表单字段 key 保存到 storage
     */
    suspend fun savePluginFormValues(uuid: String, values: Map<String, Any?>): Result<Unit> {
        return try {
            val pluginId = uuid.asPluginId()
            // 将值转为字符串 Map
            val body = values.mapValues { it.value?.toString() ?: "" }
            val response = RetrofitClient.api.saveStorage(pluginId, body)
            if (response.success) {
                Log.d(TAG, "Saved form values for $pluginId: $values")
                Result.success(Unit)
            } else {
                Result.failure(Exception("保存表单配置失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save form values", e)
            Result.failure(e)
        }
    }
}
