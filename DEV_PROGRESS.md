# sillygirl-client 开发进展

## 2026-06-24 第三次会话完成的工作

### 一、插件编辑器代码高亮

**问题描述：**
- 插件代码编辑器使用普通 OutlinedTextField，没有语法高亮
- 代码可读性差，不利于编辑和调试

**解决方案：**
实现 JavaScript 语法高亮编辑器，支持以下语法元素着色：
- **关键字** (async, await, function, const, let, var 等) - 紫色
- **字符串** (单引号、双引号、反引号) - 绿色
- **注释** (单行 // 和多行 /* */) - 灰色斜体
- **数字** - 橙色
- **函数名** (后跟括号) - 蓝色
- **内置对象** (console, document, Math 等) - 紫色
- **运算符** - 青色
- **标点符号** - 浅灰色
- **对象属性** (点号后面) - 红色

**技术实现：**
- 创建 `JavaScriptHighlightTransformation` 实现 `VisualTransformation`
- 创建 `SyntaxColors` 类管理语法颜色配置
- 使用 `AnnotatedString` 和 `SpanStyle` 实现文本着色
- 编辑器使用等宽字体 (FontFamily.Monospace)

**修改文件：**
- `PluginScreens.kt` - 新增 `CodeEditor`、`SyntaxColors`、`JavaScriptHighlightTransformation` 组件

### 二、插件列表和详情显示 origin

**问题描述：**
- 插件列表卡片只显示 author，origin 信息不明显
- 用户需要知道插件来源（官方/第三方）

**解决方案：**
在插件列表卡片底部信息行中：
- 如果 author 存在，显示 author
- 如果 origin 存在，显示 origin 标签（使用 tertiaryContainer 背景色）

**修改文件：**
- `PluginScreens.kt` - 修改 `MyPluginCard` 底部信息行，添加 origin 显示

### 三、插件表单功能增强

**问题描述：**
- 原有表单功能需要后端返回 `formFields`
- 无法从插件代码中自动提取表单定义
- 用户看不到已配置的表单值

**解决方案：**

1. **从插件代码解析 @form 注释**
   - 创建 `PluginRepository.parseFormFromCode()` 方法
   - 使用正则表达式匹配 `@form {key: "xxx", title: "xxx", ...}` 格式
   - 支持的属性：`key`, `title`, `tooltip`, `valueType`, `required`, `default`, `options`

2. **表单值存储和读取**
   - 新增 `getPluginFormValues()` 方法，从 `plugin_form.{uuid}` 读取
   - 新增 `savePluginFormValues()` 方法，保存到 `plugin_form.{uuid}`
   - 表单值以 JSON 字符串格式存储

3. **增强表单 UI**
   - 显示必填标记（红色 *）
   - 显示 tooltip 提示信息
   - 显示配置项数量统计
   - 改进的输入框 placeholder

4. **ViewModel 更新**
   - 并行加载插件内容和表单值
   - 从代码解析表单字段并与存储值合并
   - 保存表单时同步更新状态

**@form 注释格式示例：**
```javascript
/**
 * @form {key: "api.token", title: "API Token", tooltip: "请输入API令牌", required: true}
 * @form {key: "api.mode", title: "工作模式", valueType: "select", options: "normal:普通模式,advanced:高级模式"}
 * @form {key: "api.debug", title: "调试模式", valueType: "switch"}
 */
```

**修改文件：**
- `PluginRepository.kt` - 新增表单解析和存储方法
- `PluginViewModels.kt` - 更新加载和保存逻辑
- `PluginScreens.kt` - 增强 PluginFormCard UI
- `Models.kt` - PluginFormField 添加 tooltip/required 字段

---

## 2026-06-24 第二次会话完成的工作

### 一、修复已知问题

#### 1. 修复 `runBlocking` 阻塞主线程问题（高优先级）
- **文件**: `AuthRepository.kt`
- **修改**: 将 `logout()` 方法从同步函数改为 `suspend` 函数，移除了 `runBlocking`
- **影响**: 登出操作不再阻塞主线程，避免应用无响应（ANR）

#### 2. 创建类型安全的请求数据类
- **新增数据类** (在 `Models.kt`):
  - `LoginRequest` - 登录请求
  - `PluginRequest` - 插件操作请求
  - `MasterAddRequest` - 管理员添加请求
  - `MasterDelRequest` - 管理员删除请求
  - `TaskAddRequest` - 任务添加请求
  - `TaskEditRequest` - 任务编辑请求
  - `TaskActionRequest` - 任务操作请求（删除/运行）
  - `TaskSetEnableRequest` - 任务启用/禁用请求

- **更新的API接口** (`SillyGirlApi.kt`):
  - 所有POST接口使用类型安全的数据类替代 `Map<String, String>`
  - `login()` 使用 `LoginRequest`
  - `runPlugin()`, `stopPlugin()`, `installPlugin()`, `uninstallPlugin()` 使用 `PluginRequest`
  - `addMaster()` 使用 `MasterAddRequest`
  - `delMaster()` 使用 `MasterDelRequest`
  - `addTask()` 使用 `TaskAddRequest`
  - `editTask()` 使用 `TaskEditRequest`
  - `delTask()`, `runTask()` 使用 `TaskActionRequest`
  - `setTaskEnable()` 使用 `TaskSetEnableRequest`

- **更新的Repository和ViewModel**:
  - `AuthRepository.kt` - login() 使用 `LoginRequest`
  - `PluginRepository.kt` - uninstallPlugin() 使用 `PluginRequest`
  - `MastersScreen.kt` - addMaster() 和 delMaster() 使用新数据类
  - `TasksScreen.kt` - toggleTask(), deleteTask(), runTask() 使用新数据类
  - `PluginViewModels.kt` - installPlugin() 使用 `PluginRequest`

### 二、优化启动验证流程

#### 问题描述
覆盖安装后，应用默认选择了一个服务器并自动登录，用户无法选择其他服务器。

#### 解决方案
修改 `AppNavGraph.kt` 中的启动验证逻辑：
- **无Token时**: 跳转服务器列表让用户选择（不再自动登录）
- **Token过期时**: 跳转服务器列表让用户选择（不再尝试自动登录）
- **Token有效时**: 直接进入Dashboard（不变）

#### 新的启动流程
```
App启动
  │
  ├── 无服务器 → ServerListScreen（添加服务器）
  │
  ├── 有服务器 + 无Token → ServerListScreen（选择服务器）
  │
  ├── 有服务器 + Token无效 → ServerListScreen（选择服务器）
  │
  └── 有服务器 + Token有效 → Dashboard（自动登录）✅
```

### 三、增强错误处理和日志

1. **DashboardViewModel**: 添加详细日志记录，错误信息会汇总显示在界面上
2. **AuthRepository**: 添加登录、验证会话、获取用户信息的日志
3. **AppNavGraph**: 添加用户信息加载的调试日志

### 四、相关文件修改清单

| 文件 | 修改内容 |
|------|----------|
| `AuthRepository.kt` | logout改为suspend，添加日志 |
| `Models.kt` | 新增8个请求数据类 |
| `SillyGirlApi.kt` | 所有POST接口使用类型安全数据类 |
| `PluginRepository.kt` | uninstallPlugin使用PluginRequest |
| `MastersScreen.kt` | addMaster/delMaster使用新数据类 |
| `TasksScreen.kt` | toggleTask/deleteTask/runTask使用新数据类 |
| `PluginViewModels.kt` | installPlugin使用PluginRequest |
| `DashboardViewModel.kt` | 增强错误处理和日志 |
| `AppNavGraph.kt` | 优化启动验证流程，添加调试日志 |

### 五、技术笔记

1. **`saveStorage` API保持使用 `Map<String, String>`**
   - 这是通用的KV存储API，需要支持动态的键值对
   - 不适合使用固定的数据类

2. **分佣API返回404是正常的**
   - 后端需要配置MongoDB才能使用分佣功能
   - 配置项: `fanli.mongodb`

3. **启动验证流程优化**
   - 移除了自动登录逻辑，让用户主动选择服务器
   - 避免了默认使用某个服务器的问题

---

## 2026-06-24 本次会话完成的工作

### 一、自动登录优化

**问题描述：**
- 原流程需要手动输入用户名密码登录
- 杀掉 App 后 cookie/token 丢失，需要重新登录

**解决方案：**

1. **自动登录流程**
   - 添加服务器时保存用户名密码到 SharedPreferences
   - 选择服务器后自动登录，无需手动输入
   - App 启动时自动用保存的凭证登录

2. **Token 持久化修复**
   - 启动时先调用 `RetrofitClient.setServer()` 恢复服务器地址
   - 再恢复 token，验证会话有效性
   - 如果 token 过期，自动用保存的凭证重新登录

**修改文件：**
- `AppNavGraph.kt` - 添加自动登录路由和启动逻辑
- `LoginScreen.kt` - 添加 AutoLoginScreen 组件
- `ServiceScreen.kt` - 切换服务器时自动登录
- `gradle.properties` - 修复 Java 路径

**新流程：**
```
App 启动
  ↓
读取 SharedPreferences（服务器地址 + token + 用户名密码）
  ↓
RetrofitClient.setServer() → 恢复服务器地址
  ↓
RetrofitClient.token = savedToken → 恢复 token
  ↓
验证 token
  ├─ 有效 → Dashboard（无需重新登录）
  └─ 无效 → 自动用保存的凭证登录 → Dashboard
```

### 二、Bug 修复

1. **启动时 Token 无效问题**
   - 根因：启动时只恢复了 token，没有设置服务器地址
   - 修复：在 `AppNavGraph.kt` 启动逻辑中先调用 `RetrofitClient.setServer(server.url)`

---

## 2026-06-23 本次会话完成的工作

### 一、插件管理优化
- **MyPluginsScreen**：搜索栏（名称/描述/作者模糊搜索）、分类筛选 chips、运行状态绿点、状态标签（已禁用/调试/配置）
- **PluginDetailScreen**：运行状态指示器、toggle loading 转圈、禁用开关红色 track、卸载功能（确认对话框）、origin 标签
- **PluginRepository**：所有方法统一 `asPluginId()` strip `/script/` 前缀，修复"非法操作"bug；新增 `uninstallPlugin()`

### 二、Coil 3 图片加载
- 添加 `coil-compose:3.0.4` + `coil-network-okhttp:3.0.4` 依赖
- 新建 `SillyGirlApp.kt` 实现 `SingletonImageLoader.Factory`（OkHttp + 50MB 磁盘缓存 + JD Referer 拦截器）
- 插件 icon URL 识别（`isIconUrl()`）→ Coil AsyncImage 加载
- 返佣订单图片从自定义 `ImageCache` 迁移到 Coil，删除 ~60 行手写缓存代码

### 三、登录状态与会话管理
- **启动验证**：`LaunchedEffect` 检查 token → `verifySession()` → 有效则跳过登录
- **会话过期**：OkHttp `sessionInterceptor` 拦截 401/403 → 触发 `onSessionExpired` 回调 → 自动跳转登录页
- **DisposableEffect** 注册/注销回调，生命周期安全

### 四、服务管理（ServiceScreen）
- 完整实现：服务器列表、当前服务器标识（绿色"当前"标签）
- 添加服务器对话框（地址/别名/用户名/密码）
- 切换服务器（确认 → setDefaultIndex + setServer + clearToken → 跳转登录）
- 删除服务器（确认对话框）

### 五、MiniAppBar 组件抽取
- 从 9 个文件中移除 private MiniAppBar 定义
- 在 `AppComponents.kt` 中新增 public `MiniAppBar` composable
- 所有 9 个文件改为 import 共享组件

### 六、操作确认与刷新提示
- MastersScreen：删除管理员确认
- TasksScreen：删除/执行任务确认
- StorageScreen：保存键值确认（显示 key + value 预览）
- 刷新 snackbar：仅用户主动点击刷新按钮时显示"已刷新"

### 七、其他修复与优化
- 定时任务列表显示插件 `@icon` 图片（Coil AsyncImage）
- TaskItemCard/MasterCard 布局溢出修复（`Column(weight(1f))`）
- Dashboard FeatureGrid 移除重复的"管理员"和"定时任务"入口
- CI workflow 关闭自动触发，仅保留 `workflow_dispatch`

---

## 2026-06-23 操作确认 + 刷新提示

### 确认对话框
- **MastersScreen**：删除管理员确认
- **TasksScreen**：删除任务确认 + 执行任务确认
- **StorageScreen**：保存键值确认（显示 key 和 value 预览）

### 刷新 Snackbar 提示（仅用户主动点击刷新按钮时）
- **MastersScreen**：刷新后显示"已刷新"
- **TasksScreen**：刷新后显示"已刷新"
- **StorageScreen**：刷新后显示"已刷新"
- **MyPluginsScreen**：刷新后显示"已刷新"
- **PluginMarketScreen**：刷新后显示"已刷新"
- 各ViewModel增加 `showRefreshHint` 参数，默认false，初始加载/操作后刷新不提示

### Bug 修复
- TasksScreen.load() 修复为保留当前列表状态（刷新时不重置为空）
- TaskItemCard 标题 Column `fillMaxWidth()` → `weight(1f)`，修复 Switch 被挤出屏幕

---

## 2026-06-23 返佣订单图片迁移到 Coil

- 删除 FenyongViewModel 中自定义 ImageCache 单例（~60行）
- 删除 preloadImages() 方法及其调用
- OrderItemImage 改用 Coil AsyncImage，加载失败显示平台色占位
- SillyGirlApp OkHttp 拦截器统一添加 JD Referer/User-Agent
- 获得：磁盘缓存、内存管理、crossfade 动画，净减 48 行

---

## 2026-06-23 修复插件内容获取和图标显示

### Bug 修复

1. **插件详情页编辑显示"非法操作，请勿乱动"**
   - 根因：Repository 传给后端的 key 带 `/script/` 前缀（如 `plugins./script/uuid`），后端 `checkFilePlugin` 匹配不到 UUID
   - 修复：所有 Repository 方法统一用 `asPluginId()` strip 前缀

2. **插件列表 icon URL 直接输出为文字**
   - 根因：icon 字段可能是 URL，但只用 `Text()` 显示
   - 修复：检测 URL 时使用 Coil `AsyncImage` 加载图片，其余保持 Text/默认图标

---

## 2026-06-23 优化插件管理（搜索/筛选/状态/卸载/表单修复）

### MyPluginsScreen 优化
- 添加搜索栏（按名称、描述、作者模糊搜索）
- 添加分类筛选 chips（从插件 classes 字段自动收集）
- 插件卡片显示运行状态绿点（仅运行且未禁用时显示）
- 状态标签：已禁用(红色)、调试(紫色)、配置(蓝色)
- 显示分类标签和作者信息
- 空搜索结果时显示"没有匹配的插件"提示
- 顶部显示插件总数和运行中数量

### PluginDetailScreen 优化
- 运行状态指示器（运行中=绿色/已停止=灰色/已禁用=红色）
- toggle debug/disable 添加 loading 转圈状态
- 禁用开关使用红色 track（Material3 DangerColor）
- 添加卸载功能（右上角删除图标 + 确认对话框）
- 调试/禁用开关下方添加辅助说明文字
- 显示 origin 来源标签

### Bug 修复
- PluginFormCard switch 类型标签重复显示（label 显示了两次）
- menuAnchor() deprecation warning 修复（改为 MenuAnchorType.PrimaryNotEditable）

### 技术改动
- PluginRepository 添加 uninstallPlugin()（调用 POST /api/plugins/uninstall）
- PluginDetailUiState 添加 isToggling 字段
- AppNavGraph PluginDetailScreen 添加 onUninstalled 回调
- 卸载后自动刷新用户信息并返回插件列表

---

## 2026-06-23 修复插件管理功能

### 问题描述

用户反馈新版本存在以下问题：
1. 首页分佣模块不见了
2. 插件列表没有显示新加的icon等内容
3. 插件详情页的调试模式、禁用模式没有功能
4. 表单功能缺失

### 问题分析

1. **分佣模块不显示** - 后端MongoDB未配置，导致API返回空数据
2. **插件字段缺失** - `getCurrentUser` API返回的Route结构体缺少debug/disable/hasForm等字段
3. **调试/禁用模式** - 前端UI未正确连接到后端API
4. **表单功能** - 前端缺少表单编辑UI组件

### 修改内容

#### 1. PluginScreens.kt
- 修改`PluginDetailScreen`，从`plugins/list.json` API获取完整插件信息
- 添加禁用模式Switch UI
- 添加`PluginFormCard`组件（支持text/number/switch/select类型）
- 修改`PluginInfoCard`，支持emoji图标显示

#### 2. PluginViewModels.kt
- 添加`toggleDisable()`函数
- 添加`loadPluginDetail()`函数
- 更新`PluginDetailUiState`，添加pluginDetail和formFields字段

#### 3. PluginRepository.kt
- 添加`togglePluginDisable()` API调用
- 添加`getPluginDetail()`函数，从plugins/list.json获取完整插件信息

#### 4. Models.kt
- `PluginRoute`添加`formFields`字段
- `PluginInfo`添加`hasForm`字段

### 编译状态

✅ 编译成功（2026-06-23 17:30）

### 待办事项

- [x] ~~配置测试服务器的MongoDB~~ 已在插件端完成
- [ ] 推送代码到GitHub
- [ ] 构建Release APK
- [ ] 测试插件调试/禁用功能
- [ ] 测试表单配置功能

### 相关文件

- 后端API: `../sillyGirl/core/plugin_subscribe.go`
- 后端分佣: `../sillyGirl/core/fenyong_api.go`
- 测试服务器: `192.168.1.12` (pagermaid用户)
- 部署目录: `/home/pagermaid/docker/sillyplus`

---

## 2026-06-23 部署最新版本到测试服务器

### 操作步骤

1. 从GitHub下载最新linux amd64二进制文件
2. SCP上传到测试服务器`/tmp/`
3. 停止pm2进程
4. 备份旧文件
5. 替换`sillyplus`二进制文件
6. 重启pm2服务

### 部署命令

```bash
# 下载最新版本
curl -L -o sillyGirl_linux_amd64 https://github.com/wjs876046992/sillyGirl/releases/latest/download/sillyGirl_linux_amd64

# 上传到测试服务器
scp sillyGirl_linux_amd64 pagermaid@192.168.1.12:/tmp/

# 部署（需要SSH到服务器执行）
ssh pagermaid@192.168.1.12
cd /home/pagermaid/docker/sillyplus
/home/pagermaid/.pm2/modules/pm2-logrotate/node_modules/pm2/bin/pm2 stop sillyplus
cp sillyplus sillyplus_bak_$(date +%Y%m%d_%H%M%S)
cp /tmp/sillyGirl_linux_amd64 sillyplus
chmod +x sillyplus
/home/pagermaid/.pm2/modules/pm2-logrotate/node_modules/pm2/bin/pm2 start sillyplus
```

### 部署状态

✅ 部署成功（2026-06-23 17:25）
- 进程ID: 2445487
- 状态: online

---

## 技术笔记

### PM2命令（测试服务器）

由于pm2不在PATH中，需要使用完整路径：
```bash
/home/pagermaid/.pm2/modules/pm2-logrotate/node_modules/pm2/bin/pm2 [command]
```

### 后端插件数据结构

`plugins/list.json` API返回完整插件信息：
- `id` - 插件UUID
- `icon` - 图标（emoji或URL）
- `debug` - 调试模式状态
- `disable` - 禁用状态
- `has_form` - 是否有配置表单
- `running` - 运行状态
- `classes` - 分类标签

`getCurrentUser` API返回的plugins列表缺少部分字段，需要通过`plugins/list.json`补充。

### 分佣功能依赖

分佣功能需要MongoDB，配置项：
- `fanli.mongodb` - MongoDB连接字符串
- 格式: `mongodb://username:password@host:port/database`
