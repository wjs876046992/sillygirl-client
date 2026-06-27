package com.sillygirl.client.ui.screens.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillygirl.client.ui.components.GlassCard
import com.sillygirl.client.ui.components.MiniAppBar
import com.sillygirl.client.ui.theme.*

/**
 * 日志级别颜色配置
 */
private object LogLevelColors {
    val Info = Color(0xFF1890FF)      // 蓝色
    val Debug = Color(0xFF52C41A)     // 绿色
    val Warn = Color(0xFFFAAD14)      // 橙色
    val Error = Color(0xFFFF4D4F)     // 红色
    val Log = Color(0xFF722ED1)       // 紫色
    val Unknown = Color(0xFF8C8C8C)   // 灰色
}

/**
 * 根据日志级别返回对应颜色
 */
private fun getLevelColor(level: String): Color {
    return when (level.lowercase()) {
        "info" -> LogLevelColors.Info
        "debug" -> LogLevelColors.Debug
        "warn" -> LogLevelColors.Warn
        "error" -> LogLevelColors.Error
        "log" -> LogLevelColors.Log
        else -> LogLevelColors.Unknown
    }
}

/**
 * 插件日志查看页面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PluginLogsScreen(
    pluginUuid: String,
    pluginTitle: String,
    onBack: () -> Unit,
    viewModel: PluginLogsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }

    // 日志级别选项
    val levelOptions = listOf(
        null to "全部",
        "info" to "Info",
        "debug" to "Debug",
        "warn" to "Warn",
        "error" to "Error",
        "log" to "Log",
    )

    // 初始加载
    LaunchedEffect(pluginUuid) {
        viewModel.loadLogs(pluginUuid)
        viewModel.loadStats(pluginUuid)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    // 客户端过滤后的日志
    val filteredLogs = remember(uiState.logs, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.logs
        } else {
            uiState.logs.filter { log ->
                log.content.contains(searchQuery, ignoreCase = true) ||
                    log.level.contains(searchQuery, ignoreCase = true) ||
                    log.version.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = {
                    Column {
                        Text(
                            "${pluginTitle} 日志",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (uiState.stats != null) {
                            Text(
                                "共 ${uiState.stats!!.total} 条",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                placeholder = { Text("搜索日志内容...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, "清除")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            // 日志级别筛选 chips
            FlowRow(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                levelOptions.forEach { (level, label) ->
                    val isSelected = uiState.selectedLevel == level
                    val chipColor = if (level != null) getLevelColor(level) else MaterialTheme.colorScheme.primary

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setLevel(level) },
                        label = {
                            Text(
                                label,
                                fontSize = 12.sp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    chipColor
                                },
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor,
                        ),
                    )
                }
            }

            // 统计信息卡片
            uiState.stats?.let { stats ->
                if (stats.total > 0) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            StatItem("总计", stats.total.toString(), MaterialTheme.colorScheme.primary)
                            StatItem("Info", stats.byLevel["info"]?.toString() ?: "0", LogLevelColors.Info)
                            StatItem("Debug", stats.byLevel["debug"]?.toString() ?: "0", LogLevelColors.Debug)
                            StatItem("Warn", stats.byLevel["warn"]?.toString() ?: "0", LogLevelColors.Warn)
                            StatItem("Error", stats.byLevel["error"]?.toString() ?: "0", LogLevelColors.Error)
                        }
                    }
                }
            }

            // 日志列表
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                filteredLogs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.Article,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isNotBlank()) "没有匹配的日志" else "暂无日志",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filteredLogs, key = { "${it.unix}_${it.content.hashCode()}" }) { log ->
                            LogEntryCard(log)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 统计项组件
 */
@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 单条日志卡片
 */
@Composable
private fun LogEntryCard(log: com.sillygirl.client.data.model.PluginLogEntry) {
    val levelColor = getLevelColor(log.level)

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // 日志级别标签
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = levelColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    log.level.uppercase(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = levelColor,
                    fontSize = 10.sp,
                )
            }

            // 日志内容
            Column(modifier = Modifier.weight(1f)) {
                // 内容
                Text(
                    log.content,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    ),
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(4.dp))

                // 底部信息：时间 + 版本
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 时间
                    Text(
                        formatLogTime(log.unix),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )

                    // 版本标签
                    if (log.version.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                log.version,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 格式化日志时间戳
 */
private fun formatLogTime(unix: Long): String {
    if (unix <= 0) return ""
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = unix * 1000
    val now = java.util.Calendar.getInstance()

    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
    val second = cal.get(java.util.Calendar.SECOND).toString().padStart(2, '0')
    val time = "$hour:$minute:$second"

    return when {
        cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR) -> "今天 $time"
        cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR) - 1 -> "昨天 $time"
        else -> {
            val year = cal.get(java.util.Calendar.YEAR)
            val month = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
            "$year-$month-$day $time"
        }
    }
}