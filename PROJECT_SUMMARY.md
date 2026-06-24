# SillyGirl Client Project Summary

> Auto-generated for agent readability. Last updated: 2026-06-24 (latest session)

## 1. Repository Overview

- **App ID**: `com.sillygirl.client`
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Build**: Gradle Kotlin DSL (AGP 8.12.1, Kotlin 2.0.20, Compose BOM 2024.09.00)
- **SDK**: minSdk 26, compileSdk 36, targetSdk 36
- **JDK**: 21 (Temurin, homebrew)
- **Kotlin files**: 31
- **Resource files**: 122
- **Purpose**: Android mobile management app for sillyGirl AI chatbot backend. Provides server list, dashboard, commission statistics, plugin marketplace, admin management, cron tasks, and KV storage.

## 2. Top-Level Layout

```
sillygirl-client/
  build.gradle.kts                  # Root: AGP 8.12.1 + Kotlin 2.0.20 + Compose plugin
  app/build.gradle.kts              # Module: Compose BOM, Retrofit, OkHttp, Coil, Coroutines
  settings.gradle.kts               # Module inclusion
  gradle.properties                 # Gradle config (JVM args, AndroidX, JDK path, SDK path)
  local.properties                  # SDK path
  gradlew / gradlew.bat             # Gradle wrapper
  .github/workflows/build-apk.yml   # CI: assembleDebug + gh release
  PROJECT_SUMMARY.md                # This file

  app/src/main/
    AndroidManifest.xml             # INTERNET + ACCESS_NETWORK_STATE permissions, cleartext traffic
    res/
      values/
        themes.xml                  # Material Light NoActionBar (Compose handles theming)
        strings.xml                 # app_name = "SillyGirl客户端"
        ic_launcher_colors.xml      # Launcher icon colors
      mipmap-anydpi-v26/
        ic_launcher.xml             # Adaptive icon

    java/com/sillygirl/client/
      MainActivity.kt               # Entry point
      SillyGirlApp.kt               # Application class (Coil SingletonImageLoader.Factory)

      data/                          # 数据层 (10 files)
        api/
          ApiConfig.kt              # Server base URL singleton
          RetrofitClient.kt         # Retrofit/OkHttp singleton, X-Token interceptor, CookieJar
          SillyGirlApi.kt           # Retrofit interface (22 endpoints)
        model/
          Models.kt                 # All data models (20+ data classes)
        repository/
          ServerConfig.kt           # Multi-server config + token persistence (SharedPreferences)
          AuthRepository.kt         # Login / verify session / get current user / logout
          FenyongRepository.kt      # Commission dashboard + order list (pagination)
          PluginRepository.kt       # Plugin list (installed / available)
          MasterRepository.kt       # Admin list
          TaskRepository.kt         # Cron task list

      ui/                            # 表现层 (19 files)
        components/
          AppComponents.kt          # 7 reusable Composable widgets
        navigation/
          AppNavGraph.kt            # NavHost + 11 routes + MiniAppBar + login flow
        theme/
          Theme.kt                  # Light/dark theme, brand colors, typography
        screens/
          dashboard/
            DashboardScreen.kt      # Overview stats + feature grid (6 nav items)
            DashboardViewModel.kt   # Dashboard state: loading, error, fenyong, counts
          login/
            LoginScreen.kt          # Server + username/password login form
            LoginViewModel.kt       # Login state handling
          serverlist/
            ServerListScreen.kt     # Server list (add/edit/delete/switch/default)
            ServerListViewModel.kt  # Server list state + factory
          fenyong/
            FenyongScreen.kt        # Commission dashboard + order list with image cache
            FenyongViewModel.kt     # Commission state: orders, pagination, ImageCache
          plugins/
            PluginScreens.kt        # Installed plugins + plugin marketplace tabs
            PluginViewModels.kt     # Plugin state management (install/uninstall/toggle)
          masters/
            MastersScreen.kt        # Admin user list + add dialog (inline ViewModel)
          tasks/
            TasksScreen.kt          # Cron task list with enable/run/delete (inline ViewModel)
          storage/
            StorageScreen.kt        # KV storage viewer/editor (inline ViewModel)
          service/
            ServiceScreen.kt        # Service management view (placeholder, WIP)
          settings/
            SettingsScreen.kt       # Server info + about + logout button
            SettingsViewModel.kt    # Settings state + logout handler
```

## 3. Architecture

### 3.1 Data Flow (MVVM + Repository)

```
MainActivity
  └─> ServerConfig (SharedPreferences persistence: servers + token)
       └─> RetrofitClient (singleton, X-Token auth + CookieJar)
            └─> SillyGirlApi (Retrofit interface, all REST endpoints)
                 └─> Repository layer (Result<T> wrapping)
                      └─> ViewModel (StateFlow<T>)
                           └─> Screen (@Composable, collectAsStateWithLifecycle)
```

### 3.2 Navigation Flow

```
App 启动
  │
  ├── 无服务器 → ServerListScreen（添加/选择服务器）
  │
  ├── 有服务器 + 有 Token → 验证 Token
  │       │
  │       ├── Token 有效 → DashboardScreen（主页面）✅ 自动登录
  │       │
  │       └── Token 无效 → 自动用保存的凭证登录
  │               │
  │               ├── 登录成功 → DashboardScreen ✅ 自动登录
  │               └── 登录失败 → ServerListScreen
  │
  ├── 有服务器 + 无 Token → 自动用保存的凭证登录
  │       │
  │       ├── 登录成功 → DashboardScreen ✅ 自动登录
  │       └── 登录失败 → ServerListScreen
  │
  └── DashboardScreen（主页面）
                     │
                     ├── 💰 分佣概览卡片 → FenyongScreen
                     ├── 🧩 插件市场 → PluginMarketScreen
                     ├── 👥 管理员 → MastersScreen
                     ├── 💾 存储 → StorageScreen
                     ├── 🔧 服务 → ServiceScreen
                     ├── ⏰ 定时任务 → TasksScreen
                     └── ⚙️ 设置 → SettingsScreen（退出登录）
```

**关键改进**：用户只需输入一次账号密码，后续启动自动登录，无需手动输入。

All transitions use `AnimatedContentTransitionScope.SlideDirection` with 250ms tween animation.

**Routes (12 total)**:
- `server_list` — Server list (starting point when no server configured)
- `login` — Authentication (manual login, backup)
- `auto_login?url={url}&username={username}&password={password}` — Auto login after server selection
- `dashboard` — Home page after login
- `fenyong` — Commission system (orders + statistics)
- `my_plugins` — Installed plugins
- `plugin_market` — Plugin marketplace
- `masters` — Admin management
- `tasks` — Cron tasks
- `service` — Service management
- `storage` — KV storage viewer/editor
- `settings` — Settings (logout)

### 3.3 Authentication Flow

**自动登录流程（新增）：**
1. App 启动，从 `ServerConfig` 读取保存的服务器信息
2. `RetrofitClient.setServer(server.url)` 恢复服务器地址
3. 检查是否有保存的 Token
   - 有 Token → 验证 Token 有效性
   - 无 Token → 用保存的用户名密码自动登录
4. Token 有效/登录成功 → 进入 Dashboard
5. Token 无效/登录失败 → 跳转 ServerListScreen

**手动登录流程（备用）：**
1. User selects server from `ServerListScreen`
2. `RetrofitClient.setServer(server.url)` + load saved token
3. Navigate to `LoginScreen`
4. Login calls `POST /api/login/account` with username/password
5. Checks `status == "ok"` AND `currentAuthority == "admin"`
6. Saves token to `ServerConfig.saveToken()` + `RetrofitClient.token`
7. `AppNavGraph` loads `CurrentUser` to cache `UserData`
8. All subsequent requests get `X-Token: <token>` header via OkHttp interceptor

**关键改进**：用户只需输入一次账号密码，后续启动自动登录，无需手动输入。

### 3.4 Server Persistence (ServerConfig)

`ServerConfig` stores servers in SharedPreferences (`sillygirl_servers`):

```
KEYS:
  "servers"       → "url|user|pass|alias;;url2|user2|pass2|alias2"
  "default_index" → Int (selected server index)
  "token"         → String (auth token)

ENCODING:
  "|" → "||", ";" → ";;" (custom escaping to avoid delimiter conflicts)
```

- Supports add/update/remove/setDefault/getDefault
- Token cleared on logout
- Test server auto-added when no servers exist (see Issues section)

### 3.5 Theme System (Theme.kt)

**Brand colors**:
- **Primary gradient**: `#667EEA` → `#764BA2` (purple)
- **Secondary gradient**: `#5CC3FF` → `#4FACFE` (blue)
- **Semantic**: `Success=#22C55E`, `Warning=#F59E0B`, `Danger=#EF4444`

**Light mode**:
- Surface: `#F8F9FC`, Card: `#FFFFFF`, Background: `#F8F9FC`
- Primary: `#371E73`, OnSurface: `#1A1A2E`

**Dark mode**:
- Surface: `#121218`, Card: `#1E1E2E`, Background: `#121218`
- Primary: `#8067B2`, OnSurface: `#E0E0E0`

**Typography**: bold on display/headline/title variants, normal on body, medium on labels.

**Platform colors** (defined in `FenyongScreen.kt`):
- JD (京东): `#E60012` (red)
- TB (淘宝): `#FF5000` (orange)
- PDD (拼多多): `#E02A24` (red-purple)

**Status colors** (defined in `FenyongScreen.kt`):
- 结算: `SuccessColor` (#22C55E)
- 退款/失效: `DangerColor` (#EF4444)
- 待/审核: `WarningColor` (#F59E0B)
- 空/其他: `Primary` / `GrayText`

### 3.6 Image Loading (Coil 3)

Global image loading via Coil 3 with OkHttp network fetcher:

```kotlin
// SillyGirlApp.kt — implements SingletonImageLoader.Factory
class SillyGirlApp : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: Context): ImageLoader {
        // OkHttp with JD Referer + User-Agent interceptor
        // 50MB disk cache, crossfade enabled
    }
}
```

- `AsyncImage` used for: plugin icons (URL detection), task icons (`@icon`), order images
- Fallback: platform-colored placeholder with first character (JD=红, TB=橙, PDD=紫)
- Network interceptor adds JD `Referer` + `User-Agent` for anti-hotlink protection

### 3.7 CompositionLocal

```kotlin
val LocalServerConfig = staticCompositionLocalOf<ServerConfig> { error("...") }
```

Provided in `MainActivity.setContent {}`, consumed in `LoginScreen`, `SettingsScreen`, `AppNavGraph`.

## 4. Screen Details

### 4.1 DashboardScreen + DashboardViewModel

**UI State**: `DashboardUiState(isLoading, error, userName, avatar, installedPlugins, masterCount, activeTaskCount, fenyongDashboard)`

**Layout**:
1. `WelcomeHeader` — gradient card with user name
2. `FenyongOverviewCard` — today/7天/30天 commission stats + platform breakdown
3. `MetricGridCard` row (3 items) — **clickable**:
   - Plugins count → MyPluginsScreen
   - Masters count → MastersScreen
   - Active tasks → TasksScreen
4. `FeatureGrid` (3x2 grid) — 插件市场, 管理员, 存储, 服务, 定时任务
5. Error banner (if error)

**Data loading**: Parallel fetch from `MasterRepository`, `TaskRepository`, `FenyongRepository`.

### 4.2 LoginScreen + LoginViewModel + AutoLoginScreen

**UI State**: `LoginUiState(serverUrl, username, password, isLoading, error, isLoggedIn)`

**AutoLoginScreen（自动登录页面，新增）**：
- 选择服务器后自动跳转
- 显示加载动画和服务器地址
- 登录成功 → Dashboard
- 登录失败 → 返回服务器列表
- 使用保存的用户名密码自动登录

**LoginScreen（手动登录页面，备用）**：
- Validates all fields non-blank before submit
- On success: saves credentials to `ServerConfig`, sets `isLoggedIn=true`
- Uses `LoginViewModelFactory` to inject `ServerConfig`

**Layout**:
- Gradient glow circle (150dp) + icon card (72dp) centered in `Box`
- Title "SillyGirl" + subtitle
- 3 input fields: server URL, username, password (with visibility toggle)
- Error banner (if error)
- Gradient login button (PrimaryGradientColors: #667EEA → #764BA2)

### 4.3 ServerListScreen + ServerListViewModel

**UI State**: `ServerListUiState(servers, defaultIndex, isLoading, error)`

**Features**:
- Add server dialog (url, alias, username, password with visibility toggle)
- Delete confirmation dialog
- Set default server (checkmark icon)
- Empty state with icon + instructions
- Uses `ServerListViewModelFactory` to inject `ServerConfig`

### 4.4 FenyongScreen + FenyongViewModel

**UI State**: `FenyongUiState(isLoading, error, keyword, orders, page, total, filterActualGtZero)`

**Features**:
- Order list with pagination (20 per page)
- `OrderItemCard`: product image + title + status badge + time + 金额/预估/实际
- Status badge with semantic colors (结算=绿, 退款/失效=红, 待/审核=黄)
- Time format: `yyyy-MM-dd HH:mm:ss`
- `PaginationWidget`: prev/next with page indicator, **fixed at bottom** (not scrollable)
- **Quick filter**: FilterChip for "实际佣金 > 0" with count display
- Image preloading on order load via `ImageCache` singleton

**Layout Structure**:
```
Column {
    // Filter chip row (actual commission > 0)
    Row { FilterChip + count }

    // Content (when not loading/error)
    Column(fillMaxSize) {
        LazyColumn(weight(1f)) {
            // Order cards (scrollable)
            items(displayedOrders) { OrderItemCard }
        }

        // Fixed at bottom
        HorizontalDivider
        PaginationWidget (if total > 20)
    }
}
```

### 4.5 PluginScreens + PluginViewModels

**Three screens**:
- `MyPluginsScreen` (`MyPluginsViewModel`) — installed plugins with search/filter
- `PluginMarketScreen` (`PluginMarketViewModel`) — available plugins with install button
- `PluginDetailScreen` (`MyPluginsViewModel`) — plugin detail with editor, debug/disable toggle, uninstall

**UI States**:
- `MyPluginsUiState(isLoading, error, plugins, snackbarMessage)`
- `PluginDetailUiState(isLoading, error, content, isSaving, isToggling, snackbarMessage)`

**Features**:
- MyPluginsScreen:
  - Search bar (name/description/author fuzzy search)
  - Category filter chips (auto-collected from plugin classes)
  - Running status green dot indicator
  - Status badges: 已禁用(red), 调试(purple), 配置(blue)
  - Plugin count + running count header
  - Empty search result hint
- PluginDetailScreen:
  - Plugin info card (title, description, version, author, origin tag, classes)
  - Running status indicator (green/gray/red)
  - Debug mode toggle with loading spinner
  - Disable mode toggle (red track) with loading spinner
  - Code editor with save functionality
  - Reload plugin button
  - Uninstall with confirmation dialog
  - Toggle helper text descriptions

**API Usage** (uses existing storage API):
- Get plugin content: `GET /api/storage?keys=plugins.{uuid}`
- Update plugin content: `PUT /api/storage?uuid={pluginId}` with body `{plugins.{uuid}: content}`
- Reload plugin: `PUT /api/storage?uuid={pluginId}` with body `{plugins.{uuid}: reload}`
- Toggle debug: `PUT /api/storage?uuid={pluginId}` with body `{plugin_debug.{uuid}: true/false}`

### 4.6 MastersScreen + MastersViewModel (inline)

**UI State**: `MastersUiState(isLoading, error, masters, platforms, snackbarMessage)`

**Features**: List + add dialog (platform + number) + delete with snackbar feedback

### 4.7 TasksScreen + TasksViewModel (inline)

**UI State**: `TasksUiState(isLoading, error, tasks, snackbarMessage)`

**Features**: Task list with toggle (enable/disable), run, delete buttons

### 4.8 StorageScreen + StorageViewModel (inline)

**UI State**: `StorageUiState(isLoading, error, keys, selectedValue, selectedKey, snackbarMessage)`

**Features**: Key search + value display/edit + save + clear

### 4.9 ServiceScreen

**Status**: Fully implemented

**Features**:
- Server list with current server indicator (green "当前" badge)
- Add server dialog (URL, alias, username, password with visibility toggle)
- Switch server（自动登录）：
  - 确认对话框 → 点击切换
  - 显示加载动画"正在登录..."
  - 登录成功 → 切换服务器并进入 Dashboard
  - 登录失败 → 显示错误提示
- Delete server (confirmation dialog)
- Empty state with icon + instructions

### 4.10 SettingsScreen + SettingsViewModel

**UI State**: `SettingsUiState(serverUrl, isLoggingOut, logoutDone)`

**Features**: Server info display + about section + logout button with confirmation dialog

## 5. API Endpoints (22 total)

### Auth
| Method | Path | Response Model |
|---|---|---|
| POST | `/api/login/account` | `LoginResponse` (status, currentAuthority, token) |
| GET | `/api/currentUser` | `CurrentUserResponse` → `UserData` |
| POST | `/api/login/outLogin` | `Any` |

### Plugins
| Method | Path | Query/Body |
|---|---|---|
| GET | `/api/plugins/list.json` | `current`, `pageSize`, `activeKey` (tab1=installed, tab2=available) |
| POST | `/api/plugins/run` | `{ name: string }` |
| POST | `/api/plugins/stop` | `{ name: string }` |
| POST | `/api/plugins/install` | `{ name: string }` |
| POST | `/api/plugins/uninstall` | `{ name: string }` |

### Commission (分佣)
| Method | Path | Response Model |
|---|---|---|
| GET | `/api/fenyong/dashboard` | `FenyongDashboardResponse` (today/yesterday/7days/month + platforms) |
| GET | `/api/fenyong/orders` | `FenyongOrderResponse` (orders[], tongji, tabs, page, total) |

### Masters (管理员)
| Method | Path |
|---|---|
| GET | `/api/master/list` |
| POST | `/api/master/add` |
| POST | `/api/master/del` |

### Tasks (定时任务)
| Method | Path |
|---|---|
| GET | `/api/tasks` |
| POST | `/api/tasks/add` |
| POST | `/api/tasks/edit` |
| POST | `/api/tasks/del` |
| POST | `/api/tasks/setEnable` |
| POST | `/api/tasks/run` |

### Storage
| Method | Path |
|---|---|
| PUT | `/api/storage?uuid=xxx` |
| GET | `/api/storage?keys=xxx` |

## 6. Key Data Models (Models.kt)

### Generic
```kotlin
ApiResponse<T>        { success, data, errorMessage, errorCode, status }
LoginResponse         { status, currentAuthority, token }
CurrentUserResponse   { success, data: UserData }
```

### UserData + PluginRoute
```kotlin
UserData     { name, avatar, plugins: List<PluginRoute> }
PluginRoute  { path, name, component, createAt, title, description, icon, origin, version, author, running, disable, debug, hasForm, classes }
```

### PluginInfo
```kotlin
PluginInfo {
  id, title, description, version, author
  running: Boolean, disable: Boolean
  classes: List<String>, downloads: Int
  icon: String, debug: Boolean
}
```

### Commission (分佣)
```kotlin
FenyongDashboardResponse {
  success, today/yesterday/last7days/lastMonth: FenyongPeriodStats
  platforms: Map<String, FenyongPlatformStats>
  totalSettled, totalUnsettled, totalOrders
}

FenyongPeriodStats     { orders, estimate, actual }
FenyongPlatformStats   { orders, estimate, actual }
FenyongOrderResponse   { success, data: List<FenyongOrder>, tongji, tabs, page, total }
FenyongOrder           { name, image, skuName, status, createdTime, site, skuId, orderId, estimate, actual, content, bind }
FenyongOrderContent    { label, value, status }
FenyongBind            { platform, userId }
FenyongTab             { key, title, value }
```

### Time Range
```kotlin
FenyongTimeRange {
  ALL=0, TODAY=1, YESTERDAY=2, LAST_7D=7, LAST_MONTH=30,
  LAST_MONTH_2=60, LAST_YEAR=365, LAST_YEAR_2=730, CUSTOM=-1
  options: List<TimeRangeOption>
}
```

### Masters + Tasks
```kotlin
MastersResponse   { success, data: List<MasterInfo>, platforms: List<PlatformOption> }
MasterInfo        { id, platform, nickname, number, unix }
PlatformOption    { label, value }
TaskResponse      { success, data: List<TaskInfo> }
TaskInfo          { id, taskId, title, schedule, command, enable, createdAt, remark }
```

### Unused Models
```kotlin
FenyongTongjiResponse { success, data: FenyongTongjiData }  // Defined but no API endpoint
FenyongTongjiData     { 12 metrics + results: List<FenyongUserItem> }  // Unused
FenyongUserItem       { label, value, count }  // Unused
```

## 7. Dependencies

| Package | Version | Purpose |
|---|---|---|
| `androidx.compose:compose-bom` | 2024.09.00 | Compose BOM (BOM-managed versions) |
| `androidx.compose.ui:ui` | BOM | Compose UI core |
| `androidx.compose.ui:ui-graphics` | BOM | Graphics utilities |
| `androidx.compose.ui:ui-tooling-preview` | BOM | Preview support |
| `androidx.compose.material3:material3` | BOM | Material 3 components |
| `androidx.compose.material:material-icons-extended` | BOM | Extended icon set |
| `androidx.compose.foundation:foundation` | BOM | Layout, gestures, list |
| `androidx.activity:activity-compose` | 1.10.1 | Activity + Compose integration |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.9.0 | collectAsStateWithLifecycle |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.9.0 | viewModel() in Compose |
| `androidx.lifecycle:lifecycle-viewmodel` | 2.9.0 | ViewModel base |
| `androidx.navigation:navigation-compose` | 2.9.0 | NavHost + composable routes |
| `com.squareup.retrofit2:retrofit` | 2.11.0 | HTTP client |
| `com.squareup.retrofit2:converter-gson` | 2.11.0 | Gson JSON serialization |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP engine |
| `com.squareup.okhttp3:logging-interceptor` | 4.12.0 | Request/Response logging (BASIC) |
| `com.squareup.okhttp3:okhttp-urlconnection` | 4.12.0 | URL connection bridge |
| `com.google.code.gson:gson` | 2.12.1 | JSON parsing (lenient) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 | Coroutines |
| `io.coil-kt.coil3:coil-compose` | 3.0.4 | Image loading (plugin icons, task icons, order images) |
| `io.coil-kt.coil3:coil-network-okhttp` | 3.0.4 | OkHttp network fetcher for Coil (JD Referer support) |

## 8. UI Component Library (AppComponents.kt)

| Component | Props | Purpose |
|---|---|---|
| `GradientCard` | colors, modifier, content | Gradient background card (vertical, RoundedCornerShape 20dp) |
| `GlassCard` | modifier, onClick?, content | White surface card with shadow (RoundedCornerShape 16dp) |
| `StatNumberCard` | icon, iconColor, value, label, onClick | Stat display with icon circle (40dp) |
| `ListItemCard` | leading?, title, subtitle?, trailing?, onClick | Row list item with GlassCard |
| `SiteChip` | site, color | Assisted chip for platform tags |
| `MoneyText` | value | Formatted currency (¥X.XX / ¥X.XX万) |
| `BigMoneyText` | value, color | Large formatted currency (displaySmall) |
| `MiniAppBar` | title, navigationIcon?, actions | Shared 40dp top bar with Surface + Row layout |
| `ListItemColors` | titleColor, subtitleColor? | Color customization for ListItemCard |

## 9. CI/CD

**GitHub Actions** (`.github/workflows/build-apk.yml`):
- Trigger: push to `main` or `workflow_dispatch`
- Steps: Checkout → JDK 21 (Temurin) + Gradle cache → Android SDK → `assembleDebug` → upload artifact → create pre-release with APK
- Output: `app-debug.apk` attached to `apk-{sha}` pre-release

## 10. Build Configuration

### gradle.properties
```properties
android.useAndroidX=true
kotlin.code.style=official
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.java.home=/opt/homebrew/opt/openjdk@21
android.sdk.path=/opt/homebrew/share/android-commandlinetools
```

### AndroidManifest.xml
- Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`
- `usesCleartextTraffic="true"` (allows HTTP connections)
- Single activity: `MainActivity`
- Theme: `Theme.SillyGirlClient` (Material Light NoActionBar)

### build.gradle.kts (app)
- `isMinifyEnabled = false` (no ProGuard/R8 shrinking)
- Compose compiler plugin enabled
- Java/Kotlin target: 21

## 11. Important Paths & Conventions

- **Package**: `com.sillygirl.client`
- **Nav**: `navigation.compose.NavHost` with slide transitions (250ms tween)
- **State**: All screens use `ViewModel` + `StateFlow` + `collectAsStateWithLifecycle`
- **API client**: Singleton pattern (`RetrofitClient.api`), lazy-initialized, invalidated on server change
- **Auth**: `X-Token` header + `CookieJar` for session persistence
- **Logging**: `HttpLoggingInterceptor.Level.BASIC` in all builds
- **Gson**: Lenient mode, handles malformed JSON gracefully
- **ViewModel factories**: `LoginViewModelFactory`, `ServerListViewModelFactory`, `SettingsViewModelFactory` inject `ServerConfig`
- **Inline ViewModels**: `MastersViewModel`, `TasksViewModel`, `StorageViewModel` defined directly in Screen files

## 12. Plugin Management

See [PLUGIN_MANAGEMENT.md](PLUGIN_MANAGEMENT.md) for detailed design documentation.

**Key Features**:
- MyPluginsScreen: List of user's plugins from `currentUser.plugins`
- PluginDetailScreen: Plugin info, debug toggle, code editor
- Uses existing storage API (no new endpoints needed)
- Navigation: Dashboard → MyPlugins → PluginDetail

**API Usage**:
- Get plugin content: `GET /api/storage?keys=plugins.{uuid}`
- Update plugin content: `PUT /api/storage?uuid={pluginId}` with body `{plugins.{uuid}: content}`
- Reload plugin: `PUT /api/storage?uuid={pluginId}` with body `{plugins.{uuid}: reload}`
- Toggle debug: `PUT /api/storage?uuid={pluginId}` with body `{plugin_debug.{uuid}: true/false}`

## 13. Known Issues & Technical Debt

### 🔴 High Priority
| # | Issue | Location |
|---|---|---|
| ~~1~~ | ~~**Test server hardcoded** with plaintext credentials~~ | ~~`MainActivity.kt:37-43`, `LoginScreen.kt:32-34`~~ |

### 🟡 Medium Priority
| # | Issue | Location |
|---|---|---|
| ~~2~~ | ~~**`AuthRepository.logout()` uses `runBlocking`** — blocks main thread from Compose~~ | ~~`AuthRepository.kt:54`~~ |
| ~~3~~ | ~~**POST bodies use `Map<String, String>`** — not type-safe, should use data classes~~ | ~~`SillyGirlApi.kt`~~ |
| 4 | **No unit tests** — no `src/test/` or `src/androidTest/` directories | — |
| 5 | **`isMinifyEnabled = false`** — release build not shrunk | `build.gradle.kts:21` |
| 6 | **`usesCleartextTraffic="true"`** — allows HTTP, should be HTTPS-only in production | `AndroidManifest.xml:13` |

### 🟢 Low Priority
| # | Issue | Location |
|---|---|---|
| 7 | **`FenyongTongjiResponse`/`FenyongTongjiData` models exist but are unused** — no API endpoint calls them | `Models.kt` |

### ✅ Recently Fixed
| # | Issue | Fix |
|---|---|---|
| 8 | ~~MiniAppBar duplicated in 9 files~~ | Extracted to shared `AppComponents.kt` MiniAppBar |
| 9 | ~~ImageCache bypasses Coil~~ | Migrated to Coil 3 global loader, deleted custom ImageCache |
| 10 | ~~ServiceScreen placeholder~~ | Full implementation: add/switch/delete servers |
| 11 | ~~Plugin "非法操作" bug~~ | `asPluginId()` strips `/script/` prefix before API call |
| 12 | ~~Plugin icon URL not displayed~~ | Coil 3 AsyncImage with `isIconUrl()` detection |
| 13 | ~~Task list icon not showing~~ | Coil AsyncImage for `task.icons.firstOrNull()?.link` |
| 14 | ~~TaskItemCard/MasterCard layout overflow~~ | `Column(weight(1f))` instead of `fillMaxWidth()` |
| 15 | ~~No session expiry handling~~ | OkHttp 401/403 interceptor + DisposableEffect auto-redirect |
| 16 | ~~Duplicate dashboard entries~~ | Removed 管理员/定时任务 from FeatureGrid |
| 17 | ~~menuAnchor() deprecation~~ | Changed to `MenuAnchorType.PrimaryNotEditable` |
| 18 | ~~Test server hardcoded~~ | Removed test auto-fill, credentials now user-provided |
| 19 | ~~Token lost on app restart~~ | Added `RetrofitClient.setServer()` call on startup before token verification |
| 20 | ~~Manual login required every time~~ | Auto-login with saved credentials after server selection |
| 21 | ~~`runBlocking` blocks main thread~~ | Changed `logout()` to suspend function |
| 22 | ~~POST bodies not type-safe~~ | Created 8 request data classes for all POST endpoints |
| 23 | ~~Auto-select server on startup~~ | Modified launch flow to let user choose server when no valid token |
