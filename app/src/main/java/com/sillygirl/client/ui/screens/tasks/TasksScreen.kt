package com.sillygirl.client.ui.screens.tasks

import androidx.compose.foundation.layout.*
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.TaskActionRequest
import com.sillygirl.client.data.model.TaskInfo
import com.sillygirl.client.data.model.TaskSetEnableRequest
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
import java.util.*
import com.sillygirl.client.ui.components.MiniAppBar

data class TasksUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val tasks: List<TaskInfo> = emptyList(),
    val snackbarMessage: String? = null,
)

class TasksViewModel : ViewModel() {
    private val repo = TaskRepository()
    private val _ui = MutableStateFlow(TasksUiState())
    val ui: StateFlow<TasksUiState> = _ui.asStateFlow()

    init { load() }
    fun load(showRefreshHint: Boolean = false) { viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true)
        repo.getTasks().fold(
            onSuccess = { _ui.value = _ui.value.copy(isLoading = false, tasks = it, snackbarMessage = if (showRefreshHint) "已刷新" else null) },
            onFailure = { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
        )
    }}

    fun toggleTask(task: TaskInfo) { viewModelScope.launch {
        try {
            RetrofitClient.api.setTaskEnable(TaskSetEnableRequest(id = task.id.toString(), enable = !task.enable))
            load()
            _ui.value = _ui.value.copy(snackbarMessage = if (task.enable) "已停止" else "已启动")
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "操作失败：${e.message}")
        }
    }}

    fun deleteTask(task: TaskInfo) { viewModelScope.launch {
        try {
            RetrofitClient.api.delTask(TaskActionRequest(id = task.id.toString()))
            load()
            _ui.value = _ui.value.copy(snackbarMessage = "已删除")
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "删除失败：${e.message}")
        }
    }}

    fun runTask(task: TaskInfo) { viewModelScope.launch {
        try {
            RetrofitClient.api.runTask(TaskActionRequest(id = task.id.toString()))
            _ui.value = _ui.value.copy(snackbarMessage = "任务已执行")
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "执行失败：${e.message}")
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
    val snackbarHostState = remember { SnackbarHostState() }

    var taskToDelete by remember { mutableStateOf<TaskInfo?>(null) }
    var taskToRun by remember { mutableStateOf<TaskInfo?>(null) }

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
                actions = { IconButton(onClick = { viewModel.load(showRefreshHint = true) }) { Icon(Icons.Filled.Refresh, "刷新") } }
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
                        TaskItemCard(task, onToggle = { viewModel.toggleTask(task) }, onRun = { taskToRun = task }, onDelete = { taskToDelete = task })
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItemCard(task: TaskInfo, onToggle: () -> Unit, onRun: () -> Unit, onDelete: () -> Unit) {
    val firstIcon = task.icons.firstOrNull()?.link
    val hasIcon = !firstIcon.isNullOrBlank()

    GlassCard {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 图标：有icon用AsyncImage，无icon用PlayArrow
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
