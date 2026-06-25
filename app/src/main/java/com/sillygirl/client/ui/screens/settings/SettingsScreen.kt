package com.sillygirl.client.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.sillygirl.client.LocalServerConfig
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.repository.ServerConfig
import com.sillygirl.client.ui.screens.settings.SettingsViewModelFactory
import com.sillygirl.client.ui.theme.DangerColor
import com.sillygirl.client.ui.theme.PrimaryGradientColors
import com.sillygirl.client.ui.theme.themeShadow
import com.sillygirl.client.ui.components.MiniAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(LocalServerConfig.current)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？需要重新选择服务器并登录。") },
            confirmButton = {
                FilledTonalButton(
                    onClick = { showLogoutDialog = false; viewModel.logout(onLogout) },
                    shape = RoundedCornerShape(12.dp),
                ) { Text("确定") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }, shape = RoundedCornerShape(12.dp)) { Text("取消") }
            },
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.isLoggingOut, uiState.logoutDone) {
        if (uiState.isLoggingOut && uiState.logoutDone) {
            scope.launch {
                snackbarHostState.showSnackbar("已退出登录")
            }
            onLogout()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    Scaffold(
        topBar = {
            MiniAppBar(title = { Text("设置") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } })
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(14.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 服务器信息卡片
            Card(
                modifier = Modifier.fillMaxWidth().themeShadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("服务器", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Dns, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(uiState.serverUrl, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // 基础配置卡片
            Card(
                modifier = Modifier.fillMaxWidth().themeShadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("基础配置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    if (uiState.configData.isNotEmpty()) {
                        // 登录账号
                        ConfigTextField(
                            label = "登录账号",
                            tooltip = "机器人名称",
                            value = uiState.editingConfig["app.name"] ?: "",
                            onValueChange = { viewModel.updateConfigValue("app.name", it) },
                            onDone = { viewModel.saveConfig("app.name") },
                        )

                        // 登录密码
                        ConfigTextField(
                            label = "登录密码",
                            tooltip = "主要用于管理员页面登录",
                            value = uiState.editingConfig["app.password"] ?: "",
                            onValueChange = { viewModel.updateConfigValue("app.password", it) },
                            onDone = { viewModel.saveConfig("app.password") },
                            isPassword = true,
                        )

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))

                        // 启动时间
                        ConfigTextField(
                            label = "启动时间",
                            value = uiState.editingConfig["app.started_at"] ?: "",
                            onValueChange = {},
                            enabled = false,
                        )

                        // 编译版本
                        ConfigTextField(
                            label = "编译版本",
                            value = uiState.editingConfig["app.compiled_at"] ?: "",
                            onValueChange = {},
                            enabled = false,
                        )

                        // 机器码
                        ConfigTextField(
                            label = "机器码",
                            value = uiState.editingConfig["app.machine_id"] ?: "",
                            onValueChange = {},
                            enabled = false,
                        )

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))

                        // 端口
                        ConfigTextField(
                            label = "端口",
                            tooltip = "请谨慎修改，如果因此无法访问请重新运行程序并修改端口。",
                            value = uiState.editingConfig["app.port"] ?: "",
                            onValueChange = { viewModel.updateConfigValue("app.port", it) },
                            onDone = { viewModel.saveConfig("app.port") },
                        )

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))

                        // 默认存储
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("默认存储", style = MaterialTheme.typography.bodyMedium)
                            Row {
                                FilterChip(
                                    selected = (uiState.editingConfig["app.storage"] ?: "boltdb") == "boltdb",
                                    onClick = {
                                        viewModel.updateConfigValue("app.storage", "boltdb")
                                        viewModel.saveConfig("app.storage")
                                    },
                                    label = { Text("BoltDB") },
                                )
                                Spacer(Modifier.width(8.dp))
                                FilterChip(
                                    selected = (uiState.editingConfig["app.storage"] ?: "boltdb") == "redis",
                                    onClick = {
                                        viewModel.updateConfigValue("app.storage", "redis")
                                        viewModel.saveConfig("app.storage")
                                    },
                                    label = { Text("Redis") },
                                )
                            }
                        }

                        // Redis 配置（仅当存储选择 Redis 时显示）
                        if ((uiState.editingConfig["app.storage"] ?: "boltdb") == "redis") {
                            ConfigTextField(
                                label = "Redis地址",
                                value = uiState.editingConfig["app.redis_addr"] ?: "",
                                onValueChange = { viewModel.updateConfigValue("app.redis_addr", it) },
                                onDone = { viewModel.saveConfig("app.redis_addr") },
                            )

                            ConfigTextField(
                                label = "Redis密码",
                                value = uiState.editingConfig["app.redis_password"] ?: "",
                                onValueChange = { viewModel.updateConfigValue("app.redis_password", it) },
                                onDone = { viewModel.saveConfig("app.redis_password") },
                                isPassword = true,
                            )
                        }
                    } else if (!uiState.isLoading) {
                        Text(
                            "暂无配置数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 操作按钮卡片
            Card(
                modifier = Modifier.fillMaxWidth().themeShadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("操作", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    // 一键升级按钮
                    Button(
                        onClick = { viewModel.upgrade() },
                        enabled = !uiState.isUpgrading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (uiState.isUpgrading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("升级中...")
                        } else {
                            Icon(Icons.Filled.SystemUpdate, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("一键升级")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // 重启按钮
                    OutlinedButton(
                        onClick = { viewModel.restart() },
                        enabled = !uiState.isRestarting,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (uiState.isRestarting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("重启中...")
                        } else {
                            Icon(Icons.Filled.RestartAlt, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("重启程序")
                        }
                    }
                }
            }

            // 关于卡片
            Card(
                modifier = Modifier.fillMaxWidth().themeShadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("关于", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).themeShadow(4.dp, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🤖", fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("SillyGirl 客户端", style = MaterialTheme.typography.bodyMedium)
                            Text("v1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.fillMaxWidth())

            // 退出登录按钮
            Button(
                onClick = { showLogoutDialog = true },
                enabled = !uiState.isLoggingOut,
                modifier = Modifier.fillMaxWidth().height(50.dp).themeShadow(6.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DangerColor),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("退出登录", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun ConfigTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDone: (() -> Unit)? = null,
    tooltip: String? = null,
    enabled: Boolean = true,
    isPassword: Boolean = false,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        trailingIcon = {
            if (tooltip != null) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = tooltip,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        singleLine = true,
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                onDone?.invoke()
            }
        ),
    )
}
