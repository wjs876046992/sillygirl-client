# sillygirl-client 开发进展

## 2026-06-25 第十五次会话完成的工作

### 一、新增发送消息功能

**问题描述：**
- 需要实现发送消息功能，支持向指定用户/群组发送消息
- 后端 API `/api/chat/selects` 返回全部数据，未按平台过滤
- 搜索框无法唤起输入法，影响用户体验

**解决方案：**

1. **新增 Chat 功能模块**
   - `ChatRepository.kt` - 聊天数据仓库
   - `ChatViewModel.kt` - 聊天状态管理
   - `ChatScreen.kt` - 聊天界面 UI
   - 支持选择平台、机器人、用户/群组发送消息

2. **后端 API 优化**
   - `/api/chat/selects` 新增 `platform` 和 `bot_id` 参数
   - 后端过滤，只返回匹配的用户/群组
   - 跳过空昵称数据，提升数据质量

3. **客户端优化**
   - 选择平台时重新请求 API（带 platform 参数）
   - 删除本地过滤逻辑，使用后端过滤结果
   - 简化代码，移除调试方法

4. **搜索对话框优化**
   - SearchableDropdown 改用 AlertDialog + LazyColumn 实现
   - 解决 ExposedDropdownMenu 中输入法无法唤起问题
   - 支持实时搜索过滤，显示匹配数量

5. **登录认证优化**
   - 登录 token 保存到 SharedPreferences，支持自动登录
   - 会话过期处理优化，区分网络错误和认证失败
   - Cookie 认证方式从 X-Token 改为标准 Cookie

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `ChatRepository.kt` | 新增聊天数据仓库 |
| `ChatViewModel.kt` | 新增聊天状态管理 |
| `ChatScreen.kt` | 新增聊天界面 UI |
| `SillyGirlApi.kt` | 新增 getChatSelects API，支持 platform 参数 |
| `Models.kt` | 新增聊天相关数据模型 |
| `AppNavGraph.kt` | 新增 Chat 路由 |
| `DashboardScreen.kt` | 新增发送消息入口 |
| `RetrofitClient.kt` | Cookie 认证方式优化 |
| `LoginViewModel.kt` | 登录 token 保存优化 |

**后端改动（sillyGirl）：**
| 文件 | 修改内容 |
|------|----------|
| `chat_api.go` | `/api/chat/selects` 新增 platform/bot_id 参数支持 |

---

## 2026-06-25 第十四次会话完成的工作

### 一、分佣订单状态旁增加绑定用户信息

**问题描述：**
- 分佣订单列表中需要显示绑定用户信息
- 绑定信息在 API 返回值的 `bind` 字段中，包含 `platform`、`userId` 和 `nickname`
- 需要兼容 `bind` 为 null 的情况

**解决方案：**

1. **Models.kt 数据模型更新**
   - `FenyongBind` 新增 `nickname: String` 字段（与后端 `json:"nickname,omitempty"` 对应）

2. **UI 修改**
   - 在订单状态标签旁增加绑定用户信息显示
   - 格式：`渠道名 + 用户名`（如 "微信 张三"）
   - 用户名优先使用 `nickname`，无昵称时使用 `userId`
   - 无平台信息时仅显示用户名
   - 使用 `Icons.Filled.Person` 图标 + 文本，背景 `primaryContainer` 颜色
   - 仅当 `bind` 不为 null 且 `userId` 不为空时显示

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `Models.kt` | FenyongBind 新增 nickname 字段 |
| `FenyongScreen.kt` | OrderItemCard 中状态标签旁增加绑定用户信息显示 |

---

## 2026-06-25 第十三次会话完成的工作

### 一、迁移设置页面基础配置功能

**问题描述：**
- 需要将 sillyGirl 中 `/admin/system/basic` 页面的设置功能迁移到 Android 客户端
- 原页面提供服务器基础配置管理、一键升级和重启功能

**功能分析：**

sillyGirl 的基础配置页面通过 `/api/storage` 接口管理以下配置项：
- `app.name` — 登录账号（机器人名称）
- `app.password` — 登录密码
- `app.started_at` — 启动时间（只读）
- `app.compiled_at` — 编译版本（只读）
- `app.machine_id` — 机器码（只读）
- `app.port` — 端口（修改后跳转新端口）
- `app.storage` — 默认存储（BoltDB/Redis）
- `app.redis_addr` / `app.redis_password` — Redis 配置

操作功能：
- **一键升级**：向 `app.compiled_at` 写入随机字符串，触发后端下载新版本
- **重启程序**：向 `app.started_at` 写入随机字符串，触发后端重启

**解决方案：**

1. **SettingsViewModel.kt 扩展**
   - 新增 `loadBasicConfig()` — 启动时自动加载 10 个 `app.*` 配置键
   - 新增 `saveConfig(key)` — 单字段保存，失焦或回车触发
   - 新增 `upgrade()` — 一键升级（写入随机值到 `app.compiled_at`）
   - 新增 `restart()` — 重启程序（写入随机值到 `app.started_at`）
   - 新增 `SettingsUiState` 字段：`configData`、`editingConfig`、`isUpgrading`、`isRestarting`

2. **SettingsScreen.kt UI 重构**
   - **服务器信息卡片** — 显示当前服务器地址
   - **基础配置卡片** — 包含所有可编辑/只读配置字段
   - **操作按钮卡片** — 一键升级、重启程序（带 loading 状态）
   - **关于卡片** — 版本信息
   - **退出登录按钮** — 保留原有功能
   - 新增 `ConfigTextField` 组件 — 支持密码遮掩、提示图标、回车保存

3. **技术细节**
   - 密码字段使用 `PasswordVisualTransformation` 遮掩
   - `FilterChip` 切换 BoltDB/Redis 存储
   - Redis 配置条件显示（仅当选择 Redis 时）
   - 键盘 Done 动作自动保存并隐藏键盘

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `SettingsViewModel.kt` | 新增配置加载/保存/升级/重启功能 |
| `SettingsScreen.kt` | UI 全面重构，新增基础配置和操作卡片 |

---

## 2026-06-25 第十二次会话完成的工作

### 一、定时任务 API 全面修正（修复 404 错误）

**问题描述：**
- 保存/删除/启用/执行任务全部返回 404
- 客户端使用的 API 端点与后端完全不匹配
- 缺少发送环境（senders）和脚本（scripts）选择功能

**根因分析：**
- 客户端调用 `/api/tasks/add`、`/api/tasks/edit`、`/api/tasks/del`、`/api/tasks/setEnable` 等不存在的端点
- 后端实际 API：
  - `POST /api/tasks` — 创建和编辑（body 含 `task_id`）
  - `DELETE /api/tasks` — 删除（body 含 `task_id`）
  - `GET /api/tasks/run?task_id=xxx` — 执行
  - `GET /api/task/selects?task_id=xxx` — 获取可选脚本和平台

**解决方案：**

1. **SillyGirlApi.kt API 端点修正**
   - `addTask` + `editTask` + `setTaskEnable` → 统一为 `saveTask`（`POST /api/tasks`，body 为 `Map<String, Any>`）
   - `delTask` → `deleteTask`（`DELETE /api/tasks`，body 为 `Map<String, String>`）
   - `runTask` → 改为 `GET` 请求，`task_id` 作为 Query 参数
   - 新增 `getTaskSelects`（`GET /api/task/selects`）获取可选脚本和平台

2. **Models.kt 数据模型更新**
   - `TaskInfo` 新增 `scripts: List<String>` 和 `senders: List<TaskSender>` 字段
   - 新增 `TaskSender`（platform/chatId/userId/botId）
   - 新增 `TaskSelectsResponse`、`TaskSelectsData`、`NameOption`
   - 移除不再需要的 `TaskAddRequest`、`TaskEditRequest`、`TaskActionRequest`、`TaskSetEnableRequest`

3. **TasksScreen.kt 编辑弹窗重写**
   - 打开编辑弹窗时自动调用 `/api/task/selects` 获取可选项
   - **执行脚本选择**：从 selects 获取所有可用脚本（UUID → 标题），支持多选（Checkbox）
   - **执行环境选择**：
     - 平台选择（从 selects.platforms 获取）
     - 机器人选择（如果平台有多个 bot）
     - 目标选择（群组/用户，从 selects.group_names/user_names 获取）
     - 支持多环境（可添加多个 sender）
   - **任务保存**：body 包含 `task_id`、`title`、`schedule`、`command`、`remark`、`scripts`、`senders`
   - 新建任务时自动生成 UUID 作为 `task_id`

4. **TasksViewModel 修正**
   - `toggleTask` → 使用 `saveTask` 传 `task_id` + `enable`
   - `deleteTask` → 使用 `deleteTask`（`DELETE /api/tasks`）
   - `runTask` → 使用 `GET /api/tasks/run?task_id=xxx`
   - `saveTask` → 统一创建/编辑方法，body 为 `Map<String, Any>`
   - 新增 `loadSelects()` 加载可选项

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `SillyGirlApi.kt` | API 端点全面修正，新增 getTaskSelects |
| `Models.kt` | TaskInfo 新增 scripts/senders 字段，新增 TaskSender/TaskSelectsResponse 等 |
| `TasksScreen.kt` | 编辑弹窗重写，支持脚本选择和发送环境选择 |

---

## 2026-06-25 第十一次会话完成的工作

### 一、修复后台切回后登录状态丢失

**问题描述：**
- App 后台运行中再打开，登录状态丢失，跳转到服务器列表
- Token 有效期为 1 天（`Max-Age=86400`），后台短暂运行不应导致 token 过期

**根因分析：**
- `AppNavGraph.kt` 中有三处在 API 调用失败时**无条件清除 token**：
  1. `verifySession()` 的 `onFailure` 回调
  2. `loadCurrentUser()` 的 `catch` 块
  3. 外层 `catch` 块
- App 进程被系统回收后重建时，CookieManager 内存 Cookie 丢失，且后台切前台瞬间网络可能尚未就绪
- 此时 `getCurrentUser()` 抛出 `SocketTimeoutException` / `IOException`，被无条件当作 token 过期处理，直接清除了有效 token

**解决方案：**

1. **区分认证失败与网络错误**
   - 仅在 `retrofit2.HttpException` 且状态码为 401/403 时清除 token
   - 网络超时、DNS 解析失败等瞬时错误保留 token，允许用户进入 Dashboard 后通过下拉刷新重试

2. **加载 currentUser 失败时不清除 token**
   - 移除 `loadCurrentUser` catch 块中的 `clearToken` / `isLoggedIn=false`
   - 不自动重试（避免无限循环），由用户通过刷新按钮或下拉刷新手动重试

3. **外层 catch 同样区分错误类型**
   - 401/403 → 清 token + 跳转服务器列表
   - 其他 → 保留 token + 进入 Dashboard

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `AppNavGraph.kt` | 三处错误处理改为区分 401/403 vs 网络错误，仅认证失败时清除 token |

**已知微小瑕疵：**
- token 真过期（401）时，`sessionInterceptor` 在后台线程清 token 并 post `onSessionExpired` 回调到 Main，同时 `onFailure` 设 `isLoggedIn = true`，可能闪一下 Dashboard 再跳转到服务器列表（毫秒级），不影响功能。

---

## 2026-06-24 第十次会话完成的工作

### 一、重载和卸载增加确认提示

**问题描述：**
- 重载和卸载操作没有确认提示，容易误操作
- 需要增加确认对话框，防止用户误触

**解决方案：**

1. **重载确认对话框**
   - 新增 `showReloadDialog` 状态变量
   - 新增重载确认对话框，显示插件名称
   - 用户可点击"确定"重载或"取消"放弃

2. **卸载确认对话框**
   - 已有卸载确认对话框，保持不变

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `PluginScreens.kt` | 新增 `showReloadDialog` 状态和重载确认对话框 |

### 二、我的插件去掉来源筛选

**问题描述：**
- 我的插件页面的来源筛选功能使用频率低
- 简化UI，只保留搜索和分类筛选

**解决方案：**

1. **移除来源筛选**
   - 移除 `selectedOrigins` 状态变量
   - 移除 `allOrigins` 变量
   - 移除来源筛选 UI（FilterChip 列表）
   - 简化过滤逻辑，只保留搜索和分类筛选

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `PluginScreens.kt` | 移除来源筛选相关代码 |

---

## 2026-06-24 第九次会话完成的工作

### 一、插件安装/卸载/重载后刷新首页

**问题描述：**
- 安装或卸载插件后，首页的插件数量不会自动更新
- 需要手动刷新或重新进入首页才能看到最新数据

**解决方案：**

1. **AppNavGraph 新增 `refreshCurrentUser` 回调**
   - 创建 `refreshCurrentUser` lambda，调用 `getCurrentUser` API 更新 `currentUser` 状态
   - 传递给 `MyPluginsScreen` 和 `PluginMarketScreen`

2. **PluginMarketViewModel 增强**
   - `installPlugin()` 和 `uninstallPlugin()` 新增可选 `onSuccess` 回调参数
   - 安装/卸载成功后调用回调刷新首页

3. **MyPluginsViewModel 增强**
   - `reloadPlugin()` 新增可选 `onSuccess` 回调参数
   - 重载成功后调用回调刷新首页

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `AppNavGraph.kt` | 新增 `refreshCurrentUser` 回调，传递给插件页面 |
| `PluginViewModels.kt` | `installPlugin()`/`uninstallPlugin()`/`reloadPlugin()` 新增 `onSuccess` 回调 |
| `PluginScreens.kt` | `MyPluginsScreen` 和 `PluginMarketScreen` 接收并使用 `onRefreshCurrentUser` |

### 二、插件列表页添加重载和卸载按钮

**问题描述：**
- 重载和卸载操作只能在插件详情页进行
- 用户需要进入详情页才能执行这些常用操作，不够便捷

**解决方案：**

1. **MyPluginCard 增强**
   - 新增 `onReload` 和 `onUninstall` 回调参数
   - 在插件卡片底部添加操作按钮行（分隔线 + 重载/卸载芯片）
   - 重载按钮：Refresh 图标 + "重载" 文字
   - 卸载按钮：Delete 图标 + "卸载" 文字（红色）

2. **MyPluginsScreen 增强**
   - 新增卸载确认对话框（`showUninstallDialog` 状态）
   - 卸载操作带确认对话框，防止误操作
   - 卸载/重载成功后自动刷新首页

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `PluginScreens.kt` | `MyPluginCard` 添加重载/卸载按钮，`MyPluginsScreen` 添加卸载确认对话框 |

---

## 2026-06-24 第八次会话完成的工作

### 一、移除插件市场的来源和分类筛选

**问题描述：**
- 插件市场的来源筛选和分类筛选功能过于复杂，实际使用频率低
- 简化UI，只保留关键词搜索和Tab切换

**解决方案：**

1. **PluginMarketScreen 简化**
   - 移除 `selectedOrigins`、`selectedClass` 状态变量
   - 移除 `originsExpanded`、`classesExpanded` 折叠状态
   - 移除来源筛选和分类筛选的 UI 组件
   - 移除 `CollapsibleFilterSection` 可折叠组件（已无使用）
   - 移除 `AnimatedVisibility`、`clickable` 等无用 import

2. **PluginMarketViewModel 简化**
   - 移除 `currentClassFilter`、`currentOrigins` 状态
   - 移除 `availableOrigins`、`availableClasses` 字段
   - `load()` 函数简化，移除 `classFilter`、`origins` 参数
   - `switchTab()` 简化，不再传递筛选参数
   - `loadWithFilters()` 替换为 `loadWithKeyword()`

3. **PluginRepository 简化**
   - `getAvailablePlugins()` 移除 `classFilter`、`origins` 参数

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `PluginScreens.kt` | 移除来源/分类筛选UI、移除CollapsibleFilterSection |
| `PluginViewModels.kt` | 简化状态和load函数、替换loadWithFilters为loadWithKeyword |
| `PluginRepository.kt` | 简化getAvailablePlugins参数 |

---

## 2026-06-24 第七次会话完成的工作

### 一、插件市场功能迁移（从 admin/plugin/share）

**问题描述：**
- 原始 admin 页面 `/admin/plugin/share` 包含完整的插件市场功能
- sillygirl-client 缺少部分功能：插件异常消息显示、认证来源标记、加密/模块标签、Tab 计数从 API 获取

**解决方案：**

1. **数据模型更新**
   - `PluginInfo` 新增字段：`organization`(组织)、`identified`(认证来源)、`status`(状态 0/1/2/6)、`encrypt`(加密)、`module`(模块)、`messages`(异常消息)、`createAt`(创建时间)
   - `PluginMessage` 新增数据类：`unix`(时间戳)、`messageClass`(warn/error)、`version`(版本)、`content`(内容)
   - `PluginListResponse` 新增 `tab1`/`tab2`/`tab3` 计数字段和 `tab` 字段
   - `PluginListResult` 新增 `tab1`/`tab2`/`tab3` 计数字段

2. **ViewModel 更新**
   - `PluginMarketUiState` 新增 `messagesDialogPlugin` 字段用于异常消息弹窗
   - `PluginMarketViewModel` 新增方法：`showMessagesDialog()`、`dismissMessagesDialog()`、`clearPluginMessages()`
   - `loadTabCounts()` 改为从 API 响应获取 tab 计数

3. **UI 更新**
   - `MarketPluginCard` 新增标签：
     - 认证来源标签（金色背景，显示 organization）
     - 模块标签（紫色背景）
     - 加密脚本标签（橙色背景）
     - 可升级状态标签
     - 异常消息按钮（显示消息数量）
   - 新增 `PluginMessagesDialog` 异常消息弹窗：
     - 时间线样式显示消息
     - 显示时间、版本标签、内容
     - 清空按钮
   - 新增 `formatMessageTime()` 时间格式化函数

4. **API 对齐**
   - 使用服务端返回的 `tab1`/`tab2`/`tab3` 计数，不再单独请求

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `Models.kt` | `PluginInfo` 新增字段、`PluginMessage` 新增类、`PluginListResponse`/`PluginListResult` 新增 tab 计数 |
| `PluginViewModels.kt` | 新增异常消息相关方法、更新 tab 计数逻辑 |
| `PluginScreens.kt` | 新增标签显示、异常消息弹窗、时间格式化 |
| `PluginRepository.kt` | `getAvailablePlugins()` 传递 tab 计数 |

---

## 2026-06-24 第六次会话完成的工作

### 零、插件市场筛选参数修正

**问题描述：**
- `origin` 参数应传数组（多选），`class` 参数应传字符串（单选）
- 分类筛选和来源筛选应默认折叠，展开后可操作
- 筛选变更应立即触发服务端重新查询（而非客户端过滤）

**解决方案：**
1. `SillyGirlApi.getPluginList()` — `origin` 改为 `List<String>?`（Retrofit 自动编码为重复参数），`class` 保持 `String?`
2. `PluginRepository` / `PluginMarketViewModel` — 筛选参数同步更新
3. `PluginMarketScreen` — 新增 `CollapsibleFilterSection` 可折叠组件：
   - 分类筛选：单选（FilterChip + "全部"选项），默认折叠
   - 来源筛选：多选（FilterChip），默认折叠
   - 标题行可点击展开/收起，显示激活筛选数量角标
4. 筛选变更时调用 `loadWithFilters()` 重新从服务端加载，移除客户端过滤逻辑

### 一、插件市场增强：服务端筛选 + 已安装插件操作

**问题描述：**
- 插件市场的来源和分类筛选使用客户端数据（从当前页插件列表提取），数据不完整
- API 返回了 `classes` 和 `origins` 字段作为服务端提供的筛选选项，但前端未使用
- 插件市场已安装的插件缺少配置表单入口
- 已安装插件的操作按钮在一行中可能溢出

**解决方案：**

1. **API 模型更新**
   - `PluginListResponse` 新增 `classes: Map<String, Int>` 和 `origins: Map<String, String>` 字段
   - `PluginListResult` 同步新增这两个字段
   - `SillyGirlApi.getPluginList()` 新增 `classFilter`(单选)、`origins`(多选数组)、`keyword` 查询参数

2. **服务端筛选支持**
   - `PluginRepository.getAvailablePlugins()` 和 `getInstalledPlugins()` 支持 `classFilter`(String?)、`origins`(List<String>?)、`keyword` 参数
   - `PluginMarketViewModel.load()` 支持相同参数
   - 新增 `loadWithFilters()` 统一筛选方法

3. **PluginMarketScreen UI 增强**
   - 使用服务端提供的 `origins` 和 `classes` 作为筛选选项
   - **分类筛选**：单选（FilterChip），默认折叠，点击展开/收起
   - **来源筛选**：多选（FilterChip），默认折叠，点击展开/收起
   - 搜索栏：服务端关键词搜索
   - 已安装插件操作按钮使用 `FlowRow` 防止溢出
   - 新增 `CollapsibleFilterSection` 可折叠筛选组件（带激活数量角标）

4. **配置表单入口**
   - `MarketPluginCard` 新增 `onConfigForm` 回调
   - 已安装且有表单的插件显示"配置"芯片（Settings 图标）

5. **导航更新**
   - `AppNavGraph` 中 `PluginMarketScreen` 新增 `onPluginClick` 回调

**修改文件：**
| 文件 | 修改内容 |
|------|----------|
| `Models.kt` | `PluginListResponse` 和 `PluginListResult` 新增 `classes`/`origins` 字段 |
| `SillyGirlApi.kt` | `getPluginList()` 新增 `classFilter`/`origins`(List)/`keyword` 参数 |
| `PluginRepository.kt` | 分页方法支持筛选参数，返回服务端筛选数据 |
| `PluginViewModels.kt` | `PluginMarketUiState` 新增筛选选项字段，`loadWithFilters()` 统一筛选 |
| `PluginScreens.kt` | 可折叠筛选面板、单选分类、多选来源、配置表单入口 |
| `AppNavGraph.kt` | PluginMarketScreen 新增 onPluginClick 导航 |
| `PROJECT_SUMMARY.md` | 更新文档 |

---

## 2026-06-24 第五次会话完成的工作

### 一、插件列表支持分页查询

**问题描述：**
- 我的插件（MyPluginsScreen）和插件市场（PluginMarketScreen）都是一次性加载所有插件
- 当插件数量较多时，加载速度慢，用户体验差

**解决方案：**

1. **PluginRepository 分页支持**
   - `getInstalledPlugins(page, pageSize)` - 支持分页参数
   - `getAvailablePlugins(page, pageSize)` - 支持分页参数
   - 返回 `PluginListResult` 包含分页信息

2. **PluginListResult 数据模型**
   ```kotlin
   data class PluginListResult(
       val plugins: List<PluginInfo>,
       val total: Int,
       val page: Int,
       val pageSize: Int,
   ) {
       val totalPages: Int
       val hasNextPage: Boolean
       val hasPrevPage: Boolean
   }
   ```

3. **ViewModel 状态更新**
   - `MyPluginsUiState` 添加分页字段：currentPage, totalPages, total, pageSize, isLoadingMore
   - `PluginMarketUiState` 添加分页字段
   - `loadPlugins()` 和 `load()` 方法支持分页参数
   - 添加 `loadNextPage()` 和 `goToPage(page)` 方法

4. **UI 分页控件**
   - 新增 `PaginationWidget` 组件（上一页/下一页按钮 + 页码显示）
   - MyPluginsScreen 和 PluginMarketScreen 底部显示分页控件
   - 仅当 totalPages > 1 时显示

**修改文件：**
- `Models.kt` - 新增 PluginListResult 数据类
- `PluginRepository.kt` - 分页查询方法
- `PluginViewModels.kt` - 分页状态管理
- `PluginScreens.kt` - 分页 UI 控件
- `AppNavGraph.kt` - 更新 MyPluginsScreen 导航调用

### 二、首页支持下拉刷新

**问题描述：**
- DashboardScreen 不支持下拉刷新
- 用户需要点击刷新按钮才能更新数据

**解决方案：**

1. **下拉刷新实现**
   - 使用 `pointerInput` 和 `detectVerticalDragGestures` 检测下拉手势
   - 下拉距离超过阈值（150px）后松手触发刷新
   - 刷新时显示 CircularProgressIndicator

2. **UI 状态**
   - `isRefreshing` - 是否正在刷新
   - `pullDistance` - 下拉距离
   - 下拉过程中显示箭头图标，角度随下拉距离变化

3. **刷新流程**
   - 下拉超过阈值 → 松手 → 显示加载动画 → 调用 `viewModel.loadDashboard(forceRefresh = true)`
   - 数据加载完成 → 停止加载动画

**修改文件：**
- `DashboardScreen.kt` - 添加下拉刷新功能

### 修改文件清单

| 文件 | 修改内容 |
|------|----------|
| `Models.kt` | 新增 PluginListResult 数据类 |
| `PluginRepository.kt` | 分页查询方法 |
| `PluginViewModels.kt` | 分页状态管理 |
| `PluginScreens.kt` | 分页 UI 控件 |
| `AppNavGraph.kt` | 更新 MyPluginsScreen 导航调用 |
| `DashboardScreen.kt` | 添加下拉刷新功能 |
| `PROJECT_SUMMARY.md` | 更新文档 |

---

## 2026-06-24 第四次会话完成的工作

### 一、修复切换服务器失败选中错误的问题

**问题描述：**
- 用户尝试切换服务器时，如果登录失败，客户端会指向错误的服务器
- 原因：`authRepo.login()` 内部调用 `RetrofitClient.setServer(serverUrl)`，即使登录失败，客户端已经指向新服务器

**解决方案：**
- 切换前保存原始服务器地址 `originalUrl = currentUrl.value`
- 登录失败时调用 `RetrofitClient.setServer(originalUrl)` 恢复
- 成功时才更新 `currentUrl.value` 和 `setDefaultIndex`

### 二、添加服务编辑功能

**问题描述：**
- 服务管理只有添加/删除/切换功能，无法编辑已有服务器信息

**解决方案：**
- ServiceCard 新增编辑按钮（铅笔图标）
- 新增 `EditServiceDialog`，预填充当前服务器信息（地址、别名、用户名、密码）
- 编辑保存后，如果是当前服务器，同步更新 `RetrofitClient` 的地址

### 三、移除测试服务器硬编码

**问题描述：**
- APK 内置了测试服务器 `http://192.168.1.12:8081` 及其凭证
- 用户无法选择其他服务器

**解决方案：**
- 移除 `MainActivity.kt` 中自动添加测试服务器的代码
- 用户首次使用需要手动添加服务器

### 修改文件清单

| 文件 | 修改内容 |
|------|----------|
| `ServiceScreen.kt` | 添加编辑功能，修复切换失败bug |
| `MainActivity.kt` | 移除测试服务器硬编码 |
| `PROJECT_SUMMARY.md` | 更新文档 |

---

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

---

## 2026-06-25 第十五次会话完成的工作

### 一、Dashboard 分佣 API 重构（后端）

**问题描述：**
- 原 dashboard API 只返回扁平的时间维度和平台维度，缺少 site × 时间的交叉数据
- 原 `fgPeriod`/`fgPlatform` 的 BSON tag 写成 `"count"` 而 `$group` 输出字段名为 `"orders"`，导致订单数始终为 0

**解决方案：**

1. **类型重构**
   - 删除 `FenyongPlatformStats`、`FenyongPeriodStats`，统一为 `FenyongStats{Orders, Estimate, Actual}`
   - 新增 `FenyongCrossItem{Site, Period, Orders, Estimate, Actual}` 表示交叉统计
   - `FenyongDashboard` 改为 `{Success, ByTime, BySite, Cross}`

2. **聚合管道**
   - 单次 `$facet` 查询同时完成三个维度聚合：
     - `by_time`: today / last7days / lastMonth / all_time
     - `by_site`: 按 `$site` 分组
     - `cross`: `$addFields` 按 created_time 归类时段 → `$match` 过滤 → `$group` 按 site+period 分组 → `$project` 扁平化
   - 修复 BSON tag：`fgPeriod.Count` → `fgPeriod.Orders`（`bson:"orders"`）

**返回结构：**
```json
{
  "by_time": { "today": {...}, "last7days": {...}, "lastMonth": {...}, "total": {...} },
  "by_site": { "jd": {...}, "tb": {...}, "pdd": {...}, "total": {...} },
  "cross": [
    { "site": "jd", "period": "today", "orders": 5, "estimate": 10.5, "actual": 8.2 },
    ...
  ]
}
```

### 二、Dashboard 分佣 UI 重构（前端）

**问题描述：**
- 原 dashboard 分佣卡片只显示简单的三列时间维度数据，信息密度低
- 需要展示 site × 时间 的交叉表格，同时显示预估佣金和实际佣金

**解决方案：**

1. **Models.kt**
   - 新增 `FenyongCrossItem` 数据类
   - `FenyongDashboardResponse` 新增 `cross: List<FenyongCrossItem>` 字段
   - 删除旧的 `FenyongPeriodStats`、`FenyongPlatformStats`

2. **DashboardScreen.kt — FenyongOverviewCard 三板块布局**
   - **板块① 全部金额总览**：全部预估 / 全部实际 / 总订单（灰色背景卡片）
   - **板块② 交叉表格**：3行(京东/淘宝/拼多多) × 3列(今日/7天/本月) + 合计行，使用 `weight(1f)` 自适应宽度，无需滚动
   - **板块③ 平台汇总卡片**：每个平台一张小卡片，显示名称/预估/实际/订单数，平台色背景

3. **AppNavGraph.kt — 导航栏调整**
   - `MiniAppBar` 标题从 "SillyGirl" 改为 "傻妞平台管理助手 · 用户名"
   - 移除 Dashboard 中的 `WelcomeHeader` 欢迎面板

### 三、部署

- 通过 `deploy-test.sh` 部署到测试服务器 `192.168.1.12:8081`
- 流程：构建 linux amd64 二进制 → SCP 上传 → SSH 执行 PM2 restart → 健康检查 HTTP 200

**修改文件汇总：**

| 仓库 | 文件 | 修改内容 |
|------|------|----------|
| sillyGirl | `core/fenyong_api.go` | 类型重构 + cross 聚合管道 + 修复 BSON tag |
| sillyGirl | `deploy-test.sh` | 部署脚本 |
| sillygirl-client | `Models.kt` | 新增 FenyongCrossItem，更新 DashboardResponse |
| sillygirl-client | `DashboardScreen.kt` | 三板块布局 + 删除 WelcomeHeader |
| sillygirl-client | `AppNavGraph.kt` | 导航栏标题改名 + 显示用户名 |
| sillygirl-client | `FenyongRepository.kt` | 日志字段更新 |
| sillygirl-client | `SillyGirlApi.kt` | API 注释更新 |

---

## 2026-06-25 第十六次会话完成的工作

### 一、导航栏标题改名 + 移除欢迎面板

- `MiniAppBar` 标题从 "SillyGirl" 改为 "傻妞平台管理助手 · 用户名"
- Dashboard 中删除 `WelcomeHeader` 欢迎面板及其函数

### 二、佣金明细表格内容居中

- 表格数据单元格和合计行的 Text 组件添加 `textAlign = TextAlign.Center`
- 确保金额数值在单元格内水平居中显示

### 三、后端修复订单数始终为0的bug

- `fgPeriod`/`fgPlatform` 的 BSON tag 从 `bson:"count"` 改为 `bson:"orders"`，与 `$group` 输出字段名一致

**修改文件汇总：**

| 仓库 | 文件 | 修改内容 |
|------|------|----------|
| sillygirl-client | `AppNavGraph.kt` | 导航栏标题改名 + 显示用户名 |
| sillygirl-client | `DashboardScreen.kt` | 删除 WelcomeHeader + 表格内容 textAlign 居中 |
| sillyGirl | `core/fenyong_api.go` | BSON tag count→orders 修复 |
