package com.sillygirl.client.ui.screens.service

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.repository.AuthRepository
import com.sillygirl.client.data.repository.ServerConfig
import com.sillygirl.client.ui.components.GlassCard
import com.sillygirl.client.ui.theme.DangerColor
import com.sillygirl.client.ui.theme.SuccessColor
import com.sillygirl.client.ui.theme.themeShadow
import com.sillygirl.client.ui.components.MiniAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScreen(
    onBack: () -> Unit,
    onServerSwitched: () -> Unit,
    serverConfig: ServerConfig,
) {
    val servers = remember { mutableStateOf(serverConfig.getServers()) }
    val currentUrl = remember { mutableStateOf(RetrofitClient.currentServerUrl()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var serverToSwitch by remember { mutableStateOf<Pair<Int, ServerConfig.ServerInfo>?>(null) }
    var serverToDelete by remember { mutableStateOf<Pair<Int, ServerConfig.ServerInfo>?>(null) }
    var serverToEdit by remember { mutableStateOf<Pair<Int, ServerConfig.ServerInfo>?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val authRepo = remember { AuthRepository() }
    var isSwitching by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // 切换确认
    serverToSwitch?.let { (idx, server) ->
        AlertDialog(
            onDismissRequest = { if (!isSwitching) serverToSwitch = null },
            title = { Text("切换服务器") },
            text = {
                if (isSwitching) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        if (server.requiresAuth) {
                            Text("正在登录「${server.displayName}」...")
                        } else {
                            Text("正在连接「${server.displayName}」...")
                        }
                    }
                } else {
                    if (server.requiresAuth) {
                        Text("确定要切换到「${server.displayName}」吗？当前会话将退出。")
                    } else {
                        Text("确定要切换到「${server.displayName}」吗？（无需认证）")
                    }
                }
            },
            confirmButton = {
                if (!isSwitching) {
                    TextButton(onClick = {
                        scope.launch {
                            isSwitching = true
                            // 保存当前服务器地址，失败时恢复
                            val originalUrl = currentUrl.value

                            if (!server.requiresAuth) {
                                // 无需认证，直接切换
                                RetrofitClient.setServer(server.url)
                                RetrofitClient.token = null
                                serverConfig.setDefaultIndex(idx)
                                serverConfig.clearToken()
                                currentUrl.value = server.url
                                isSwitching = false
                                serverToSwitch = null
                                onServerSwitched()
                            } else {
                                // 需要认证，尝试自动登录
                                val result = authRepo.login(server.url, server.username, server.password)
                                result.fold(
                                    onSuccess = {
                                        serverConfig.setDefaultIndex(idx)
                                        serverConfig.saveToken(RetrofitClient.token ?: "")
                                        currentUrl.value = server.url
                                        isSwitching = false
                                        serverToSwitch = null
                                        onServerSwitched()
                                    },
                                    onFailure = { e ->
                                        // 登录失败，恢复到原来的服务器
                                        if (originalUrl != null) {
                                            RetrofitClient.setServer(originalUrl)
                                        }
                                        isSwitching = false
                                        serverToSwitch = null
                                        snackbarMessage = "登录失败: ${e.message}"
                                    }
                                )
                            }
                        }
                    }) { Text("切换") }
                }
            },
            dismissButton = {
                if (!isSwitching) {
                    TextButton(onClick = { serverToSwitch = null }) { Text("取消") }
                }
            },
        )
    }

    // 删除确认
    serverToDelete?.let { (idx, server) ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text("删除服务器") },
            text = { Text("确定要删除「${server.displayName}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        serverConfig.removeServer(idx)
                        servers.value = serverConfig.getServers()
                        serverToDelete = null
                        snackbarMessage = "已删除"
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { serverToDelete = null }) { Text("取消") } },
        )
    }

    // 添加服务器
    if (showAddDialog) {
        AddServiceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { info ->
                try {
                    serverConfig.addServer(info)
                    servers.value = serverConfig.getServers()
                    showAddDialog = false
                    snackbarMessage = "已添加"
                } catch (e: Exception) {
                    snackbarMessage = e.message
                }
            },
        )
    }

    // 编辑服务器
    serverToEdit?.let { (idx, server) ->
        EditServiceDialog(
            server = server,
            onDismiss = { serverToEdit = null },
            onSave = { updated ->
                try {
                    serverConfig.updateServer(idx, updated)
                    servers.value = serverConfig.getServers()
                    // 如果编辑的是当前服务器，更新 currentUrl
                    if (server.url == currentUrl.value) {
                        currentUrl.value = updated.url
                        RetrofitClient.setServer(updated.url)
                    }
                    serverToEdit = null
                    snackbarMessage = "已保存"
                } catch (e: Exception) {
                    snackbarMessage = e.message
                }
            },
        )
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text("服务管理") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "添加服务器")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (servers.value.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Dns, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("暂无服务器", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("点击右上角 + 添加", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 当前连接提示
                item {
                    Text(
                        "当前连接：${currentUrl ?: "无"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                itemsIndexed(servers.value) { index, server ->
                    val isCurrent = server.url == currentUrl.value
                    ServiceCard(
                        server = server,
                        isCurrent = isCurrent,
                        onSwitch = { serverToSwitch = index to server },
                        onEdit = { serverToEdit = index to server },
                        onDelete = { serverToDelete = index to server },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    server: ServerConfig.ServerInfo,
    isCurrent: Boolean,
    onSwitch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).themeShadow(4.dp, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Dns, null,
                    tint = if (isCurrent) SuccessColor else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(server.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (!server.requiresAuth) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text("免登录", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    if (isCurrent) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = SuccessColor.copy(alpha = 0.12f)) {
                            Text("当前", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = SuccessColor, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
                        }
                    }
                }
                Text(server.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!isCurrent) {
                FilledTonalButton(
                    onClick = onSwitch,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("切换", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, "编辑", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, "删除", tint = DangerColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddServiceDialog(
    onDismiss: () -> Unit,
    onAdd: (ServerConfig.ServerInfo) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var requiresAuth by remember { mutableStateOf(true) }  // 默认需要认证

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加服务器") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("http://192.168.1.18:3000") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = alias, onValueChange = { alias = it },
                    label = { Text("别名（可选）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = !requiresAuth,
                        onCheckedChange = { requiresAuth = !it },
                    )
                    Text("无需认证（直接访问）")
                }
                if (requiresAuth) {
                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text("用户名") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("密码") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        onAdd(ServerConfig.ServerInfo(
                            url = url,
                            username = username,
                            password = password,
                            alias = alias,
                            requiresAuth = requiresAuth,
                        ))
                    }
                },
                enabled = url.isNotBlank(),
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditServiceDialog(
    server: ServerConfig.ServerInfo,
    onDismiss: () -> Unit,
    onSave: (ServerConfig.ServerInfo) -> Unit,
) {
    var url by remember { mutableStateOf(server.url) }
    var username by remember { mutableStateOf(server.username) }
    var password by remember { mutableStateOf(server.password) }
    var alias by remember { mutableStateOf(server.alias) }
    var showPassword by remember { mutableStateOf(false) }
    var requiresAuth by remember { mutableStateOf(server.requiresAuth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑服务器") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("http://192.168.1.18:3000") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = alias, onValueChange = { alias = it },
                    label = { Text("别名（可选）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = !requiresAuth,
                        onCheckedChange = { requiresAuth = !it },
                    )
                    Text("无需认证（直接访问）")
                }
                if (requiresAuth) {
                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text("用户名") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("密码") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        onSave(ServerConfig.ServerInfo(
                            url = url,
                            username = username,
                            password = password,
                            alias = alias,
                            requiresAuth = requiresAuth,
                        ))
                    }
                },
                enabled = url.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
