# 插件管理功能设计文档

> Created: 2026-06-23
> Updated: 2026-06-23 (simplified to use existing storage API)

## 1. 功能概述

插件管理功能允许用户管理 SillyGirl 后端的插件，包括查看插件列表、编辑插件内容、重载插件、切换调试模式等。

## 2. 页面结构

### 2.1 MyPluginsScreen（我的插件）

**入口**：DashboardScreen 点击插件数量卡片

**数据来源**：`currentUser.plugins`（PluginRoute 列表）

**功能**：
- 显示用户已安装的插件列表
- 点击插件进入插件详情页

**UI 元素**：
- MiniAppBar（标题：我的插件）
- LazyColumn（插件卡片列表）
- 空状态提示

### 2.2 PluginDetailScreen（插件详情）

**入口**：MyPluginsScreen 点击插件卡片

**功能**：
- 显示插件信息（标题、描述、版本、作者、分类）
- 切换调试模式（debug）
- 编辑插件内容（代码编辑器）
- 重载插件

**UI 元素**：
- MiniAppBar（标题：插件名称，操作：重载、编辑/预览切换）
- PluginInfoCard（插件信息 + debug 开关）
- PluginEditorCard（代码编辑器，条件显示）

## 3. 数据模型

### 3.1 PluginRoute（扩展）

```kotlin
data class PluginRoute(
    val path: String = "",           // 插件路径（UUID）
    val name: String = "",           // 插件名称
    val component: String = "",      // 组件路径
    @SerializedName("create_at") val createAt: String? = null,
    val title: String = "",          // 显示标题
    val description: String = "",    // 描述
    val icon: String = "",           // 图标
    val version: String = "v1.0.0",  // 版本
    val author: String = "",         // 作者
    val running: Boolean = false,    // 是否运行中
    val disable: Boolean = false,    // 是否禁用
    val debug: Boolean = false,      // 是否调试模式
    @SerializedName("has_form") val hasForm: Boolean = false,  // 是否有表单
    val classes: List<String> = emptyList(),  // 分类标签
)
```

## 4. API 接口（使用现有 storage API）

### 4.1 获取插件内容

**请求**：
```
GET /api/storage?keys=plugins.{uuid}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "plugins.9e594935_8d3a_57a7_bb87_c296ee343bd3": "// plugin source code..."
  }
}
```

### 4.2 编辑插件内容

**请求**：
```
PUT /api/storage?uuid={pluginId}
Content-Type: application/json

{
  "plugins.{uuid}": "// updated plugin source code..."
}
```

**响应**：
```json
{
  "success": true
}
```

**说明**：保存后会自动触发插件重载。

### 4.3 重载插件

**请求**：
```
PUT /api/storage?uuid={pluginId}
Content-Type: application/json

{
  "plugins.{uuid}": "reload"
}
```

**响应**：
```json
{
  "success": true
}
```

### 4.4 切换调试模式

**请求**：
```
PUT /api/storage?uuid={pluginId}
Content-Type: application/json

{
  "plugin_debug.{uuid}": "true"
}
```

**响应**：
```json
{
  "success": true
}
```

**说明**：
- debug 模式的 key 为 `plugin_debug.{uuid}`
- disable 模式的 key 为 `plugin_disable.{uuid}`

## 5. 实现细节

### 5.1 ViewModel 状态管理

```kotlin
data class MyPluginsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val plugins: List<PluginRoute> = emptyList(),
    val snackbarMessage: String? = null,
)

data class PluginDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val content: String = "",        // 插件内容
    val isSaving: Boolean = false,
    val snackbarMessage: String? = null,
)
```

### 5.2 导航流程

```
DashboardScreen
  │
  ├── 点击插件数量卡片
  │     │
  │     └── MyPluginsScreen
  │           │
  │           └── 点击插件卡片
  │                 │
  │                 └── PluginDetailScreen
  │                       │
  │                       ├── 编辑代码
  │                       ├── 切换调试模式
  │                       └── 重载插件
  │
  └── 其他功能卡片...
```

### 5.3 关键代码片段

**获取插件内容**：
```kotlin
suspend fun getPluginContent(uuid: String): Result<String> {
    return try {
        val keys = "plugins.$uuid"
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
```

**保存插件内容**：
```kotlin
suspend fun updatePluginContent(uuid: String, content: String): Result<Unit> {
    return try {
        val body = mapOf("plugins.$uuid" to content)
        val response = RetrofitClient.api.saveStorage(uuid, body)
        if (response.success) Result.success(Unit)
        else Result.failure(Exception("保存插件内容失败"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**重载插件**：
```kotlin
suspend fun reloadPlugin(uuid: String): Result<Unit> {
    return try {
        val body = mapOf("plugins.$uuid" to "reload")
        val response = RetrofitClient.api.saveStorage(uuid, body)
        if (response.success) Result.success(Unit)
        else Result.failure(Exception("重载插件失败"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**切换调试模式**：
```kotlin
suspend fun togglePluginDebug(uuid: String, debug: Boolean): Result<Unit> {
    return try {
        val body = mapOf("plugin_debug.$uuid" to debug.toString())
        val response = RetrofitClient.api.saveStorage(uuid, body)
        if (response.success) Result.success(Unit)
        else Result.failure(Exception("切换调试模式失败"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

## 6. 后端实现参考

### 6.1 插件头部注释格式

```javascript
/**
 * @title 我的插件
 * @description 这是一个示例插件
 * @author Herman
 * @version v1.0.0
 * @form
 * @rule 测试
 */
```

### 6.2 Debug/Disable 状态

Debug 和 Disable 状态存储在 bucket 中，key 格式为：

```
plugin_debug.{pluginId} = true/false
plugin_disable.{pluginId} = true/false
```

## 7. 待实现功能

- [ ] 插件表单的 select 类型支持
- [ ] 插件表单的 validation 验证
- [ ] 插件代码语法高亮
- [ ] 插件运行日志查看
- [ ] 插件卸载功能
- [ ] 插件分享功能

## 8. 注意事项

1. **插件路径**：插件的唯一标识是 `path` 字段（UUID 格式）
2. **表单保存**：保存表单后会自动触发插件重载
3. **代码编辑**：代码编辑器使用简单的 OutlinedTextField，后续可以替换为代码编辑器组件
4. **错误处理**：所有 API 调用都有错误处理和 snackbar 提示
5. **状态管理**：使用 StateFlow + collectAsStateWithLifecycle 管理状态
