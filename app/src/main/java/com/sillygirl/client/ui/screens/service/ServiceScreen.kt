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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.repository.ServerConfig
import com.sillygirl.client.ui.components.GlassCard
import com.sillygirl.client.ui.theme.DangerColor
import com.sillygirl.client.ui.theme.SuccessColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScreen(
    onBack: () -> Unit,
    onServerSwitched: () -> Unit,
    serverConfig: ServerConfig,
) {
    val servers = remember { mutableStateOf(serverConfig.getServers()) }
    val currentUrl = RetrofitClient.currentServerUrl()
    var showAddDialog by remember { mutableStateOf(false) }
    var serverToSwitch by remember { mutableStateOf<Pair<Int, ServerConfig.ServerInfo>?>(null) }
    var serverToDelete by remember { mutableStateOf<Pair<Int, ServerConfig.ServerInfo>?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // 切换确认
    serverToSwitch?.let { (idx, server) ->
        AlertDialog(
            onDismissRequest = { serverToSwitch = null },
            title = { Text("切换服务器") },
            text = { Text("确定要切换到「${server.displayName}」吗？当前会话将退出。") },
            confirmButton = {
                TextButton(onClick = {
                    serverConfig.setDefaultIndex(idx)
                    // 设置新服务器地址到 RetrofitClient，让登录页能连上
                    RetrofitClient.setServer(server.url)
                    serverConfig.clearToken()
                    onServerSwitched()
                    serverToSwitch = null
                }) { Text("切换") }
            },
            dismissButton = { TextButton(onClick = { serverToSwitch = null }) { Text("取消") } },
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
                    val isCurrent = server.url == currentUrl
                    ServiceCard(
                        server = server,
                        isCurrent = isCurrent,
                        onSwitch = { serverToSwitch = index to server },
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
    onDelete: () -> Unit,
) {
    GlassCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).shadow(4.dp, RoundedCornerShape(10.dp)),
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        onAdd(ServerConfig.ServerInfo(url = url, username = username, password = password, alias = alias))
                    }
                },
                enabled = url.isNotBlank(),
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
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
