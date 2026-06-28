package com.sillygirl.client.ui.screens.plugins

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillygirl.client.ui.components.MiniAppBar
import com.sillygirl.client.ui.theme.*

private object LogLevelColors {
    val Info = Color(0xFF1890FF)
    val Debug = Color(0xFF52C41A)
    val Warn = Color(0xFFFAAD14)
    val Error = Color(0xFFFF4D4F)
    val Log = Color(0xFF722ED1)
    val Unknown = Color(0xFF8C8C8C)
}

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

    val levelOptions = listOf(
        null to "全部",
        "info" to "Info",
        "debug" to "Debug",
        "warn" to "Warn",
        "error" to "Error",
        "log" to "Log",
    )

    LaunchedEffect(pluginUuid) {
        viewModel.loadLogs(pluginUuid)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

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
                .padding(padding)
                .verticalScroll(rememberScrollState()),
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

            // 级别筛选 chips
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
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else chipColor,
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

            // 统计卡片
            uiState.stats?.let { stats ->
                if (stats.total > 0) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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

            // 加载中
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            // 错误
            if (!uiState.isLoading && uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.refresh() }) { Text("重试") }
                    }
                }
            }

            // 空状态
            if (!uiState.isLoading && uiState.error == null && filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.Article, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "没有匹配的日志" else "暂无日志",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 日志列表 — 纯平铺，无嵌套
            if (!uiState.isLoading && uiState.error == null && filteredLogs.isNotEmpty()) {
                filteredLogs.forEach { log ->
                    LogEntryCard(log)
                }

                // 分页导航
                if (uiState.total > uiState.pageSize) {
                    PaginationBar(
                        currentPage = uiState.currentPage,
                        totalPages = uiState.totalPages,
                        total = uiState.total,
                        hasPrev = uiState.hasPrevPage,
                        hasNext = uiState.hasNextPage,
                        onPrev = { viewModel.loadPage(uiState.currentPage - 1) },
                        onNext = { viewModel.loadPage(uiState.currentPage + 1) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LogEntryCard(log: com.sillygirl.client.data.model.PluginLogEntry) {
    val levelColor = getLevelColor(log.level)
    var isExpanded by remember { mutableStateOf(false) }
    val shouldTruncate = log.content.lines().size > 10 || log.content.length > 500
    val context = LocalContext.current
    var showCopied by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .background(levelColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
                    .padding(top = 1.dp),
            ) {
                Text(log.level.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = levelColor, fontSize = 10.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    log.content,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 10,
                    overflow = TextOverflow.Ellipsis,
                )

                if (shouldTruncate) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.padding(0.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isExpanded) "收起" else "点击展开全部",
                            fontSize = 12.sp,
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(formatLogTime(log.unix), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    if (log.version.isNotBlank()) {
                        Text(
                            log.version,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 复制按钮
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("log", log.content)
                    clipboard.setPrimaryClip(clip)
                    showCopied = true
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    if (showCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = "复制",
                    modifier = Modifier.size(16.dp),
                    tint = if (showCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // 复制成功提示自动消失
    if (showCopied) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            showCopied = false
        }
    }
}

@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    total: Int,
    hasPrev: Boolean,
    hasNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "共 $total 条",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = onPrev,
                    enabled = hasPrev,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("上一页", fontSize = 12.sp)
                }

                Text(
                    "$currentPage / $totalPages",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )

                FilledTonalButton(
                    onClick = onNext,
                    enabled = hasNext,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text("下一页", fontSize = 12.sp)
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(16.dp))
                }
            }
        }
    }
}

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
