package com.sillygirl.client.ui.screens.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.sillygirl.client.ui.components.GlassCard
import com.sillygirl.client.ui.theme.*
import com.sillygirl.client.data.model.PluginRoute
import com.sillygirl.client.ui.components.MiniAppBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MyPluginsScreen(
    onBack: () -> Unit,
    onPluginClick: (PluginRoute) -> Unit,
    onRefreshCurrentUser: () -> Unit = {},
    viewModel: MyPluginsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedClasses by remember { mutableStateOf(setOf<String>()) }
    var showUninstallDialog by remember { mutableStateOf<PluginRoute?>(null) }
    var showReloadDialog by remember { mutableStateOf<PluginRoute?>(null) }

    // 初始加载
    LaunchedEffect(Unit) {
        viewModel.loadPlugins(page = 1)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    // 卸载确认对话框
    showUninstallDialog?.let { plugin ->
        AlertDialog(
            onDismissRequest = { showUninstallDialog = null },
            title = { Text("卸载插件") },
            text = { Text("确定要卸载「${plugin.title.ifBlank { plugin.name }}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pluginPath = plugin.path
                        showUninstallDialog = null
                        viewModel.uninstallPlugin(pluginPath) {
                            onRefreshCurrentUser()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("卸载") }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = null }) { Text("取消") }
            },
        )
    }

    // 重载确认对话框
    showReloadDialog?.let { plugin ->
        AlertDialog(
            onDismissRequest = { showReloadDialog = null },
            title = { Text("重载插件") },
            text = { Text("确定要重载「${plugin.title.ifBlank { plugin.name }}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pluginPath = plugin.path
                        showReloadDialog = null
                        viewModel.reloadPlugin(pluginPath) {
                            onRefreshCurrentUser()
                        }
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showReloadDialog = null }) { Text("取消") }
            },
        )
    }

    // 收集所有 classes
    val allClasses = remember(uiState.plugins) {
        uiState.plugins.flatMap { it.classes }.distinct().sorted()
    }

    // 过滤插件
    val filteredPlugins = remember(uiState.plugins, searchQuery, selectedClasses) {
        uiState.plugins.filter { plugin ->
            val matchesSearch = searchQuery.isBlank() ||
                plugin.title.contains(searchQuery, ignoreCase = true) ||
                plugin.name.contains(searchQuery, ignoreCase = true) ||
                plugin.description.contains(searchQuery, ignoreCase = true) ||
                plugin.author.contains(searchQuery, ignoreCase = true)
            val matchesClass = selectedClasses.isEmpty() || plugin.classes.any { it in selectedClasses }
            matchesSearch && matchesClass
        }
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text("我的插件 (${uiState.total})") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { viewModel.loadPlugins(page = 1, showRefreshHint = true) }) { Icon(Icons.Filled.Refresh, "刷新") } }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { p ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.plugins.isEmpty() -> Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Extension, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("暂无已安装插件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                Column(Modifier.fillMaxSize().padding(p)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // 搜索栏
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("搜索插件名称、描述、作者...") },
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
                        }

                        // Classes 筛选（多选）
                        if (allClasses.isNotEmpty()) {
                            item {
                                Column {
                                    Text(
                                        "分类筛选",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        allClasses.forEach { cls ->
                                            val isSelected = cls in selectedClasses
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedClasses = if (isSelected) {
                                                        selectedClasses - cls
                                                    } else {
                                                        selectedClasses + cls
                                                    }
                                                },
                                                label = { Text(cls) },
                                                shape = RoundedCornerShape(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 插件统计
                        item {
                            Text(
                                "共 ${filteredPlugins.size} 个插件" +
                                    if (uiState.plugins.count { it.running } > 0)
                                        " · ${uiState.plugins.count { it.running }} 个运行中"
                                    else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (filteredPlugins.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.SearchOff, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(8.dp))
                                        Text("没有匹配的插件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else {
                            items(filteredPlugins, key = { it.path }) { plugin ->
                                MyPluginCard(
                                    plugin = plugin,
                                    onClick = { onPluginClick(plugin) },
                                    onReload = { showReloadDialog = plugin },
                                    onUninstall = { showUninstallDialog = plugin },
                                )
                            }

                            // 加载更多指示器
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 分页控件
                    if (uiState.totalPages > 1) {
                        PaginationWidget(
                            currentPage = uiState.currentPage,
                            totalPages = uiState.totalPages,
                            onPrevPage = { viewModel.goToPage(uiState.currentPage - 1) },
                            onNextPage = { viewModel.goToPage(uiState.currentPage + 1) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PluginMarketScreen(
    onBack: () -> Unit,
    onPluginClick: (PluginRoute) -> Unit = {},
    onRefreshCurrentUser: () -> Unit = {},
    viewModel: PluginMarketViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }

    // Tab 数据
    val tabs = listOf(
        "tab1" to "已安装",
        "tab2" to "未安装",
        "tab3" to "可升级",
    )
    val tabCounts = mapOf(
        "tab1" to uiState.tab1Count,
        "tab2" to uiState.tab2Count,
        "tab3" to uiState.tab3Count,
    )

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    // 搜索变更时重新从服务端加载
    fun reloadWithSearch() {
        viewModel.loadWithKeyword(keyword = searchQuery.ifBlank { null })
    }

    // 异常消息弹窗
    uiState.messagesDialogPlugin?.let { plugin ->
        PluginMessagesDialog(
            plugin = plugin,
            onDismiss = { viewModel.dismissMessagesDialog() },
            onClear = { viewModel.clearPluginMessages(plugin.id) },
        )
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text("插件市场") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = {
                        searchQuery = ""
                        viewModel.load(page = 1, isRefresh = true, showRefreshHint = true)
                    }) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            // Tab 栏
            TabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == uiState.activeTab }.coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEach { (key, label) ->
                    val count = tabCounts[key]
                    Tab(
                        selected = uiState.activeTab == key,
                        onClick = {
                            searchQuery = ""
                            viewModel.switchTab(key)
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(label)
                                if (count != null && count > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (uiState.activeTab == key)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                    ) {
                                        Text(
                                            "$count",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.plugins.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Store, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("暂无插件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // 搜索栏
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    reloadWithSearch()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("搜索插件名称、描述、作者...") },
                                leadingIcon = { Icon(Icons.Filled.Search, null) },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            reloadWithSearch()
                                        }) {
                                            Icon(Icons.Filled.Clear, "清除")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                            )
                        }

                        // 插件统计
                        item {
                            Text(
                                "共 ${uiState.total} 个插件",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (uiState.plugins.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.SearchOff, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(8.dp))
                                        Text("没有匹配的插件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else {
                            items(uiState.plugins) { plugin ->
                                val installedPlugin = viewModel.getInstalledPlugin(plugin.id)
                                MarketPluginCard(
                                    plugin = plugin,
                                    installedPlugin = installedPlugin,
                                    onInstall = { viewModel.installPlugin(plugin.id) { onRefreshCurrentUser() } },
                                    onUninstall = { viewModel.uninstallPlugin(plugin.id) { onRefreshCurrentUser() } },
                                    onToggleDebug = { debug -> viewModel.toggleDebug(plugin.id, debug) },
                                    onToggleDisable = { disable -> viewModel.toggleDisable(plugin.id, disable) },
                                    onToggleRunning = { running -> viewModel.toggleRunning(plugin.id, running) },
                                    onConfigForm = {
                                        installedPlugin?.let { onPluginClick(it) }
                                    },
                                    onShowMessages = { viewModel.showMessagesDialog(plugin) },
                                )
                            }
                        }

                        // 加载更多指示器
                        if (uiState.isLoadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }

                    // 分页控件
                    if (uiState.totalPages > 1) {
                        PaginationWidget(
                            currentPage = uiState.currentPage,
                            totalPages = uiState.totalPages,
                            onPrevPage = { viewModel.goToPage(uiState.currentPage - 1) },
                            onNextPage = { viewModel.goToPage(uiState.currentPage + 1) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyPluginCard(
    plugin: PluginRoute,
    onClick: () -> Unit,
    onReload: (() -> Unit)? = null,
    onUninstall: (() -> Unit)? = null,
) {
    GlassCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            // 图标 + 运行状态指示点
            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).themeShadow(4.dp, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        plugin.icon.isBlank() -> Icon(
                            Icons.Filled.Extension, null,
                            tint = if (plugin.running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        isIconUrl(plugin.icon) -> AsyncImage(
                            model = plugin.icon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        else -> Text(plugin.icon, fontSize = 20.sp)
                    }
                }
                // 运行状态点
                if (plugin.running && !plugin.disable) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(SuccessColor)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            // 内容区
            Column(Modifier.weight(1f)) {
                // 标题行 + 状态标签
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plugin.title.ifBlank { plugin.name },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    if (plugin.disable) {
                        StatusChip("已禁用", DangerColor)
                    }
                    if (plugin.debug) {
                        StatusChip("调试", MaterialTheme.colorScheme.tertiary)
                    }
                    if (plugin.hasForm) {
                        StatusChip("配置", MaterialTheme.colorScheme.secondary)
                    }
                }

                // 描述
                if (plugin.description.isNotBlank()) {
                    Text(
                        plugin.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                }

                // 底部信息行：版本 + 作者 + origin + 分类标签
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(plugin.version, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (plugin.author.isNotBlank()) {
                        Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            plugin.author,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    if (plugin.origin.isNotBlank()) {
                        Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                plugin.origin,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 9.sp,
                                maxLines = 1,
                            )
                        }
                    }
                    if (plugin.classes.isNotEmpty()) {
                        Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            plugin.classes.first(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                }

                // 操作按钮行（重载、卸载）
                if (onReload != null || onUninstall != null) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (onReload != null) {
                            AssistChip(
                                onClick = onReload,
                                label = { Text("重载", fontSize = 10.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Refresh,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp),
                            )
                        }
                        if (onUninstall != null) {
                            AssistChip(
                                onClick = onUninstall,
                                label = {
                                    Text(
                                        "卸载",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                }
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** 判断 icon 字段是否为 URL */
private fun isIconUrl(icon: String): Boolean =
    icon.startsWith("http://") || icon.startsWith("https://")

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.padding(start = 4.dp),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarketPluginCard(
    plugin: com.sillygirl.client.data.model.PluginInfo,
    installedPlugin: PluginRoute?,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onToggleDebug: (Boolean) -> Unit,
    onToggleDisable: (Boolean) -> Unit,
    onToggleRunning: (Boolean) -> Unit,
    onConfigForm: () -> Unit = {},
    onShowMessages: () -> Unit = {},
) {
    val isInstalled = installedPlugin != null

    GlassCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).themeShadow(4.dp, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    plugin.icon.isBlank() -> Icon(
                        Icons.Filled.Store, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    isIconUrl(plugin.icon) -> AsyncImage(
                        model = plugin.icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    else -> Text(plugin.icon, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                // 标题行 + 状态标签
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plugin.title.ifBlank { plugin.id }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (isInstalled) {
                        Spacer(Modifier.width(6.dp))
                        StatusChip("已安装", SuccessColor)
                    }
                    if (plugin.status == 1) {
                        Spacer(Modifier.width(6.dp))
                        StatusChip("可升级", MaterialTheme.colorScheme.primary)
                    }
                    if (plugin.status == 6) {
                        Spacer(Modifier.width(6.dp))
                        StatusChip("原创", Color(0xFF722ED1)) // 紫色
                    }
                }
                if (plugin.description.isNotBlank()) {
                    Text(plugin.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                // 标签行：来源 + 作者 + 下载量 + 版本
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 认证来源标签
                    if (plugin.identified && plugin.organization.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFF7E6), // 金色背景
                        ) {
                            Text(
                                plugin.organization,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD48806), // 金色文字
                                fontSize = 9.sp,
                            )
                        }
                    } else if (plugin.organization.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                plugin.organization,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                            )
                        }
                    }
                    Text("${plugin.author} · ${plugin.downloads}次下载 · v${plugin.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // 标签行：分类 + 特殊标签
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    if (plugin.classes.isNotEmpty()) {
                        plugin.classes.take(3).forEach { cls ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    cls,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 9.sp,
                                )
                            }
                        }
                    }
                    if (plugin.module) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                "模块",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 9.sp,
                            )
                        }
                    }
                    if (plugin.encrypt) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFF1E6), // 橙色背景
                        ) {
                            Text(
                                "加密脚本",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD46B08), // 橙色文字
                                fontSize = 9.sp,
                            )
                        }
                    }
                }

                // 已安装插件的操作按钮
                if (isInstalled) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // 运行状态开关
                        AssistChip(
                            onClick = { onToggleRunning(!installedPlugin.running) },
                            label = {
                                Text(
                                    if (installedPlugin.running) "运行中" else "已停止",
                                    fontSize = 10.sp,
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (installedPlugin.running) SuccessColor else Color.Gray)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp),
                        )

                        // Debug 开关
                        AssistChip(
                            onClick = { onToggleDebug(!installedPlugin.debug) },
                            label = {
                                Text(
                                    if (installedPlugin.debug) "调试" else "正常",
                                    fontSize = 10.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.BugReport,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (installedPlugin.debug) MaterialTheme.colorScheme.tertiary else Color.Gray,
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp),
                        )

                        // Disable 开关
                        AssistChip(
                            onClick = { onToggleDisable(!installedPlugin.disable) },
                            label = {
                                Text(
                                    if (installedPlugin.disable) "已禁用" else "启用",
                                    fontSize = 10.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (installedPlugin.disable) Icons.Filled.Block else Icons.Filled.CheckCircle,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (installedPlugin.disable) DangerColor else SuccessColor,
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp),
                        )

                        // 配置表单按钮（仅当插件有表单时显示）
                        if (installedPlugin.hasForm) {
                            AssistChip(
                                onClick = onConfigForm,
                                label = { Text("配置", fontSize = 10.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Settings,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.secondary,
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp),
                            )
                        }

                        // 异常消息按钮（仅当有消息时显示）
                        if (plugin.messages.isNotEmpty()) {
                            AssistChip(
                                onClick = onShowMessages,
                                label = {
                                    Text(
                                        "异常 ${plugin.messages.size}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Warning,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp),
                            )
                        }

                        // 卸载按钮
                        AssistChip(
                            onClick = onUninstall,
                            label = { Text("卸载", fontSize = 10.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp),
                        )
                    }
                }
            }

            // 右侧按钮：未安装显示安装
            if (!isInstalled) {
                FilledTonalButton(
                    onClick = onInstall,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Text("安装", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * 插件异常消息弹窗
 */
@Composable
private fun PluginMessagesDialog(
    plugin: com.sillygirl.client.data.model.PluginInfo,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("异常收集 - ${plugin.title.ifBlank { plugin.id }}")
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(plugin.messages.sortedByDescending { it.unix }) { message ->
                    val timeStr = formatMessageTime(message.unix)
                    val isWarn = message.messageClass == "warn"
                    val dotColor = if (isWarn) Color(0xFFFAAD14) else MaterialTheme.colorScheme.error

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 时间线圆点
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            // 时间
                            Text(
                                timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // 版本标签
                            if (message.version.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (message.version == plugin.version)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer,
                                ) {
                                    Text(
                                        message.version,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = if (message.version == plugin.version)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                            // 内容
                            if (message.content.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    message.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isWarn) Color(0xFFD48806) else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onClear,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("清空")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

/**
 * 格式化消息时间戳
 */
private fun formatMessageTime(unix: Long): String {
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

/**
 * 分页控件
 */
@Composable
private fun PaginationWidget(
    currentPage: Int,
    totalPages: Int,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 上一页按钮
            FilledTonalButton(
                onClick = onPrevPage,
                enabled = currentPage > 1,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("上一页", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.width(16.dp))

            // 页码信息
            Text(
                "$currentPage / $totalPages",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.width(16.dp))

            // 下一页按钮
            FilledTonalButton(
                onClick = onNextPage,
                enabled = currentPage < totalPages,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text("下一页", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ===== 插件详情页面 =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailScreen(
    plugin: PluginRoute,
    onBack: () -> Unit,
    onUninstalled: () -> Unit,
    viewModel: MyPluginsViewModel = viewModel(),
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }
    var editorContent by remember { mutableStateOf("") }
    var currentPlugin by remember { mutableStateOf(plugin) }
    var showUninstallDialog by remember { mutableStateOf(false) }

    LaunchedEffect(plugin.path) {
        viewModel.loadPluginContent(plugin.path)
        viewModel.loadPluginDetail(plugin.path)
    }

    LaunchedEffect(detailState.snackbarMessage) {
        val msg = detailState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearDetailSnackbar()
    }

    LaunchedEffect(detailState.content) {
        if (detailState.content.isNotEmpty() && editorContent.isEmpty()) {
            editorContent = detailState.content
        }
    }

    LaunchedEffect(detailState.pluginDetail) {
        detailState.pluginDetail?.let { currentPlugin = it }
    }

    // 卸载确认对话框
    if (showUninstallDialog) {
        AlertDialog(
            onDismissRequest = { showUninstallDialog = false },
            title = { Text("卸载插件") },
            text = { Text("确定要卸载「${currentPlugin.title.ifBlank { currentPlugin.name }}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUninstallDialog = false
                        viewModel.uninstallPlugin(plugin.path, onUninstalled)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("卸载") }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text(currentPlugin.title.ifBlank { currentPlugin.name }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { showUninstallDialog = true }) {
                        Icon(Icons.Filled.DeleteOutline, "卸载", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { viewModel.reloadPlugin(plugin.path) }) {
                        Icon(Icons.Filled.Refresh, "重载")
                    }
                    IconButton(onClick = { showEditor = !showEditor }) {
                        Icon(if (showEditor) Icons.Filled.Visibility else Icons.Filled.Code, if (showEditor) "预览" else "编辑")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            detailState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            detailState.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(detailState.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.loadPluginContent(plugin.path) }) {
                            Text("重试")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 插件信息卡片
                    item {
                        PluginInfoCard(
                            plugin = currentPlugin,
                            debug = currentPlugin.debug,
                            disable = currentPlugin.disable,
                            isToggling = detailState.isToggling,
                            onToggleDebug = {
                                viewModel.toggleDebug(plugin.path, !currentPlugin.debug)
                            },
                            onToggleDisable = {
                                viewModel.toggleDisable(plugin.path, !currentPlugin.disable)
                            }
                        )
                    }

                    // 表单配置 — 只要有解析到的表单字段就显示
                    if (detailState.formFields.isNotEmpty()) {
                        item {
                            PluginFormCard(
                                fields = detailState.formFields,
                                isSaving = detailState.isSaving,
                                onSave = { formData ->
                                    viewModel.savePluginForm(plugin.path, formData)
                                }
                            )
                        }
                    }

                    // 代码编辑器
                    if (showEditor) {
                        item {
                            PluginEditorCard(
                                content = editorContent,
                                onContentChange = { editorContent = it },
                                isSaving = detailState.isSaving,
                                onSave = {
                                    viewModel.updatePluginContent(plugin.path, editorContent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginInfoCard(
    plugin: PluginRoute,
    debug: Boolean,
    disable: Boolean,
    isToggling: Boolean,
    onToggleDebug: () -> Unit,
    onToggleDisable: () -> Unit,
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 头部：图标 + 标题 + 运行状态
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).themeShadow(4.dp, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        plugin.icon.isBlank() -> Icon(
                            Icons.Filled.Extension, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        isIconUrl(plugin.icon) -> AsyncImage(
                            model = plugin.icon,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                        )
                        else -> Text(plugin.icon, fontSize = 24.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        plugin.title.ifBlank { plugin.name },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "by ${plugin.author.ifBlank { "未知作者" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 运行状态指示
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    disable -> DangerColor
                                    plugin.running -> SuccessColor
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                }
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            disable -> "已禁用"
                            plugin.running -> "运行中"
                            else -> "已停止"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            disable -> DangerColor
                            plugin.running -> SuccessColor
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            // 描述
            if (plugin.description.isNotBlank()) {
                Text(
                    plugin.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 标签行
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        plugin.version,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (plugin.origin.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            plugin.origin,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                if (plugin.classes.isNotEmpty()) {
                    plugin.classes.take(3).forEach { cls ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                cls,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // 调试模式
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("调试模式", style = MaterialTheme.typography.bodyMedium)
                    Text("开启后输出详细日志", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isToggling) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Switch(checked = debug, onCheckedChange = { onToggleDebug() })
                }
            }

            // 禁用模式
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("禁用插件", style = MaterialTheme.typography.bodyMedium)
                    Text("禁用后插件停止运行", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isToggling) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Switch(
                        checked = disable,
                        onCheckedChange = { onToggleDisable() },
                        colors = SwitchDefaults.colors(checkedTrackColor = DangerColor),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginFormCard(
    fields: List<com.sillygirl.client.data.model.PluginFormField>,
    isSaving: Boolean,
    onSave: (Map<String, Any?>) -> Unit,
) {
    var formData by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }

    LaunchedEffect(fields) {
        formData = fields.associate { it.key to it.value }
    }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("插件配置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    "${fields.size} 个配置项",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            fields.forEach { field ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 标题行（包含必填标记）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            field.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        if (field.required) {
                            Text(
                                " *",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    // 提示信息
                    if (field.tooltip.isNotBlank()) {
                        Text(
                            field.tooltip,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    when (field.type) {
                        "switch" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (field.tooltip.isBlank()) {
                                    Text(field.label, style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(
                                    checked = formData[field.key] as? Boolean ?: false,
                                    onCheckedChange = { newValue ->
                                        formData = formData.toMutableMap().apply { put(field.key, newValue) }
                                    }
                                )
                            }
                        }
                        "select" -> {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                            ) {
                                OutlinedTextField(
                                    value = formData[field.key]?.toString() ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    field.options.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                formData = formData.toMutableMap().apply { put(field.key, option.value) }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        "number" -> {
                            OutlinedTextField(
                                value = formData[field.key]?.toString() ?: "",
                                onValueChange = { newValue ->
                                    formData = formData.toMutableMap().apply { put(field.key, newValue.toIntOrNull() ?: 0) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("请输入数字", style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                        else -> {
                            OutlinedTextField(
                                value = formData[field.key]?.toString() ?: "",
                                onValueChange = { newValue ->
                                    formData = formData.toMutableMap().apply { put(field.key, newValue) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                placeholder = { Text("请输入${field.label}", style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Button(
                onClick = { onSave(formData) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("保存配置")
                }
            }
        }
    }
}

@Composable
private fun PluginEditorCard(
    content: String,
    onContentChange: (String) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("插件代码", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text("${content.length} 字符", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 代码高亮编辑器
            CodeEditor(
                code = content,
                onCodeChange = onContentChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp),
            )

            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("保存代码")
                }
            }
        }
    }
}

/**
 * 简单的代码编辑器，支持 JavaScript 语法高亮
 */
@Composable
private fun CodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val syntaxColors = remember { SyntaxColors() }

    OutlinedTextField(
        value = code,
        onValueChange = onCodeChange,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        ),
        visualTransformation = JavaScriptHighlightTransformation(syntaxColors),
    )
}

/**
 * JavaScript 语法高亮颜色配置
 */
private class SyntaxColors(
    val keyword: Color = Color(0xFFC678DD),      // 紫色 - 关键字
    val string: Color = Color(0xFF98C379),         // 绿色 - 字符串
    val comment: Color = Color(0xFF5C6370),        // 灰色 - 注释
    val number: Color = Color(0xFFD19A66),         // 橙色 - 数字
    val function: Color = Color(0xFF61AFEF),       // 蓝色 - 函数
    val operator: Color = Color(0xFF56B6C2),       // 青色 - 运算符
    val punctuation: Color = Color(0xFFABB2BF),    // 浅灰 - 标点
    val property: Color = Color(0xFFE06C75),       // 红色 - 属性名
)

/**
 * JavaScript 语法高亮转换器
 */
private class JavaScriptHighlightTransformation(
    private val colors: SyntaxColors,
) : VisualTransformation {

    // JavaScript 关键字
    private val keywords = setOf(
        "async", "await", "break", "case", "catch", "class", "const",
        "continue", "debugger", "default", "delete", "do", "else",
        "export", "extends", "finally", "for", "from", "function",
        "get", "if", "import", "in", "instanceof", "let", "new",
        "of", "return", "set", "static", "super", "switch", "this",
        "throw", "try", "typeof", "var", "void", "while", "with", "yield"
    )

    // 内置对象和方法
    private val builtins = setOf(
        "console", "document", "window", "Math", "JSON", "Promise",
        "Array", "Object", "String", "Number", "Boolean", "Date",
        "RegExp", "Error", "setTimeout", "setInterval", "clearTimeout",
        "clearInterval", "parseInt", "parseFloat", "isNaN", "undefined",
        "null", "true", "false", "NaN", "Infinity"
    )

    override fun filter(text: AnnotatedString): TransformedText {
        return try {
            val annotatedString = highlightJavaScript(text.text)
            TransformedText(annotatedString, OffsetMapping.Identity)
        } catch (e: Exception) {
            // 语法高亮出错时回退到原始文本，防止闪退
            TransformedText(text, OffsetMapping.Identity)
        }
    }

    private fun highlightJavaScript(code: String): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < code.length) {
                val c = code[i]

                // 单行注释 //
                if (c == '/' && i + 1 < code.length && code[i + 1] == '/') {
                    val start = i
                    while (i < code.length && code[i] != '\n') i++
                    withStyle(SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic)) {
                        append(code.substring(start, i))
                    }
                    continue
                }

                // 多行注释 /* */
                if (c == '/' && i + 1 < code.length && code[i + 1] == '*') {
                    val start = i
                    i += 2
                    while (i < code.length - 1 && !(code[i] == '*' && code[i + 1] == '/')) i++
                    i = (i + 2).coerceAtMost(code.length) // skip */ or clamp to end
                    withStyle(SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic)) {
                        append(code.substring(start, i))
                    }
                    continue
                }

                // 字符串 (单引号、双引号、反引号)
                if (c == '\'' || c == '"' || c == '`') {
                    val quote = c
                    val start = i
                    i++
                    while (i < code.length && code[i] != quote) {
                        if (code[i] == '\\' && i + 1 < code.length) i++ // 跳过转义字符
                        i++
                    }
                    if (i < code.length) i++ // 跳过结束引号
                    withStyle(SpanStyle(color = colors.string)) {
                        append(code.substring(start, i))
                    }
                    continue
                }

                // 数字
                if (c.isDigit()) {
                    val start = i
                    while (i < code.length && (code[i].isDigit() || code[i] == '.')) i++
                    withStyle(SpanStyle(color = colors.number)) {
                        append(code.substring(start, i))
                    }
                    continue
                }

                // 标识符 (关键字、内置对象、函数名)
                if (c.isLetter() || c == '_' || c == '$') {
                    val start = i
                    while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_' || code[i] == '$')) i++
                    val word = code.substring(start, i)

                    when {
                        word in keywords -> {
                            withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Medium)) {
                                append(word)
                            }
                        }
                        word in builtins -> {
                            withStyle(SpanStyle(color = colors.keyword)) {
                                append(word)
                            }
                        }
                        // 检查是否是函数调用 (后面跟着括号)
                        i < code.length && code[i] == '(' -> {
                            withStyle(SpanStyle(color = colors.function)) {
                                append(word)
                            }
                        }
                        // 检查是否是对象属性 (前面是点号)
                        start > 0 && code[start - 1] == '.' -> {
                            withStyle(SpanStyle(color = colors.property)) {
                                append(word)
                            }
                        }
                        else -> {
                            append(word)
                        }
                    }
                    continue
                }

                // 运算符
                if (c in "+-*/%=!<>&|^~?:") {
                    withStyle(SpanStyle(color = colors.operator)) {
                        append(c)
                    }
                    i++
                    continue
                }

                // 标点符号
                if (c in "(){}[];,.") {
                    withStyle(SpanStyle(color = colors.punctuation)) {
                        append(c)
                    }
                    i++
                    continue
                }

                // 其他字符
                append(c)
                i++
            }
        }
    }
}
