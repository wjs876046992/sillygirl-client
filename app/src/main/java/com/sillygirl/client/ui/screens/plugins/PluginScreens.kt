package com.sillygirl.client.ui.screens.plugins

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.sillygirl.client.ui.components.GlassCard
import com.sillygirl.client.ui.theme.*
import com.sillygirl.client.data.model.PluginRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPluginsScreen(
    plugins: List<PluginRoute>,
    onBack: () -> Unit,
    onPluginClick: (PluginRoute) -> Unit,
    viewModel: MyPluginsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(plugins) {
        viewModel.loadPlugins(plugins)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    // 收集所有分类
    val allCategories = remember(uiState.plugins) {
        uiState.plugins.flatMap { it.classes }.distinct().sorted()
    }

    // 过滤插件
    val filteredPlugins = remember(uiState.plugins, searchQuery, selectedCategory) {
        uiState.plugins.filter { plugin ->
            val matchesSearch = searchQuery.isBlank() ||
                plugin.title.contains(searchQuery, ignoreCase = true) ||
                plugin.name.contains(searchQuery, ignoreCase = true) ||
                plugin.description.contains(searchQuery, ignoreCase = true) ||
                plugin.author.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || plugin.classes.contains(selectedCategory)
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text("我的插件 (${uiState.plugins.size})") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { viewModel.loadPlugins(plugins, showRefreshHint = true) }) { Icon(Icons.Filled.Refresh, "刷新") } }
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
                LazyColumn(
                    Modifier.fillMaxSize().padding(p),
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

                    // 分类筛选
                    if (allCategories.isNotEmpty()) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text("全部") },
                                    shape = RoundedCornerShape(20.dp),
                                )
                                allCategories.take(5).forEach { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { selectedCategory = if (selectedCategory == category) null else category },
                                        label = { Text(category) },
                                        shape = RoundedCornerShape(20.dp),
                                    )
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
                            MyPluginCard(plugin, onClick = { onPluginClick(plugin) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginMarketScreen(
    onBack: () -> Unit,
    viewModel: PluginMarketViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text("插件市场") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { viewModel.load(showRefreshHint = true) }) { Icon(Icons.Filled.Refresh, "刷新") } }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { p ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.plugins.isEmpty() -> Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Store, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("暂无可用插件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(p),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.plugins) { plugin ->
                        MarketPluginCard(plugin, onInstall = { viewModel.installPlugin(plugin.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(Modifier.width(8.dp))
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                title()
            }
            actions()
        }
    }
}

@Composable
private fun MyPluginCard(plugin: PluginRoute, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            // 图标 + 运行状态指示点
            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).shadow(4.dp, RoundedCornerShape(10.dp)),
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

                // 底部信息行：版本 + 作者 + 分类标签
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(plugin.version, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        plugin.author.ifBlank { plugin.origin.ifBlank { "未知" } },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
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

@Composable
private fun MarketPluginCard(plugin: com.sillygirl.client.data.model.PluginInfo, onInstall: () -> Unit) {
    GlassCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).shadow(4.dp, RoundedCornerShape(10.dp)),
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
            Column(Modifier.fillMaxWidth()) {
                Text(plugin.title.ifBlank { plugin.id }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (plugin.description.isNotBlank()) {
                    Text(plugin.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Text("${plugin.author} · ${plugin.downloads}次下载 · v${plugin.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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

                    // 表单配置
                    if (currentPlugin.hasForm && detailState.formFields.isNotEmpty()) {
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
                    modifier = Modifier.size(48.dp).shadow(4.dp, RoundedCornerShape(12.dp)),
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("插件配置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)

            fields.forEach { field ->
                when (field.type) {
                    "switch" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(field.label, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = formData[field.key] as? Boolean ?: false,
                                onCheckedChange = { newValue ->
                                    formData = formData.toMutableMap().apply { put(field.key, newValue) }
                                }
                            )
                        }
                    }
                    "select" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(field.label, style = MaterialTheme.typography.labelMedium)
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
                    }
                    "number" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(field.label, style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = formData[field.key]?.toString() ?: "",
                                onValueChange = { newValue ->
                                    formData = formData.toMutableMap().apply { put(field.key, newValue.toIntOrNull() ?: 0) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(field.label, style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = formData[field.key]?.toString() ?: "",
                                onValueChange = { newValue ->
                                    formData = formData.toMutableMap().apply { put(field.key, newValue) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onSave(formData) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
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

            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodySmall,
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
