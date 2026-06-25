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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillygirl.client.data.model.UserData
import com.sillygirl.client.ui.components.*
import com.sillygirl.client.ui.theme.*

private val GrayText = Color(0xFF999999)

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
    onRefreshReady: (((Boolean) -> Unit) -> Unit)? = null,
    viewModel: DashboardViewModel = viewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 将刷新回调暴露给外部（顶部栏刷新按钮）
    LaunchedEffect(Unit) {
        onRefreshReady?.invoke { showRefreshHint ->
            viewModel.loadDashboard(forceRefresh = true, showRefreshHint = showRefreshHint)
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val name = currentUser?.name ?: uiState.userName
    val installedPlugins = currentUser?.plugins?.size ?: uiState.installedPlugins

    // 监听 snackbar 消息
    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
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

                item {
                    FenyongOverviewCard(uiState.fenyongDashboard, onNavigateToFenyong)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricGridCard(
                            icon = Icons.Filled.Extension,
                            value = "$installedPlugins",
                            label = "插件",
                            color = Color(0xFF667EEA),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToMyPlugins,
                        )
                        MetricGridCard(
                            icon = Icons.Filled.People,
                            value = "${uiState.masterCount}",
                            label = "管理员",
                            color = Color(0xFF52C41A),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToMasters,
                        )
                        MetricGridCard(
                            icon = Icons.Filled.Schedule,
                            value = "${uiState.activeTaskCount}",
                            label = "定时任务",
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
}

@Composable
private fun MetricGridCard(
    icon: ImageVector,
    value: String,
    label: String,
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = color)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ===== 平台 key → 中文名 =====
private fun platformDisplayName(code: String): String = when (code.lowercase()) {
    "jd" -> "京东"
    "tb" -> "淘宝"
    "pdd" -> "拼多多"
    else -> code
}

// ===== 分佣概览（三板块：总额 / 交叉表格 / 平台卡片） =====
@Composable
private fun FenyongOverviewCard(dash: com.sillygirl.client.data.model.FenyongDashboardResponse?, onClick: () -> Unit) {
    if (dash == null) return

    val byTime = dash.byTime
    val bySite = dash.bySite
    val cross = dash.cross

    // 构建交叉查找表 crossLookup[site][period] → FenyongCrossItem
    val crossLookup = cross.groupBy({ it.site }, { it.period to it })
        .mapValues { (_, v) -> v.toMap() }

    // 行列定义
    val periods = listOf("today" to "今日", "last7days" to "7天", "lastMonth" to "本月")
    val sites = listOf("jd", "tb", "pdd")
    val siteLabels = sites.map { platformDisplayName(it) }

    GlassCard(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 120.dp),
    ) {
        // ── 标题栏 ──
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

        Spacer(Modifier.height(10.dp))

        // ── 板块① 全部金额总览 ──
        val grandTotal = byTime["total"] ?: com.sillygirl.client.data.model.FenyongStats()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("全部预估", style = MaterialTheme.typography.labelSmall, color = GrayText)
                Spacer(Modifier.height(2.dp))
                Text("¥${feyMoney(grandTotal.estimate)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("全部实际", style = MaterialTheme.typography.labelSmall, color = GrayText)
                Spacer(Modifier.height(2.dp))
                Text("¥${feyMoney(grandTotal.actual)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SuccessColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("总订单", style = MaterialTheme.typography.labelSmall, color = GrayText)
                Spacer(Modifier.height(2.dp))
                Text("${grandTotal.orders}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 板块② 交叉表格 ──
        Text("佣金明细", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            // 表头行
            Row(modifier = Modifier.fillMaxWidth()) {
                // 左上角空格
                Box(modifier = Modifier.width(42.dp).height(28.dp))
                // 列标题
                for ((_, label) in periods) {
                    Box(
                        modifier = Modifier.weight(1f).height(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // 数据行（各平台）
            for ((siteIdx, siteKey) in sites.withIndex()) {
                val siteStats = bySite[siteKey]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (siteIdx % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .padding(vertical = 2.dp),
                ) {
                    // 行标题
                    Box(modifier = Modifier.width(42.dp).height(48.dp), contentAlignment = Alignment.CenterStart) {
                        Text(siteLabels[siteIdx], style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    // 各时段
                    for ((periodKey, _) in periods) {
                        val item = crossLookup[siteKey]?.get(periodKey)
                        Box(modifier = Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "¥${feyMoney(item?.estimate ?: 0.0)}",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "¥${feyMoney(item?.actual ?: 0.0)}",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessColor,
                                )
                            }
                        }
                    }
                }
            }

            // 分隔线 + 合计行
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            val totalRow = periods.associate { (key, _) -> key to (byTime[key] ?: com.sillygirl.client.data.model.FenyongStats()) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(vertical = 2.dp),
            ) {
                Box(modifier = Modifier.width(42.dp).height(48.dp), contentAlignment = Alignment.CenterStart) {
                    Text("合计", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                for ((periodKey, _) in periods) {
                    val cell = totalRow[periodKey]
                    Box(modifier = Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "¥${feyMoney(cell?.estimate ?: 0.0)}",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "¥${feyMoney(cell?.actual ?: 0.0)}",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessColor,
                            )
                        }
                    }
                }
            }
        }
        val siteEntries = bySite.filter { it.key != "total" }.entries.toList()
        if (siteEntries.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("平台汇总", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for ((i, entry) in siteEntries.withIndex()) {
                    val (siteKey, stats) = entry
                    SiteSummaryCard(
                        siteCode = siteKey,
                        estimate = stats.estimate,
                        actual = stats.actual,
                        orders = stats.orders,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ===== 平台汇总小卡片 =====
@Composable
private fun SiteSummaryCard(siteCode: String, estimate: Double, actual: Double, orders: Int, modifier: Modifier = Modifier) {
    val color = getSiteColor(siteCode)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(platformDisplayName(siteCode), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color)
        Text("¥${feyMoney(estimate)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("¥${feyMoney(actual)}", style = MaterialTheme.typography.labelSmall, color = SuccessColor)
        Text("${orders}单", style = MaterialTheme.typography.labelSmall, color = GrayText)
    }
}

@Composable
private fun getSiteColor(siteCode: String): Color = when (siteCode.lowercase()) {
    "jd" -> Color(0xFFE60012)
    "tb" -> Color(0xFFFF5000)
    "pdd" -> Color(0xFFE02A24)
    else -> MaterialTheme.colorScheme.primary
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
