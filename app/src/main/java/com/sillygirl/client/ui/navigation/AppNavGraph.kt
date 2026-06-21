package com.sillygirl.client.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sillygirl.client.LocalServerConfig
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.repository.ServerConfig
import com.sillygirl.client.ui.screens.dashboard.DashboardScreen
import com.sillygirl.client.ui.screens.fenyong.FenyongScreen
import com.sillygirl.client.ui.screens.login.LoginScreen
import com.sillygirl.client.ui.screens.settings.SettingsScreen
import com.sillygirl.client.ui.screens.plugins.MyPluginsScreen
import com.sillygirl.client.ui.screens.plugins.PluginMarketScreen
import com.sillygirl.client.ui.screens.masters.MastersScreen
import com.sillygirl.client.ui.screens.tasks.TasksScreen
import com.sillygirl.client.ui.screens.service.ServiceScreen
import com.sillygirl.client.ui.screens.storage.StorageScreen
import com.sillygirl.client.ui.screens.serverlist.ServerListScreen
import com.sillygirl.client.ui.screens.serverlist.ServerListViewModel
import com.sillygirl.client.ui.screens.serverlist.ServerListViewModelFactory

object Routes {
    const val SERVER_LIST = "server_list"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val FENYONG = "fenyong"
    const val SETTINGS = "settings"
    const val MY_PLUGINS = "my_plugins"
    const val PLUGIN_MARKET = "plugin_market"
    const val MASTERS = "masters"
    const val TASKS = "tasks"
    const val SERVICE = "service"
    const val STORAGE = "storage"
}

// ===== 底部导航项 =====
data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
)

val mainNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, Icons.Filled.Home, "首页"),
    BottomNavItem(Routes.FENYONG, Icons.Filled.Paid, "分佣"),
    BottomNavItem(Routes.PLUGIN_MARKET, Icons.Filled.Extension, "插件"),
    BottomNavItem(Routes.MASTERS, Icons.Filled.People, "我的"),
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val serverConfig = LocalServerConfig.current

    val defaultServer = serverConfig.getDefaultServer()
    var hasServer by remember { mutableStateOf(defaultServer != null) }
    var isLoggedIn by remember { mutableStateOf(RetrofitClient.token != null && hasServer) }

    LaunchedEffect(defaultServer, RetrofitClient.token) {
        hasServer = defaultServer != null
        isLoggedIn = RetrofitClient.token != null && hasServer
    }

    val startRoute = when {
        !hasServer -> Routes.SERVER_LIST
        !isLoggedIn -> Routes.LOGIN
        else -> Routes.DASHBOARD
    }

    Scaffold(
        bottomBar = {
            if (isLoggedIn && hasServer) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                ) {
                    mainNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label) },
                            selected = navController.currentBackStackEntry?.destination?.route == item.route,
                            onClick = {
                                if (navController.currentBackStackEntry?.destination?.route != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(startRoute) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
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
                DashboardScreen(
                    onNavigateToFenyong = { navController.navigate(Routes.FENYONG) },
                    onNavigateToMyPlugins = { navController.navigate(Routes.MY_PLUGINS) },
                    onNavigateToPluginMarket = { navController.navigate(Routes.PLUGIN_MARKET) },
                    onNavigateToMasters = { navController.navigate(Routes.MASTERS) },
                    onNavigateToTasks = { navController.navigate(Routes.TASKS) },
                    onNavigateToService = { navController.navigate(Routes.SERVICE) },
                    onNavigateToStorage = { navController.navigate(Routes.STORAGE) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                )
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
                MyPluginsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PLUGIN_MARKET) {
                PluginMarketScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.MASTERS) {
                MastersScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.TASKS) {
                TasksScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SERVICE) {
                ServiceScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.STORAGE) {
                StorageScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
