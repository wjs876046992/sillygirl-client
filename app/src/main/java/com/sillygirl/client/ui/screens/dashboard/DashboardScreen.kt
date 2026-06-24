package com.sillygirl.client.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillygirl.client.data.model.UserData
import com.sillygirl.client.ui.components.*
import com.sillygirl.client.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentUser: UserData? = null,
    onNavigateToFenyong: () -> Unit = {},
    onNavigateToPluginMarket: () -> Unit = {},
    onNavigateToMyPlugins: () -> Unit = {},
    onNavigateToMasters: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToService: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onRefreshReady: ((() -> Unit) -> Unit)? = null,
    viewModel: DashboardViewModel = viewModel(),
) {
    // 将刷新回调暴露给外部（顶部栏刷新按钮）
    LaunchedEffect(Unit) {
        onRefreshReady?.invoke { viewModel.loadDashboard(forceRefresh = true) }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val name = currentUser?.name ?: uiState.userName
    val installedPlugins = currentUser?.plugins?.size ?: uiState.installedPlugins

    // 下拉刷新状态
    var isRefreshing by remember { mutableStateOf(false) }
    var pullDistance by remember { mutableFloatStateOf(0f) }
    val refreshThreshold = 150f

    // 监听加载状态变化，结束刷新动画
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && isRefreshing) {
            isRefreshing = false
            pullDistance = 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading && !isRefreshing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (pullDistance > refreshThreshold && !isRefreshing) {
                                    isRefreshing = true
                                    viewModel.loadDashboard(forceRefresh = true)
                                }
                                pullDistance = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0 && !isRefreshing) {
                                    pullDistance = (pullDistance + dragAmount).coerceAtMost(200f)
                                }
                            }
                        )
                    },
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 下拉刷新指示器
                if (pullDistance > 0 || isRefreshing) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((pullDistance * 0.5f).coerceAtMost(60f).dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else if (pullDistance > refreshThreshold) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "释放刷新",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            } else {
                                Icon(
                                    Icons.Filled.ArrowDownward,
                                    contentDescription = "下拉刷新",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate((pullDistance / refreshThreshold * 180f).coerceAtMost(180f)),
                                )
                            }
                        }
                    }
                }

                item { WelcomeHeader(name = name) }

                item {
                    FenyongOverviewCard(uiState.fenyongDashboard, onNavigateToFenyong)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricGridCard(
                            icon = Icons.Filled.Extension,
                            value = "$installedPlugins",
                            color = Color(0xFF667EEA),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToMyPlugins,
                        )
                        MetricGridCard(
                            icon = Icons.Filled.People,
                            value = "${uiState.masterCount}",
                            color = Color(0xFF52C41A),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToMasters,
                        )
                        MetricGridCard(
                            icon = Icons.Filled.Schedule,
                            value = "${uiState.activeTaskCount}",
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToTasks,
                        )
                    }
                }

                item {
                    FeatureGrid(
                        onNavigateToPluginMarket,
                        onNavigateToStorage,
                        onNavigateToService,
                    )
                }

                if (uiState.error != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .themeShadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp)),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Error,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                            Text(
                                uiState.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===== 欢迎 =====
@Composable
private fun WelcomeHeader(name: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .themeShadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(PrimaryGradientColors))
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .align(Alignment.TopEnd)
                    .offset(x = 14.dp, y = (-14).dp)
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .align(Alignment.BottomEnd)
                    .offset(x = (-10).dp, y = 10.dp)
            )

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Text(
                    "你好，$name",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "SillyGirl 管理助手",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun MetricGridCard(
    icon: ImageVector,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier
            .themeShadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, modifier = Modifier.size(24.dp), tint = color)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun FixedSiteCard(siteName: String, amount: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(siteName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Spacer(Modifier.height(2.dp))
            Text(amount, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ===== 分佣概览 =====
@Composable
private fun FenyongOverviewCard(dash: com.sillygirl.client.data.model.FenyongDashboardResponse?, onClick: () -> Unit) {
    if (dash == null) return

    GlassCard(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 120.dp).heightIn(min = 130.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Brush.horizontalGradient(PrimaryGradientColors), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("💰", fontSize = 12.sp)
                }
                Spacer(Modifier.width(5.dp))
                Text("分佣", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                null,
                modifier = Modifier.rotate(-45f).size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("今日", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                Spacer(Modifier.height(2.dp))
                Text(feyMoney(dash.today.estimate), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("7天", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                Spacer(Modifier.height(2.dp))
                Text(feyMoney(dash.last7days.estimate), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("本月", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                Spacer(Modifier.height(2.dp))
                Text(feyMoney(dash.lastMonth.estimate), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        val sites = dash.platforms.entries.toList()
        if (sites.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            val chunks = sites.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (chunk in chunks) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        for ((i, site) in chunk.withIndex()) {
                            val (siteName, stats) = site
                            FixedSiteCard(siteName, feyMoney(stats.estimate), modifier = Modifier.weight(1f))
                            if (i < chunk.lastIndex) Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ===== 功能网格 =====
@Composable
private fun FeatureGrid(
    onNavigateToPluginMarket: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToService: () -> Unit,
) {
    val actions = listOf(
        FeatureItem(Icons.Filled.Store, "插件市场", onNavigateToPluginMarket, Color(0xFF667EEA)),
        FeatureItem(Icons.Filled.Storage, "存储", onNavigateToStorage, Color(0xFF22C55E)),
        FeatureItem(Icons.Filled.Dns, "服务", onNavigateToService, Color(0xFFF59E0B)),
    )

    val gridItems = actions.chunked(3)

    for ((rowIdx, rowItems) in gridItems.withIndex()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (item in rowItems) {
                FeatureCard(item, modifier = Modifier.weight(1f))
            }
        }
        if (rowIdx < gridItems.size - 1) Spacer(Modifier.height(12.dp))
    }
}

data class FeatureItem(val icon: ImageVector, val label: String, val onClick: () -> Unit, val color: Color)

@Composable
private fun FeatureCard(
    item: FeatureItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .themeShadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(14.dp))
            .clickable(onClick = item.onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    item.icon,
                    null,
                    modifier = Modifier.size(24.dp),
                    tint = item.color,
                )
            }
            Text(
                item.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun feyMoney(v: Double) = if (v >= 10000) String.format("%.1f万", v / 10000) else String.format("%.2f", v)
