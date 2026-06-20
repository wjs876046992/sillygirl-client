package com.sillygirl.client.ui.screens.masters

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
import com.sillygirl.client.data.model.MasterInfo
import com.sillygirl.client.data.repository.MasterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

data class MastersUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val masters: List<MasterInfo> = emptyList(),
    val platforms: List<String> = emptyList(),
)

class MastersViewModel : ViewModel() {
    private val repo = MasterRepository()
    private val _ui = MutableStateFlow(MastersUiState())
    val ui: StateFlow<MastersUiState> = _ui.asStateFlow()

    init { load() }
    fun load() { viewModelScope.launch {
        _ui.value = MastersUiState(isLoading = true)
        repo.getMasters().fold(
            onSuccess = { r ->
                _ui.value = MastersUiState(masters = r, platforms = emptyList())
            },
            onFailure = { _ui.value = MastersUiState(isLoading = false, error = it.message) }
        )
    }}

    fun removeMaster(master: MasterInfo) { viewModelScope.launch {
        try {
            RetrofitClient.api.delMaster(mapOf("id" to "${master.id}"))
            load()
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "移除失败：${e.message}")
        }
    }}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MastersScreen(
    onBack: () -> Unit,
    viewModel: MastersViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        AddMasterDialog(
            onDismiss = { showAdd = false },
            onAdded = { showAdd = false; viewModel.load() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理员") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Default.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { viewModel.load() }) { Icon(Icons.Filled.Refresh, "刷新") }
                    IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "添加") }
                }
            )
        }
    ) { p ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ui.masters.isEmpty() -> Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.People, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("暂无管理员")
                }
            }
            else -> {
                LazyColumn(Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ui.masters) { master ->
                        MasterCard(master, onRemove = { viewModel.removeMaster(master) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterCard(master: MasterInfo, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(master.nickname.ifBlank { master.number }, style = MaterialTheme.typography.titleSmall)
                Text("${master.platform} · ${master.number}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) { Icon(Icons.Filled.RemoveCircleOutline, "移除", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AddMasterDialog(onDismiss: () -> Unit, onAdded: () -> Unit) {
    var platform by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加管理员") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = platform, onValueChange = { platform = it }, label = { Text("平台") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("账号") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (platform.isBlank() || number.isBlank()) return@TextButton
                loading = true
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        RetrofitClient.api.addMaster(mapOf("platform" to platform, "number" to number))
                        onAdded()
                    } catch (_: Exception) { loading = false }
                }
            }, enabled = !loading) { Text(if (loading) "添加中..." else "添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
