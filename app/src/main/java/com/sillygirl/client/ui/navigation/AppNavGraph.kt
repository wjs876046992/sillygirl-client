package com.sillygirl.client.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.sillygirl.client.LocalServerConfig
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.UserData
import com.sillygirl.client.data.repository.AuthRepository
import com.sillygirl.client.data.repository.ServerConfig
import com.sillygirl.client.ui.screens.dashboard.DashboardScreen
import com.sillygirl.client.ui.screens.fenyong.FenyongScreen
import com.sillygirl.client.ui.screens.login.LoginScreen
import com.sillygirl.client.ui.screens.login.AutoLoginScreen
import com.sillygirl.client.ui.screens.settings.SettingsScreen
import com.sillygirl.client.ui.screens.plugins.MyPluginsScreen
import com.sillygirl.client.ui.screens.plugins.PluginMarketScreen
import com.sillygirl.client.ui.screens.plugins.PluginDetailScreen
import com.sillygirl.client.ui.screens.masters.MastersScreen
import com.sillygirl.client.ui.screens.tasks.TasksScreen
import com.sillygirl.client.ui.screens.service.ServiceScreen
import com.sillygirl.client.ui.screens.storage.StorageScreen
import com.sillygirl.client.ui.screens.chat.ChatScreen
import com.sillygirl.client.ui.screens.serverlist.ServerListScreen
import com.sillygirl.client.ui.screens.serverlist.ServerListViewModel
import com.sillygirl.client.ui.screens.serverlist.ServerListViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sillygirl.client.ui.components.MiniAppBar
import com.sillygirl.client.ui.components.BrandTopBar
import androidx.compose.ui.graphics.Color

object Routes {
    const val SERVER_LIST = "server_list"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val FENYONG = "fenyong"
    const val SETTINGS = "settings"
    const val MY_PLUGINS = "my_plugins"
    const val PLUGIN_MARKET = "plugin_market"
    const val PLUGIN_DETAIL = "plugin_detail/{pluginUuid}"
    const val MASTERS = "masters"
    const val TASKS = "tasks"
    const val SERVICE = "service"
    const val STORAGE = "storage"
    const val CHAT = "chat"
    const val AUTO_LOGIN = "auto_login" // 自动登录加载页
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val serverConfig = LocalServerConfig.current
    val authRepo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    val defaultServer = serverConfig.getDefaultServer()
    var hasServer by remember { mutableStateOf(defaultServer != null) }
    var isVerifying by remember { mutableStateOf(true) } // 启动验证中
    var isLoggedIn by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<UserData?>(null) }
    var currentUserLoaded by remember { mutableStateOf(false) }

    // 刷新 currentUser 的回调（供插件安装/卸载/重载后更新首页）
    val refreshCurrentUser: () -> Unit = {
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getCurrentUser()
                }
                if (response.success && response.data != null) {
                    currentUser = response.data
                }
            } catch (_: Exception) {}
        }
    }

    // 注册会话过期回调 — 任何 API 返回 401 时触发
    DisposableEffect(Unit) {
        RetrofitClient.onSessionExpired = {
            // 在主线程处理状态重置
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                currentUser = null
                currentUserLoaded = false
                isLoggedIn = false
                // 同时清除本地保存的 token，防止下次启动仍使用过期 token
                serverConfig.clearToken()
            }
        }
        onDispose {
            RetrofitClient.onSessionExpired = null
        }
    }

    // 启动时验证 token
    LaunchedEffect(defaultServer) {
        hasServer = defaultServer != null
        if (!hasServer) {
            isVerifying = false
            return@LaunchedEffect
        }

        try {
            // 有服务器：先设置服务器地址
            val server = defaultServer!!

            // 检查服务器 URL 是否有效
            if (server.url.isBlank()) {
                android.util.Log.e("AppNavGraph", "Server URL is blank, clearing server config")
                isVerifying = false
                hasServer = false
                return@LaunchedEffect
            }

            RetrofitClient.setServer(server.url)

            // 如果服务器无需认证，直接进入
            if (!server.requiresAuth) {
                RetrofitClient.token = null
                isLoggedIn = true
                isVerifying = false
                return@LaunchedEffect
            }

            // 尝试用保存的 token 验证
            val savedToken = serverConfig.getToken()
            if (savedToken == null) {
                // 无 token，跳转到服务器列表让用户选择
                isLoggedIn = false
                isVerifying = false
                return@LaunchedEffect
            }

            // 有 token，尝试验证
            RetrofitClient.token = savedToken
            withContext(Dispatchers.IO) {
                authRepo.verifySession()
            }.fold(
                onSuccess = {
                    isLoggedIn = true
                    isVerifying = false
                },
                onFailure = { e ->
                    // 仅在认证失败(401/403)时清除 token，网络错误等瞬时问题保留 token
                    val isAuthFailure = e is retrofit2.HttpException &&
                        (e.code() == 401 || e.code() == 403)
                    if (isAuthFailure) {
                        android.util.Log.w("AppNavGraph", "Token expired (HTTP ${e.code()}), clearing")
                        serverConfig.clearToken()
                        RetrofitClient.token = null
                        isLoggedIn = false
                    } else {
                        // 网络错误/超时等瞬时问题：保留 token，回到 SERVER_LIST 让用户重试
                        // 不进入 Dashboard（否则 token 无效时任何 API 调用都会触发 401 → 强制退出）
                        android.util.Log.w("AppNavGraph", "Verify failed (${e::class.simpleName}: ${e.message}), keeping token")
                        isLoggedIn = false
                    }
                    isVerifying = false
                }
            )
        } catch (e: Exception) {
            // 捕获任何未预期的异常，防止闪退
            android.util.Log.e("AppNavGraph", "Startup verification failed", e)
            val isAuthFailure = e is retrofit2.HttpException && (e.code() == 401 || e.code() == 403)
            if (isAuthFailure) {
                serverConfig.clearToken()
                RetrofitClient.token = null
            }
            // 无论何种错误都回到 SERVER_LIST，保留 token 让用户可重试
            isLoggedIn = false
            isVerifying = false
        }
    }

    // 加载用户信息并缓存
    LaunchedEffect(isLoggedIn, currentUserLoaded) {
        if (isLoggedIn && !currentUserLoaded) {
            // 先设置为true防止重复调用
            currentUserLoaded = true
            scope.launch {
                try {
                    // 先获取原始响应以便调试
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.api.getCurrentUser()
                    }
                    android.util.Log.d("AppNavGraph", "Raw response: success=${response.success}, data=${response.data}")
                    response.data?.let {
                        android.util.Log.d("AppNavGraph", "User data: name='${it.name}', avatar='${it.avatar}', plugins=${it.plugins.size}")
                        android.util.Log.d("AppNavGraph", "First 3 plugins: ${it.plugins.take(3).map { p -> p.title }}")
                    } ?: run {
                        android.util.Log.e("AppNavGraph", "User data is null!")
                    }

                    if (response.success && response.data != null) {
                        currentUser = response.data
                    } else {
                        // API 返回 success=false — 可能 token 过期或服务端问题
                        // 不清除 token，不自动重试（避免无限循环）
                        // 用户可通过 Dashboard 下拉刷新或点击刷新按钮重试
                        android.util.Log.e("AppNavGraph", "Failed to load user info: success=${response.success}")
                        currentUser = null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppNavGraph", "Exception loading user info (${e::class.simpleName}: ${e.message})")
                    // 网络错误等瞬时问题：保留 token，不退出登录
                    // 用户可通过 Dashboard 下拉刷新重试
                }
            }
        }
    }

    val startRoute = when {
        !hasServer -> Routes.SERVER_LIST
        !isLoggedIn -> Routes.SERVER_LIST  // 没有登录成功，跳转到服务器列表（支持自动登录）
        else -> Routes.DASHBOARD
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        // 启动验证中：显示加载动画，不创建 NavHost
        if (isVerifying && hasServer) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("正在验证登录状态...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }

        // 验证完成后创建 NavHost（startDestination 此时已是最终值）
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding),
            enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(250)) },
            exitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(250)) },
            popEnterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(250)) },
            popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(250)) },
        ) {
            composable(Routes.SERVER_LIST) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel<ServerListViewModel>(
                    factory = ServerListViewModelFactory(serverConfig)
                )
                ServerListScreen(
                    viewModel = vm,
                    onServerSelected = { server: ServerConfig.ServerInfo ->
                        if (!server.requiresAuth) {
                            // 无需认证，直接设置服务器并进入主页面
                            RetrofitClient.setServer(server.url)
                            RetrofitClient.token = null
                            isLoggedIn = true
                            currentUserLoaded = false
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.SERVER_LIST) { inclusive = true }
                            }
                        } else {
                            // 需要认证，进入自动登录流程
                            navController.navigate("auto_login?url=${server.url}&username=${server.username}&password=${server.password}") {
                                popUpTo(Routes.SERVER_LIST) { inclusive = true }
                            }
                        }
                    },
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        isLoggedIn = true
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            // 自动登录页面 - 选择服务器后自动用保存的凭证登录
            composable(
                route = "auto_login?url={url}&username={username}&password={password}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("username") { type = NavType.StringType },
                    navArgument("password") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                val username = backStackEntry.arguments?.getString("username") ?: ""
                val password = backStackEntry.arguments?.getString("password") ?: ""

                AutoLoginScreen(
                    serverUrl = url,
                    username = username,
                    password = password,
                    onLoginSuccess = {
                        isLoggedIn = true
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onLoginFailed = {
                        // 登录失败，返回服务器列表
                        navController.navigate(Routes.SERVER_LIST) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.DASHBOARD) {
                var dashboardRefresh by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
                val refreshDashboard = remember {
                    {
                        // 刷新 currentUser
                        scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.api.getCurrentUser()
                                }
                                if (response.success && response.data != null) {
                                    currentUser = response.data
                                }
                            } catch (_: Exception) {}
                        }
                        // 刷新 dashboard 数据（带刷新提示）
                        dashboardRefresh?.invoke(true)
                    }
                }
                val serverDisplayUrl = remember {
                    val server = serverConfig.getDefaultServer()
                    // 只显示备注(alias)，无备注则不显示，避免暴露服务器地址
                    server?.alias?.takeIf { it.isNotBlank() } ?: ""
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column {
                        BrandTopBar(
                            title = "傻妞平台管理助手",
                            subtitle = currentUser?.name?.takeIf { it.isNotBlank() },
                            serverUrl = serverDisplayUrl,
                            actions = {
                                IconButton(onClick = { refreshDashboard() }) {
                                    Icon(Icons.Filled.Refresh, "刷新", tint = Color.White)
                                }
                                IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                                    Icon(Icons.Filled.Settings, "设置", tint = Color.White)
                                }
                            },
                        )
                        DashboardScreen(
                            currentUser = currentUser,
                            onNavigateToFenyong = { navController.navigate(Routes.FENYONG) },
                            onNavigateToMyPlugins = { navController.navigate(Routes.MY_PLUGINS) },
                            onNavigateToPluginMarket = { navController.navigate(Routes.PLUGIN_MARKET) },
                            onNavigateToMasters = { navController.navigate(Routes.MASTERS) },
                            onNavigateToTasks = { navController.navigate(Routes.TASKS) },
                            onNavigateToService = { navController.navigate(Routes.SERVICE) },
                            onNavigateToStorage = { navController.navigate(Routes.STORAGE) },
                            onNavigateToChat = { navController.navigate(Routes.CHAT) },
                            onRefreshReady = { refreshFn -> dashboardRefresh = refreshFn },
                        )
                    }
                }
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onLogout = {
                        RetrofitClient.reset()
                        serverConfig.clearToken()
                        navController.navigate(Routes.SERVER_LIST) {
                            popUpTo(0) { inclusive = true }
                        }
                        isLoggedIn = false
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.FENYONG) {
                FenyongScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.MY_PLUGINS) {
                MyPluginsScreen(
                    onBack = { navController.popBackStack() },
                    onPluginClick = { plugin ->
                        // 导航到插件详情页，使用 UUID
                        val uuid = plugin.path.removePrefix("/script/")
                        navController.navigate("plugin_detail/$uuid")
                    },
                    onRefreshCurrentUser = refreshCurrentUser,
                )
            }
            composable(Routes.PLUGIN_MARKET) {
                PluginMarketScreen(
                    onBack = { navController.popBackStack() },
                    onPluginClick = { plugin ->
                        val uuid = plugin.path.removePrefix("/script/")
                        navController.navigate("plugin_detail/$uuid")
                    },
                    onRefreshCurrentUser = refreshCurrentUser,
                )
            }
            composable(Routes.PLUGIN_DETAIL) { backStackEntry ->
                val pluginUuid = backStackEntry.arguments?.getString("pluginUuid") ?: ""
                val plugin = currentUser?.plugins?.find { it.path.removePrefix("/script/") == pluginUuid }
                if (plugin != null) {
                    PluginDetailScreen(
                        plugin = plugin,
                        onBack = { navController.popBackStack() },
                        onUninstalled = {
                            // 卸载后刷新用户信息并返回插件列表
                            currentUserLoaded = false
                            navController.popBackStack()
                        },
                    )
                }
            }

            composable(Routes.MASTERS) {
                MastersScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.TASKS) {
                TasksScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SERVICE) {
                ServiceScreen(
                    onBack = { navController.popBackStack() },
                    onServerSwitched = {
                        // 切换服务器成功（已在 ServiceScreen 中自动登录）
                        // 重置用户信息，重新加载
                        currentUser = null
                        currentUserLoaded = false
                        isLoggedIn = true
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    serverConfig = serverConfig,
                )
            }

            composable(Routes.STORAGE) {
                StorageScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.CHAT) {
                ChatScreen(onBack = { navController.popBackStack() })
            }
        }

        // 监听登出/Token过期：isLoggedIn 从 true→false 时导航回 SERVER_LIST
        var prevIsLoggedIn by remember { mutableStateOf(isLoggedIn) }
        LaunchedEffect(isLoggedIn) {
            if (prevIsLoggedIn && !isLoggedIn) {
                navController.navigate(Routes.SERVER_LIST) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            prevIsLoggedIn = isLoggedIn
        }
    }
}
