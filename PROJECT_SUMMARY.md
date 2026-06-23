# SillyGirlClient 项目总结

## 项目概述

Android 移动端应用，用于管理和监控 SillyGirl AI 聊天机器人后端服务。基于 Jetpack Compose (Material 3) 开发，提供完整的服务器管理、插件市场、管理员管理、定时任务、分佣统计等功能。

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material 3 |
| 构建系统 | Gradle Kotlin DSL (AGP 8.12.1) |
| Kotlin 版本 | 2.0.20 |
| 网络层 | Retrofit 2.11.0 + OkHttp 4.12.0 |
| 图片加载 | Coil 3 (coil-compose 3.0.4) |
| 状态管理 | StateFlow + ViewModel |
| 导航 | Navigation Compose 2.9.0 |
| 目标 SDK | minSdk 26, compileSdk 36, targetSdk 36 |
| CI/CD | GitHub Actions (ubuntu + JDK 21 + Android SDK) |

## 项目结构

```
app/src/main/java/com/sillygirl/client/
├── MainActivity.kt                          # 应用入口，ServerConfig 初始化
├── data/
│   ├── api/
│   │   ├── ApiConfig.kt                     # 服务端基础 URL 管理
│   │   ├── RetrofitClient.kt                # Retrofit 实例管理，认证 Token 注入
│   │   └── SillyGirlApi.kt                  # API 接口定义（全部端点）
│   ├── model/Models.kt                      # 所有数据模型
│   └── repository/
│       ├── AuthRepository.kt                # 登录、注销、用户信息
│       ├── FenyongRepository.kt             # 分佣统计、订单查询
│       ├── MasterRepository.kt              # 管理员列表
│       ├── PluginRepository.kt              # 插件列表（已安装/市场）
│       ├── ServerConfig.kt                  # 多服务器配置持久化 (SharedPreferences)
│       └── TaskRepository.kt                # 定时任务列表
└── ui/
    ├── components/AppComponents.kt          # 可复用 UI 组件
    ├── navigation/AppNavGraph.kt            # 导航路由、登录状态管理
    ├── theme/Theme.kt                       # 主题、颜色、字体
    └── screens/
        ├── dashboard/    DashboardScreen + ViewModel
        ├── fenyong/      FenyongScreen + ViewModel
        ├── login/        LoginScreen + ViewModel
        ├── masters/      MastersScreen
        ├── plugins/      PluginScreens + PluginViewModels
        ├── serverlist/   ServerListScreen + ViewModel
        ├── service/      ServiceScreen
        ├── settings/     SettingsScreen + ViewModel
        ├── storage/      StorageScreen
        └── tasks/        TasksScreen
```

## 功能模块

| 模块 | 功能 |
|------|------|
| 服务器列表 | 多服务器管理，支持添加/编辑/删除/切换/默认设置 |
| 登录 | Token 认证，自动恢复登录状态 |
| 仪表盘 | 概览统计（插件数、管理员数、任务数），分佣概览，功能入口网格 |
| 分佣统计 | 收益看板（今日/7天/本月），订单列表，关键词搜索，分页加载 |
| 插件市场 | 已安装插件列表，市场插件浏览，安装/卸载 |
| 管理员 | 管理员列表展示 |
| 定时任务 | 任务列表、启停管理 |
| 服务 | 服务管理 |
| 存储 | Key-Value 存储查看和编辑 |
| 设置 | 退出登录 |

## 开发进展

- **总提交数**: 105 个 commit
- **开发阶段**: 功能开发中，核心模块基本完成
- **CI**: GitHub Actions 工作流已稳定，push 到 main 自动构建 APK 并上传

## 已重复解决的问题

### Compose Modifier.weight() 编译问题

在开发过程中多次遇到 `Modifier.weight()` 在 `Row`/`Column` 中无法正确工作的编译问题。经过多轮迭代（包括尝试添加 `weight_` 辅助函数、替换导入为显式 import、降级 Kotlin 版本等），最终发现是 `RowScope` 作用域未正确导入导致 `weight()` 扩展函数无法调用。修复方式为补充 `import androidx.compose.foundation.layout.RowScope` 或确保 `.weight()` 在正确的 scope 中调用。此问题影响了 FenyongScreen、ServerListScreen 等多个页面。

### CI/CD 工作流不稳定

初期 CI 经历了多轮修复：Gradle cache 导致的构建问题、Java 版本不匹配（17 → 21）、Android SDK 未配置、GITHUB_TOKEN YAML 别名转义问题、APK 路径查找失败等，共 10+ 次 commit 后才稳定。最终方案：直接 `assembleDebug` + `gh release`，使用 JDK 21 (Temurin)。

### 卡片内容为空

`Content()` 函数未正确被调用，导致 `GradientCard`、`GlassCard` 等所有自定义卡片组件的内容区域为空。修复方式为确保 `@Composable private fun Content()` 被正确调用。

### Kotlin 版本兼容

Kotlin 2.0.21 与 Compose BOM 2024.09.00 存在兼容性问题，降级到 Kotlin 2.0.20 后解决。
