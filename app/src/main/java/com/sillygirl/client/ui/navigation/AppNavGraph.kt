package com.sillygirl.client.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sillygirl.client.ui.screens.dashboard.DashboardScreen
import com.sillygirl.client.ui.screens.fenyong.FenyongScreen
import com.sillygirl.client.ui.screens.login.LoginScreen
import com.sillygirl.client.ui.screens.settings.SettingsScreen

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val FENYONG = "fenyong"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    var isLoggedIn by remember { mutableStateOf(false) }

    val startRoute = if (isLoggedIn) Routes.DASHBOARD else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startRoute,
    ) {
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
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.FENYONG) {
            FenyongScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                    isLoggedIn = false
                }
            )
        }
    }
}
