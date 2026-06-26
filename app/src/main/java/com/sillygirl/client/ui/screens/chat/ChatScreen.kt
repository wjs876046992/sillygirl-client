package com.sillygirl.client.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillygirl.client.data.model.NameLabel
import com.sillygirl.client.ui.components.GlassCard
import com.sillygirl.client.ui.components.MiniAppBar
import com.sillygirl.client.ui.theme.PrimaryGradientColors
import com.sillygirl.client.ui.theme.SuccessColor
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    onBack: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MiniAppBar(
                title = {
                    Text("发送消息", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (uiState.isLoading && uiState.selects == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp)),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 目标选择卡片
                GlassCard {
                    Text(
                        "目标",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))

                    // 平台选择
                    val platforms = uiState.selects?.platforms?.keys?.sorted() ?: emptyList()
                    DropdownSelector(
                        label = "平台",
                        options = platforms.map { it to it },
                        selected = uiState.selectedPlatform,
                        onSelected = { viewModel.selectPlatform(it) },
                        placeholder = "选择平台",
                    )

                    Spacer(Modifier.height(10.dp))

                    // Bot 选择
                    val bots = uiState.selects?.platforms?.get(uiState.selectedPlatform) ?: emptyList()
                    DropdownSelector(
                        label = "机器人",
                        options = bots.map { it to it },
                        selected = uiState.selectedBot,
                        onSelected = { viewModel.selectBot(it) },
                        placeholder = "选择机器人（可选）",
                        enabled = uiState.selectedPlatform != null,
                    )

                    Spacer(Modifier.height(10.dp))

                    // 目标类型切换
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !uiState.isGroup,
                            onClick = { viewModel.setTargetType(false) },
                            label = { Text("私聊") },
                            leadingIcon = if (!uiState.isGroup) {
                                { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                        FilterChip(
                            selected = uiState.isGroup,
                            onClick = { viewModel.setTargetType(true) },
                            label = { Text("群聊") },
                            leadingIcon = if (uiState.isGroup) {
                                { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // 用户/群选择
                    if (uiState.isGroup) {
                        val groups = uiState.filteredGroups
                        Column {
                            if (uiState.selectedPlatform != null) {
                                Text(
                                    "共 ${groups.size} 个群组",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            SearchableDropdown(
                                label = "目标群",
                                options = groups,
                                selected = uiState.selectedChatId,
                                onSelected = { viewModel.selectChatId(it) },
                                placeholder = if (uiState.selectedPlatform == null) "请先选择平台" else "搜索群组名称或ID...",
                            )
                        }
                    } else {
                        val users = uiState.filteredUsers
                        Column {
                            if (uiState.selectedPlatform != null) {
                                Text(
                                    "共 ${users.size} 个用户",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            SearchableDropdown(
                                label = "目标用户",
                                options = users,
                                selected = uiState.selectedUserId,
                                onSelected = { viewModel.selectUserId(it) },
                                placeholder = if (uiState.selectedPlatform == null) "请先选择平台" else "搜索用户名称或ID...",
                            )
                        }
                    }
                }

                // 消息输入卡片
                GlassCard {
                    Text(
                        "消息",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.message,
                        onValueChange = { viewModel.updateMessage(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入要发送的消息...") },
                        minLines = 3,
                        maxLines = 8,
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                // 发送按钮
                Button(
                    onClick = { viewModel.sendMessage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !uiState.isSending
                        && uiState.selectedPlatform != null
                        && uiState.message.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGradientColors[0],
                    ),
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("发送中...", color = Color.White)
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("发送", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // 数据统计
                if (uiState.selects != null && uiState.selectedPlatform != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "已筛选: ${if (uiState.isGroup) "${uiState.filteredGroups.size} 个群组" else "${uiState.filteredUsers.size} 个用户"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // 发送结果
                if (uiState.lastResult != null) {
                    val result = uiState.lastResult!!
                    Surface(
                        color = SuccessColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = SuccessColor,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "发送成功",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessColor,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "平台: ${result.platform}  ·  Bot: ${result.botId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (result.userId.isNotBlank()) {
                                Text(
                                    "用户: ${result.userId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (result.chatId.isNotBlank()) {
                                Text(
                                    "群组: ${result.chatId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun DropdownSelector(
    label: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelected: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val isSelected = selected != null && selected.isNotBlank()
    val displayText = if (isSelected) {
        options.find { it.first == selected }?.second ?: selected
    } else {
        placeholder
    }

    // 动画：箭头旋转
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrow",
    )

    // 动画：选中态边框颜色
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        animationSpec = tween(durationMillis = 200),
        label = "border",
    )

    // 动画：选中态背景色
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 200),
        label = "bg",
    )

    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { expanded = true },
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
                border = BorderStroke(1.dp, borderColor),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 选中时显示圆点指示器
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = displayText,
                        modifier = Modifier.weight(1f),
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(arrowRotation).size(22.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 下拉菜单
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .widthIn(min = 220.dp)
                    .padding(horizontal = 4.dp),
            ) {
                // 清除选项
                if (isSelected) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "清除选择",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            onSelected("")
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }

                // 选项列表
                options.forEachIndexed { index, (value, label) ->
                    val isThisSelected = value == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                color = if (isThisSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isThisSelected) FontWeight.SemiBold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        },
                        leadingIcon = {
                            if (isThisSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Spacer(Modifier.size(18.dp))
                            }
                        },
                    )
                }

                // 空选项
                if (options.isEmpty()) {
                    Text(
                        "  暂无选项",
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchableDropdown(
    label: String,
    options: List<NameLabel>,
    selected: String?,
    onSelected: (String) -> Unit,
    placeholder: String,
) {
    var showDialog by remember { mutableStateOf(false) }
    val isSelected = selected != null && selected.isNotBlank()
    val displayText = if (isSelected) {
        options.find { it.value == selected }?.label ?: selected
    } else {
        placeholder
    }

    // 动画：选中态边框颜色
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        animationSpec = tween(durationMillis = 200),
        label = "border",
    )

    // 动画：选中态背景色
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 200),
        label = "bg",
    )

    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            shape = RoundedCornerShape(12.dp),
            color = bgColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 选中时显示圆点指示器
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = displayText,
                    modifier = Modifier.weight(1f),
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                )
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "搜索",
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // 搜索对话框
    if (showDialog) {
        SearchDialog(
            title = label,
            options = options,
            selected = selected,
            onSelected = {
                onSelected(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun SearchDialog(
    title: String,
    options: List<NameLabel>,
    selected: String?,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    // 搜索过滤
    val filtered = if (searchQuery.isBlank()) {
        options.take(50)
    } else {
        options.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.value.contains(searchQuery, ignoreCase = true)
        }.take(30)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("🔍 输入名称或ID搜索...") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
                )
                Spacer(Modifier.height(8.dp))
                // 数据统计
                Text(
                    if (searchQuery.isBlank()) {
                        if (options.size > 50) "共 ${options.size} 项，显示前 50 项" else "共 ${options.size} 项"
                    } else {
                        "匹配 ${filtered.size} / ${options.size} 项"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                // 选项列表（使用 Column 而非 LazyColumn，避免与 AlertDialog 内部的 verticalScroll 嵌套崩溃）
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    // 清除选择选项
                    if (selected != null && selected.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text("清除选择", color = MaterialTheme.colorScheme.error) },
                            onClick = { onSelected("") },
                        )
                    }
                    // 选项
                    for (item in filtered) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        item.label,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    if (item.value != item.label.split("(").firstOrNull()?.trim() && item.value.isNotBlank()) {
                                        Text(
                                            "ID: ${item.value}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            onClick = { onSelected(item.value) },
                        )
                    }
                    // 无匹配结果
                    if (filtered.isEmpty() && searchQuery.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text("未找到匹配项", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {},
                            enabled = false,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}
