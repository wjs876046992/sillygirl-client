package com.sillygirl.client.ui.navigation

import androidx.compose.runtime.*
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

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val serverConfig = LocalServerConfig.current

    // 检查是否已有选中的服务器
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

    NavHost(
        navController = navController,
        startDestination = startRoute,
    ) {
        // ---- Server Selection ----
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

        // ---- Auth ----
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

        // ---- Dashboard (hub) ----
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

        // ---- Settings ----
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

        // ---- Fenyong ----
        composable(Routes.FENYONG) {
            FenyongScreen(onBack = { navController.popBackStack() })
        }

        // ---- Plugins ----
        composable(Routes.MY_PLUGINS) {
            MyPluginsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PLUGIN_MARKET) {
            PluginMarketScreen(onBack = { navController.popBackStack() })
        }

        // ---- Masters ----
        composable(Routes.MASTERS) {
            MastersScreen(onBack = { navController.popBackStack() })
        }

        // ---- Tasks ----
        composable(Routes.TASKS) {
            TasksScreen(onBack = { navController.popBackStack() })
        }

        // ---- Service ----
        composable(Routes.SERVICE) {
            ServiceScreen(onBack = { navController.popBackStack() })
        }

        // ---- Storage ----
        composable(Routes.STORAGE) {
            StorageScreen(onBack = { navController.popBackStack() })
        }
    }
}
