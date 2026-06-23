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
import com.sillygirl.client.LocalServerConfig
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.UserData
import com.sillygirl.client.data.repository.AuthRepository
import com.sillygirl.client.data.repository.ServerConfig
import com.sillygirl.client.ui.screens.dashboard.DashboardScreen
import com.sillygirl.client.ui.screens.fenyong.FenyongScreen
import com.sillygirl.client.ui.screens.login.LoginScreen
import com.sillygirl.client.ui.screens.settings.SettingsScreen
import com.sillygirl.client.ui.screens.plugins.MyPluginsScreen
import com.sillygirl.client.ui.screens.plugins.PluginMarketScreen
import com.sillygirl.client.ui.screens.plugins.PluginDetailScreen
import com.sillygirl.client.ui.screens.masters.MastersScreen
import com.sillygirl.client.ui.screens.tasks.TasksScreen
import com.sillygirl.client.ui.screens.service.ServiceScreen
import com.sillygirl.client.ui.screens.storage.StorageScreen
import com.sillygirl.client.ui.screens.serverlist.ServerListScreen
import com.sillygirl.client.ui.screens.serverlist.ServerListViewModel
import com.sillygirl.client.ui.screens.serverlist.ServerListViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // 注册会话过期回调 — 任何 API 返回 401 时触发
    DisposableEffect(Unit) {
        RetrofitClient.onSessionExpired = {
            // 在主线程处理导航（通过 coroutine）
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                currentUser = null
                currentUserLoaded = false
                isLoggedIn = false
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
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

        // 有服务器：尝试用保存的 token 验证
        val savedToken = serverConfig.getToken()
        if (savedToken == null) {
            // 无 token，直接去登录
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
            onFailure = {
                // token 过期，清除
                serverConfig.clearToken()
                RetrofitClient.token = null
                isLoggedIn = false
                isVerifying = false
            }
        )
    }

    // 加载用户信息并缓存
    LaunchedEffect(isLoggedIn, currentUserLoaded) {
        if (isLoggedIn && !currentUserLoaded) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    authRepo.getCurrentUserInfo()
                }.fold(
                    onSuccess = { currentUser = it },
                    onFailure = {
                        // 用户信息加载失败，可能 token 已过期
                        serverConfig.clearToken()
                        RetrofitClient.token = null
                        currentUser = null
                        currentUserLoaded = false
                        isLoggedIn = false
                    }
                )
            }
            currentUserLoaded = true
        }
    }

    val startRoute = when {
        !hasServer -> Routes.SERVER_LIST
        !isLoggedIn -> Routes.LOGIN
        else -> Routes.DASHBOARD
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        // 启动验证中：显示加载动画
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
                        RetrofitClient.setServer(server.url)
                        val savedToken = serverConfig.getToken()
                        if (savedToken != null) {
                            RetrofitClient.token = savedToken
                            serverConfig.saveToken(savedToken)
                        }
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SERVER_LIST) { inclusive = true }
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

            composable(Routes.DASHBOARD) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column {
                        MiniAppBar(
                            title = { Text("SillyGirl") },
                            actions = {
                                IconButton(onClick = {  /*reload*/  }) {
                                    Icon(Icons.Filled.Refresh, "刷新")
                                }
                                IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                                    Icon(Icons.Filled.Settings, "设置")
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
                    plugins = currentUser?.plugins ?: emptyList(),
                    onBack = { navController.popBackStack() },
                    onPluginClick = { plugin ->
                        // 导航到插件详情页，使用 UUID
                        val uuid = plugin.path.removePrefix("/script/")
                        navController.navigate("plugin_detail/$uuid")
                    },
                )
            }
            composable(Routes.PLUGIN_MARKET) {
                PluginMarketScreen(onBack = { navController.popBackStack() })
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
                        // 切换服务器：重置所有状态，跳转登录
                        RetrofitClient.reset()
                        currentUser = null
                        currentUserLoaded = false
                        isLoggedIn = false
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    serverConfig = serverConfig,
                )
            }

            composable(Routes.STORAGE) {
                StorageScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun MiniAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(Modifier.width(8.dp))
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                title()
            }
            actions()
        }
    }
}
