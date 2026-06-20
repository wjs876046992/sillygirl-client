package com.sillygirl.client.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToFenyong: () -> Unit = {},
    onNavigateToMyPlugins: () -> Unit = {},
    onNavigateToPluginMarket: () -> Unit = {},
    onNavigateToMasters: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToService: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SillyGirl") },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) { Icon(Icons.Filled.Refresh, "刷新") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Filled.Settings, "设置") }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WelcomeCard(name = uiState.userName)

                // Stats row — clickable
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(Modifier.weight(1f), Icons.Filled.Extension, "已安装插件", "${uiState.installedPlugins}", onClick = onNavigateToMyPlugins)
                    StatCard(Modifier.weight(1f), Icons.Filled.People, "管理员", "${uiState.masterCount}", onClick = onNavigateToMasters)
                    StatCard(Modifier.weight(1f), Icons.Filled.Schedule, "运行中任务", "${uiState.activeTaskCount}", onClick = onNavigateToTasks)
                }

                // Quick actions
                Text("快捷入口", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard(Modifier.weight(1f), Icons.Filled.Paid, "分佣", onClick = onNavigateToFenyong)
                    QuickActionCard(Modifier.weight(1f), Icons.Filled.Extension, "插件市场", onClick = onNavigateToPluginMarket)
                    QuickActionCard(Modifier.weight(1f), Icons.Filled.Storage, "存储", onClick = onNavigateToStorage)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard(Modifier.weight(1f), Icons.Filled.Dns, "服务", onClick = onNavigateToService)
                    QuickActionCard(Modifier.weight(1f), Icons.Filled.ManageAccounts, "管理员", onClick = onNavigateToMasters)
                    QuickActionCard(Modifier.weight(1f), Icons.Filled.Schedule, "定时任务", onClick = onNavigateToTasks)
                }

                // Fenyong summary
                uiState.fenyongDashboard?.let { dash ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToFenyong),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Paid, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text("分佣概览", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.weight(1f))
                                Text("查看详情 >", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                FenyongStatItem("今日收入", "¥${feyMoney(dash.today.actual)}")
                                FenyongStatItem("已结算", "¥${feyMoney(dash.totalSettled)}")
                                FenyongStatItem("总订单", feyInt(dash.totalOrders))
                            }
                        }
                    }
                }

                if (uiState.error != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WelcomeCard(name: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Text("欢迎回来 👋", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, icon: ImageVector, label: String, value: String, onClick: () -> Unit = {}) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickActionCard(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FenyongStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun feyMoney(v: Double) = if (v >= 10000) String.format("%.1f万", v / 10000) else String.format("%.2f", v)
private fun feyInt(v: Int) = if (v >= 10000) String.format("%.1f万", v / 10000.0) else "$v"
