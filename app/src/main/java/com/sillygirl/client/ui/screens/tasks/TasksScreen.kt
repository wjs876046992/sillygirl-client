package com.sillygirl.client.ui.screens.tasks

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.TaskInfo
import com.sillygirl.client.data.repository.TaskRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.*

data class TasksUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val tasks: List<TaskInfo> = emptyList(),
)

class TasksViewModel : ViewModel() {
    private val repo = TaskRepository()
    private val _ui = MutableStateFlow(TasksUiState())
    val ui: StateFlow<TasksUiState> = _ui.asStateFlow()

    init { load() }
    fun load() { viewModelScope.launch {
        _ui.value = TasksUiState(isLoading = true)
        repo.getTasks().fold(
            onSuccess = { _ui.value = TasksUiState(tasks = it) },
            onFailure = { _ui.value = TasksUiState(isLoading = false, error = it.message) }
        )
    }}

    fun toggleTask(task: TaskInfo) { viewModelScope.launch {
        try {
            RetrofitClient.api.setTaskEnable(mapOf("id" to task.id.toString(), "enable" to !task.enable))
            load()
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "操作失败：${e.message}")
        }
    }}

    fun deleteTask(task: TaskInfo) { viewModelScope.launch {
        try {
            RetrofitClient.api.delTask(mapOf("id" to task.id.toString()))
            load()
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "删除失败：${e.message}")
        }
    }}

    fun runTask(task: TaskInfo) { viewModelScope.launch {
        try {
            RetrofitClient.api.runTask(mapOf("id" to task.id.toString()))
        } catch (_: Exception) {}
    }}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    viewModel: TasksViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("定时任务") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { viewModel.load() }) { Icon(Icons.Filled.Refresh, "刷新") } }
            )
        }
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
                LazyColumn(Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ui.tasks) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { viewModel.toggleTask(task) },
                            onRun = { viewModel.runTask(task) },
                            onDelete = { viewModel.deleteTask(task) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskInfo, onToggle: () -> Unit, onRun: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(task.title.ifBlank { task.taskId }, style = MaterialTheme.typography.titleSmall)
                    Text(task.schedule, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = task.enable, onCheckedChange = { onToggle() })
            }
            if (task.command.isNotBlank()) {
                Text(task.command, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(onClick = onRun, modifier = Modifier.height(28.dp)) { Text("执行", style = MaterialTheme.typography.labelSmall) }
                FilledTonalButton(onClick = onDelete, modifier = Modifier.height(28.dp), colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}
