package com.sillygirl.client.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.*
import com.sillygirl.client.data.repository.TaskRepository
import com.sillygirl.client.ui.components.GlassCard
import com.sillygirl.client.ui.theme.*
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillygirl.client.ui.components.MiniAppBar
import java.util.UUID

data class TasksUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val tasks: List<TaskInfo> = emptyList(),
    val snackbarMessage: String? = null,
)

data class EditTaskData(
    val taskId: String = "",
    val title: String = "",
    val schedule: String = "",
    val command: String = "",
    val remark: String = "",
    val scripts: List<String> = emptyList(),
    val senders: List<TaskSender> = emptyList(),
)

class TasksViewModel : ViewModel() {
    private val repo = TaskRepository()
    private val _ui = MutableStateFlow(TasksUiState())
    val ui: StateFlow<TasksUiState> = _ui.asStateFlow()

    // selects 数据
    private val _selects = MutableStateFlow<TaskSelectsData?>(null)
    val selects: StateFlow<TaskSelectsData?> = _selects.asStateFlow()

    init { load() }

    fun load(showRefreshHint: Boolean = false) { viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true)
        repo.getTasks().fold(
            onSuccess = { _ui.value = _ui.value.copy(isLoading = false, tasks = it, snackbarMessage = if (showRefreshHint) "已刷新" else null) },
            onFailure = { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
        )
    }}

    fun loadSelects(taskId: String = "") { viewModelScope.launch {
        try {
            val resp = RetrofitClient.api.getTaskSelects(taskId)
            if (resp.success) _selects.value = resp.data
        } catch (_: Exception) {}
    }}

    fun toggleTask(task: TaskInfo) { viewModelScope.launch {
        try {
            val body = mutableMapOf<String, Any>(
                "task_id" to task.taskId,
                "enable" to !task.enable,
            )
            RetrofitClient.api.saveTask(body)
            load()
            _ui.value = _ui.value.copy(snackbarMessage = if (task.enable) "已停止" else "已启动")
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "操作失败：${e.message}")
        }
    }}

    fun deleteTask(task: TaskInfo) { viewModelScope.launch {
        try {
            RetrofitClient.api.deleteTask(mapOf("task_id" to task.taskId))
            load()
            _ui.value = _ui.value.copy(snackbarMessage = "已删除")
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "删除失败：${e.message}")
        }
    }}

    fun runTask(task: TaskInfo) { viewModelScope.launch {
        try {
            RetrofitClient.api.runTask(task.taskId)
            _ui.value = _ui.value.copy(snackbarMessage = "任务已执行")
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "执行失败：${e.message}")
        }
    }}

    fun saveTask(data: EditTaskData, onDone: (Boolean, String?) -> Unit) { viewModelScope.launch {
        try {
            val body = mutableMapOf<String, Any>(
                "task_id" to data.taskId.ifBlank { UUID.randomUUID().toString() },
                "title" to data.title,
                "schedule" to data.schedule,
                "command" to data.command,
                "remark" to data.remark,
                "scripts" to data.scripts,
                "senders" to data.senders.map { sender ->
                    mutableMapOf<String, String>().apply {
                        if (sender.platform.isNotEmpty()) put("platform", sender.platform)
                        if (sender.chatId.isNotEmpty()) put("chat_id", sender.chatId)
                        if (sender.userId.isNotEmpty()) put("user_id", sender.userId)
                        if (sender.botId.isNotEmpty()) put("bot_id", sender.botId)
                    }
                },
            )
            RetrofitClient.api.saveTask(body)
            load()
            _ui.value = _ui.value.copy(snackbarMessage = "已保存")
            onDone(true, null)
        } catch (e: Exception) {
            onDone(false, e.message ?: "保存失败")
        }
    }}

    fun clearSnackbar() {
        _ui.value = _ui.value.copy(snackbarMessage = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    viewModel: TasksViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val selects by viewModel.selects.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var taskToDelete by remember { mutableStateOf<TaskInfo?>(null) }
    var taskToRun by remember { mutableStateOf<TaskInfo?>(null) }
    var taskToEdit by remember { mutableStateOf<EditTaskData?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var editError by remember { mutableStateOf<String?>(null) }

    // 打开编辑弹窗时加载 selects
    LaunchedEffect(taskToEdit) {
        if (taskToEdit != null) {
            viewModel.loadSelects(taskToEdit?.taskId ?: "")
        }
    }

    // 删除确认
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("删除任务") },
            text = { Text("确定要删除「${task.title.ifBlank { task.taskId }}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteTask(task); taskToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { taskToDelete = null }) { Text("取消") } },
        )
    }

    // 执行确认
    taskToRun?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToRun = null },
            title = { Text("执行任务") },
            text = { Text("确定要立即执行「${task.title.ifBlank { task.taskId }}」吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.runTask(task); taskToRun = null }) { Text("执行") }
            },
            dismissButton = { TextButton(onClick = { taskToRun = null }) { Text("取消") } },
        )
    }

    // 编辑/添加任务弹窗
    taskToEdit?.let { data ->
        TaskEditDialog(
            data = data,
            selects = selects,
            isSaving = isSaving,
            editError = editError,
            onDismiss = { if (!isSaving) { taskToEdit = null; editError = null } },
            onSave = { newData ->
                isSaving = true
                editError = null
                viewModel.saveTask(newData) { ok, msg ->
                    isSaving = false
                    if (ok) { taskToEdit = null } else { editError = msg }
                }
            },
            onUpdate = { taskToEdit = it },
        )
    }

    LaunchedEffect(ui.snackbarMessage) {
        val msg = ui.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text("定时任务") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { taskToEdit = EditTaskData() }) { Icon(Icons.Filled.Add, "添加") }
                    IconButton(onClick = { viewModel.load(showRefreshHint = true) }) { Icon(Icons.Filled.Refresh, "刷新") }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { p ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ui.tasks.isEmpty() -> Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("暂无定时任务")
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(p),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(ui.tasks) { task ->
                        TaskItemCard(
                            task,
                            onToggle = { viewModel.toggleTask(task) },
                            onRun = { taskToRun = task },
                            onEdit = {
                                taskToEdit = EditTaskData(
                                    taskId = task.taskId,
                                    title = task.title,
                                    schedule = task.schedule,
                                    command = task.command,
                                    remark = task.remark,
                                    scripts = task.scripts,
                                    senders = task.senders,
                                )
                            },
                            onDelete = { taskToDelete = task },
                        )
                    }
                }
            }
        }
    }
}

// ====== 编辑弹窗 ======

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditDialog(
    data: EditTaskData,
    selects: TaskSelectsData?,
    isSaving: Boolean,
    editError: String?,
    onDismiss: () -> Unit,
    onSave: (EditTaskData) -> Unit,
    onUpdate: (EditTaskData) -> Unit,
) {
    var expandedPlatform by remember { mutableStateOf(false) }
    var expandedScript by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (data.taskId.isBlank()) "添加任务" else "编辑任务") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 标题
                OutlinedTextField(
                    value = data.title,
                    onValueChange = { onUpdate(data.copy(title = it)) },
                    label = { Text("任务名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 计划时间
                OutlinedTextField(
                    value = data.schedule,
                    onValueChange = { onUpdate(data.copy(schedule = it)) },
                    label = { Text("Cron 表达式") },
                    placeholder = { Text("例: */5 * * * *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 命令
                OutlinedTextField(
                    value = data.command,
                    onValueChange = { onUpdate(data.copy(command = it)) },
                    label = { Text("执行命令") },
                    placeholder = { Text("发送给机器人的消息内容") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 执行脚本选择
                if (selects != null && selects.scripts.isNotEmpty()) {
                    Text("执行脚本", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    ExposedDropdownMenuBox(
                        expanded = expandedScript,
                        onExpandedChange = { expandedScript = it },
                    ) {
                        OutlinedTextField(
                            value = if (data.scripts.isEmpty()) "未选择" else data.scripts.joinToString(", ") { uuid ->
                                selects.scripts[uuid] ?: uuid
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("选择脚本（可多选）") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedScript) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = expandedScript,
                            onDismissRequest = { expandedScript = false },
                        ) {
                            selects.scripts.forEach { (uuid, title) ->
                                val selected = uuid in data.scripts
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = selected, onCheckedChange = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(title.ifBlank { uuid })
                                        }
                                    },
                                    onClick = {
                                        val newScripts = if (selected) data.scripts - uuid else data.scripts + uuid
                                        onUpdate(data.copy(scripts = newScripts))
                                    },
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 发送环境
                Text("执行环境", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)

                if (data.senders.isEmpty()) {
                    // 默认添加一个空 sender
                    SideEffect {
                        onUpdate(data.copy(senders = listOf(TaskSender())))
                    }
                }

                data.senders.forEachIndexed { index, sender ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("环境 ${index + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                if (data.senders.size > 1) {
                                    IconButton(onClick = {
                                        onUpdate(data.copy(senders = data.senders.toMutableList().also { it.removeAt(index) }))
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Filled.Close, "删除", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // 平台选择
                            if (selects != null && selects.platforms.isNotEmpty()) {
                                var expandedPlat by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = expandedPlat,
                                    onExpandedChange = { expandedPlat = it },
                                ) {
                                    OutlinedTextField(
                                        value = sender.platform.ifBlank { "选择平台" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("平台") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedPlat) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedPlat,
                                        onDismissRequest = { expandedPlat = false },
                                    ) {
                                        selects.platforms.keys.forEach { platform ->
                                            DropdownMenuItem(
                                                text = { Text(platform) },
                                                onClick = {
                                                    onUpdate(data.copy(senders = data.senders.toMutableList().also {
                                                        it[index] = it[index].copy(platform = platform, chatId = "", userId = "")
                                                    }))
                                                    expandedPlat = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            // Bot ID 选择（如果平台有多个 bot）
                            val botIds = selects?.platforms?.get(sender.platform) ?: emptyList()
                            if (botIds.size > 1) {
                                var expandedBot by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = expandedBot,
                                    onExpandedChange = { expandedBot = it },
                                ) {
                                    OutlinedTextField(
                                        value = sender.botId.ifBlank { "选择机器人" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("机器人") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedBot) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedBot,
                                        onDismissRequest = { expandedBot = false },
                                    ) {
                                        botIds.forEach { bid ->
                                            DropdownMenuItem(
                                                text = { Text(bid) },
                                                onClick = {
                                                    onUpdate(data.copy(senders = data.senders.toMutableList().also {
                                                        it[index] = it[index].copy(botId = bid)
                                                    }))
                                                    expandedBot = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            // 群组/用户选择
                            val groupNames = selects?.groupNames ?: emptyList()
                            val userNames = selects?.userNames ?: emptyList()
                            if (groupNames.isNotEmpty() || userNames.isNotEmpty()) {
                                var expandedTarget by remember { mutableStateOf(false) }
                                val currentLabel = when {
                                    sender.chatId.isNotEmpty() -> groupNames.find { it.value == sender.chatId }?.label ?: sender.chatId
                                    sender.userId.isNotEmpty() -> userNames.find { it.value == sender.userId }?.label ?: sender.userId
                                    else -> "选择群组/用户"
                                }
                                ExposedDropdownMenuBox(
                                    expanded = expandedTarget,
                                    onExpandedChange = { expandedTarget = it },
                                ) {
                                    OutlinedTextField(
                                        value = currentLabel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("目标（群组/用户）") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTarget) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedTarget,
                                        onDismissRequest = { expandedTarget = false },
                                    ) {
                                        // 群组
                                        if (groupNames.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("—— 群组 ——", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                onClick = {},
                                            )
                                            groupNames.forEach { g ->
                                                DropdownMenuItem(
                                                    text = { Text(g.label) },
                                                    onClick = {
                                                        onUpdate(data.copy(senders = data.senders.toMutableList().also {
                                                            it[index] = it[index].copy(chatId = g.value, userId = "")
                                                        }))
                                                        expandedTarget = false
                                                    },
                                                )
                                            }
                                        }
                                        // 用户
                                        if (userNames.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("—— 用户 ——", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                onClick = {},
                                            )
                                            userNames.forEach { u ->
                                                DropdownMenuItem(
                                                    text = { Text(u.label) },
                                                    onClick = {
                                                        onUpdate(data.copy(senders = data.senders.toMutableList().also {
                                                            it[index] = it[index].copy(userId = u.value, chatId = "")
                                                        }))
                                                        expandedTarget = false
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 添加发送环境按钮
                OutlinedButton(
                    onClick = { onUpdate(data.copy(senders = data.senders + TaskSender())) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加发送环境")
                }

                HorizontalDivider()

                // 备注
                OutlinedTextField(
                    value = data.remark,
                    onValueChange = { onUpdate(data.copy(remark = it)) },
                    label = { Text("备注") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!editError.isNullOrBlank()) {
                    Text(editError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(data) },
                enabled = !isSaving && data.schedule.isNotBlank(),
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("取消") }
        },
    )
}

// ====== 卡片 ======

@Composable
private fun TaskItemCard(task: TaskInfo, onToggle: () -> Unit, onRun: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val firstIcon = task.icons.firstOrNull()?.link
    val hasIcon = !firstIcon.isNullOrBlank()

    GlassCard {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).themeShadow(4.dp, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (hasIcon) {
                        AsyncImage(
                            model = firstIcon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (task.enable) SuccessColor.copy(alpha = 0.15f) else DangerColor.copy(alpha = 0.15f)),
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow, null,
                                modifier = Modifier.size(14.dp),
                                tint = if (task.enable) SuccessColor else DangerColor,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title.ifBlank { task.taskId }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(task.schedule, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = task.enable, onCheckedChange = { onToggle() })
            }
            if (task.command.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    task.command,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 显示脚本标签
            if (task.scripts.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "脚本: ${task.scripts.size} 个",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            // 显示发送环境
            if (task.senders.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "环境: ${task.senders.size} 个",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onRun,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("执行", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("编辑", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerColor),
                ) {
                    Text("删除", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
