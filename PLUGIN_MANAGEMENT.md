# 插件管理功能设计文档

> Created: 2026-06-23

## 1. 功能概述

插件管理功能允许用户管理 SillyGirl 后端的插件，包括查看插件列表、编辑插件内容、重载插件、切换调试模式、配置插件表单等。

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
- 配置插件表单（如果插件有 @form 注解）
- 重载插件

**UI 元素**：
- MiniAppBar（标题：插件名称，操作：重载、编辑/预览切换）
- PluginInfoCard（插件信息 + debug 开关）
- PluginFormCard（表单配置，条件显示）
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

### 3.2 PluginDetail

```kotlin
data class PluginDetail(
    val uuid: String = "",
    val content: String = "",        // 插件源代码
    val form: List<PluginFormField> = emptyList(),  // 表单配置
    val debug: Boolean = false,
    val disable: Boolean = false,
)
```

### 3.3 PluginFormField

```kotlin
data class PluginFormField(
    val key: String = "",            // 字段 key
    val label: String = "",          // 显示标签
    val type: String = "text",       // 类型：text, number, switch, select
    val value: Any? = null,          // 当前值
    val options: List<PluginFormOption> = emptyList(),  // 选项（select 类型）
)
```

## 4. API 接口

### 4.1 获取插件详情

**请求**：
```
GET /api/plugins/detail?uuid={uuid}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "uuid": "710fe387_77c3_5a2c_8f95_dbf2fdd5f237",
    "content": "// plugin source code...",
    "form": [
      {
        "key": "qqbot.appId",
        "label": "App ID",
        "type": "text",
        "value": "1903850706"
      },
      {
        "key": "qqbot.appSecret",
        "label": "App Secret",
        "type": "text",
        "value": ""
      }
    ],
    "debug": false,
    "disable": false
  }
}
```

### 4.2 编辑插件内容

**请求**：
```
PUT /api/plugins/content?uuid={uuid}
Content-Type: application/json

{
  "content": "// updated plugin source code..."
}
```

**响应**：
```json
{
  "success": true
}
```

### 4.3 重载插件

**请求**：
```
POST /api/plugins/reload
Content-Type: application/json

{
  "uuid": "710fe387_77c3_5a2c_8f95_dbf2fdd5f237"
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
POST /api/plugins/debug
Content-Type: application/json

{
  "uuid": "710fe387_77c3_5a2c_8f95_dbf2fdd5f237",
  "debug": true
}
```

**响应**：
```json
{
  "success": true
}
```

### 4.5 保存插件表单配置

**请求**：
```
PUT /api/storage?uuid={pluginId}
Content-Type: application/json

{
  "qqbot.appId": "1903850706",
  "qqbot.appSecret": "new_secret",
  "plugin_debug.710fe387_77c3_5a2c_8f95_dbf2fdd5f237": true,
  "plugin_disable.710fe387_77c3_5a2c_8f95_dbf2fdd5f237": false
}
```

**响应**：
```json
{
  "success": true
}
```

**说明**：
- 表单字段的 key 格式为 `{pluginName}.{fieldKey}`
- debug 模式的 key 为 `plugin_debug.{pluginId}`
- disable 模式的 key 为 `plugin_disable.{pluginId}`
- 保存后会自动触发插件重载

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
    val detail: PluginDetail? = null,
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
  │                       ├── 配置表单
  │                       └── 重载插件
  │
  └── 其他功能卡片...
```

### 5.3 关键代码片段

**插件卡片**：
```kotlin
@Composable
private fun MyPluginCard(plugin: PluginRoute, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // 图标
            Box(modifier = Modifier.size(40.dp).shadow(4.dp, RoundedCornerShape(10.dp))) {
                Icon(Icons.Filled.Extension, null, tint = ...)
            }
            Spacer(Modifier.width(12.dp))
            // 信息
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plugin.title.ifBlank { plugin.name })
                    if (plugin.debug) Text("调试")
                    if (plugin.hasForm) Text("表单")
                }
                Text(plugin.description)
                Text("${plugin.version} · ${plugin.author}")
            }
            Icon(Icons.Filled.ChevronRight, null)
        }
    }
}
```

**Debug 开关**：
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    Text("调试模式", style = MaterialTheme.typography.bodyMedium)
    Switch(checked = debug, onCheckedChange = { onToggleDebug() })
}
```

**表单字段**：
```kotlin
when (field.type) {
    "switch" -> {
        Row(modifier = Modifier.fillMaxWidth(), ...) {
            Text(field.label)
            Switch(checked = formData[field.key] as? Boolean ?: false, ...)
        }
    }
    "number" -> {
        OutlinedTextField(
            value = formData[field.key]?.toString() ?: "",
            onValueChange = { formData[field.key] = it },
            label = { Text(field.label) },
        )
    }
    else -> { // text
        OutlinedTextField(
            value = formData[field.key]?.toString() ?: "",
            onValueChange = { formData[field.key] = it },
            label = { Text(field.label) },
        )
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

### 6.2 插件表单配置

插件表单配置存储在 bucket 中，key 格式为 `{pluginName}.{fieldKey}`：

```
qqbot.appId = "1903850706"
qqbot.appSecret = "secret"
qqbot.guild_enabled = "true"
```

### 6.3 Debug/Disable 状态

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
