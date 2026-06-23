package com.sillygirl.client.ui.screens.plugins

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillygirl.client.ui.components.GlassCard
import com.sillygirl.client.ui.theme.DangerColor
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

    LaunchedEffect(plugins) {
        viewModel.loadPlugins(plugins)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text("我的插件") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { viewModel.loadPlugins(plugins) }) { Icon(Icons.Filled.Refresh, "刷新") } }
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
                    items(uiState.plugins) { plugin ->
                        MyPluginCard(plugin, onClick = { onPluginClick(plugin) })
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
                actions = { IconButton(onClick = { viewModel.load() }) { Icon(Icons.Filled.Refresh, "刷新") } }
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).shadow(4.dp, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (plugin.icon.isNotBlank()) {
                    Text(plugin.icon, fontSize = 20.sp)
                } else {
                    Icon(
                        Icons.Filled.Extension, null,
                        tint = if (plugin.running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plugin.title.ifBlank { plugin.name }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(6.dp))
                    if (plugin.debug) {
                        Text("调试", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (plugin.hasForm) {
                        Text("表单", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                if (plugin.description.isNotBlank()) {
                    Text(plugin.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(plugin.version, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(plugin.author.ifBlank { plugin.origin.ifBlank { "未知" } }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
                Icon(
                    Icons.Filled.Store, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
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
    viewModel: MyPluginsViewModel = viewModel(),
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }
    var editorContent by remember { mutableStateOf("") }
    var currentPlugin by remember { mutableStateOf(plugin) }

    LaunchedEffect(plugin.path) {
        viewModel.loadPluginContent(plugin.path)
        // 从plugins/list.json获取完整的插件信息
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

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text(currentPlugin.title.ifBlank { currentPlugin.name }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
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
    onToggleDebug: () -> Unit,
    onToggleDisable: () -> Unit,
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 头部：图标 + 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).shadow(4.dp, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (plugin.icon.isNotBlank()) {
                        Text(plugin.icon, fontSize = 24.sp)
                    } else {
                        Icon(
                            Icons.Filled.Extension, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
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
                Text("调试模式", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = debug, onCheckedChange = { onToggleDebug() })
            }

            // 禁用模式
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("禁用模式", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = disable, onCheckedChange = { onToggleDisable() })
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(field.label, style = MaterialTheme.typography.labelMedium)

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
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
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
