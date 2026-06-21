package com.sillygirl.client.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillygirl.client.ui.components.*
import com.sillygirl.client.ui.theme.*

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
                title = {
                    Column {
                        Text("SillyGirl", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("管理面板", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ===== 欢迎卡片 =====
                WelcomeHeader(name = uiState.userName)

                // ===== 统计行 =====
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatNumberCard(
                        Modifier.weight(1f),
                        icon = Icons.Filled.Extension,
                        iconColor = Color(0xFF667EEA),
                        value = "${uiState.installedPlugins}",
                        label = "已安装插件",
                        onClick = onNavigateToMyPlugins,
                    )
                    StatNumberCard(
                        Modifier.weight(1f),
                        icon = Icons.Filled.People,
                        iconColor = Color(0xFF52C41A),
                        value = "${uiState.masterCount}",
                        label = "管理员",
                        onClick = onNavigateToMasters,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatNumberCard(
                        Modifier.weight(1f),
                        icon = Icons.Filled.Schedule,
                        iconColor = Color(0xFFF59E0B),
                        value = "${uiState.activeTaskCount}",
                        label = "运行中任务",
                        onClick = onNavigateToTasks,
                    )
                }

                // ===== 分佣概览 =====
                uiState.fenyongDashboard?.let { dash ->
                    GlassCard(
                        onClick = onNavigateToFenyong,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(PrimaryGradientColors), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("💰", fontSize = 22.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("分佣概览", fontWeight = FontWeight.Bold)
                                Text("今日收入 ¥${feyMoney(dash.today.actual)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(">", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Mini stats
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MiniStat("已结算", "¥${feyMoney(dash.totalSettled)}")
                            MiniStat("总订单", feyInt(dash.totalOrders))
                        }
                    }
                }

                // ===== 快捷入口 =====
                QuickActionGrid(
                    onNavigateToFenyong, onNavigateToPluginMarket, onNavigateToStorage,
                    onNavigateToService, onNavigateToMasters, onNavigateToTasks,
                )

                if (uiState.error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeader(name: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(PrimaryGradientColors), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            // 背景装饰
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(100.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50.dp)),
            )

            Column {
                Text("欢迎回来 👋", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(4.dp))
                Text(name, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("SillyGirl 管理助手", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun QuickActionGrid(
    onNavigateToFenyong: () -> Unit,
    onNavigateToPluginMarket: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToService: () -> Unit,
    onNavigateToMasters: () -> Unit,
    onNavigateToTasks: () -> Unit,
) {
    val actions = listOf(
        QuickAction(Icons.Filled.Paid, "分佣", onNavigateToFenyong, PrimaryGradientColors),
        QuickAction(Icons.Filled.Extension, "插件市场", onNavigateToPluginMarket, listOf(Color(0xFF5CC3FF), Color(0xFF4FACFE))),
        QuickAction(Icons.Filled.Storage, "存储", onNavigateToStorage, listOf(Color(0xFF22C55E), Color(0xFF10B981))),
        QuickAction(Icons.Filled.Dns, "服务", onNavigateToService, listOf(Color(0xFFF59E0B), Color(0xFFF97316))),
        QuickAction(Icons.Filled.People, "管理员", onNavigateToMasters, listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))),
        QuickAction(Icons.Filled.Schedule, "定时任务", onNavigateToTasks, listOf(Color(0xFFEF4444), Color(0xFFF43F5E))),
    )

    for (i in actions.indices step 3) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (j in 0 until 3) {
                val idx = i + j
                if (idx < actions.size) {
                    QuickActionItem(actions[idx], Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        if (i + 3 < actions.size) {
            Spacer(Modifier.height(12.dp))
        }
    }
}

data class QuickAction(val icon: ImageVector, val label: String, val onClick: () -> Unit, val colors: List<Color>)

@Composable
private fun QuickActionItem(action: QuickAction, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.clickable(onClick = action.onClick)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(action.colors), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    action.icon, null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(action.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun feyMoney(v: Double) = if (v >= 10000) String.format("%.1f万", v / 10000) else String.format("%.2f", v)
private fun feyInt(v: Int) = if (v >= 10000) String.format("%.1f万", v / 10000.0) else "$v"
