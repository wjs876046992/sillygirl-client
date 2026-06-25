package com.sillygirl.client.ui.screens.masters

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.MasterAddRequest
import com.sillygirl.client.data.model.MasterDelRequest
import com.sillygirl.client.data.model.MasterInfo
import com.sillygirl.client.data.repository.MasterRepository
import com.sillygirl.client.ui.components.GlassCard
import com.sillygirl.client.ui.theme.DangerColor
import com.sillygirl.client.ui.theme.themeShadow
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.sillygirl.client.ui.components.MiniAppBar

data class MastersUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val masters: List<MasterInfo> = emptyList(),
    val platforms: List<String> = emptyList(),
    val snackbarMessage: String? = null,
)

class MastersViewModel : ViewModel() {
    private val repo = MasterRepository()
    private val _ui = MutableStateFlow(MastersUiState())
    val ui: StateFlow<MastersUiState> = _ui.asStateFlow()

    init { load() }
    fun load(showRefreshHint: Boolean = false) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true)
            val result = repo.getMasters()
            result.fold(
                onSuccess = { r -> _ui.value = _ui.value.copy(isLoading = false, masters = r, platforms = emptyList(), snackbarMessage = if (showRefreshHint) "已刷新" else null) },
                onFailure = { e -> _ui.value = _ui.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun removeMaster(master: MasterInfo) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.delMaster(MasterDelRequest(id = "${master.id}"))
                load()
                _ui.value = _ui.value.copy(snackbarMessage = "已移除管理员")
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = "移除失败：${e.message}")
            }
        }
    }

    fun clearSnackbar() {
        _ui.value = _ui.value.copy(snackbarMessage = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MastersScreen(
    onBack: () -> Unit,
    viewModel: MastersViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.snackbarMessage) {
        val msg = ui.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    var showAdd by remember { mutableStateOf(false) }
    var masterToDelete by remember { mutableStateOf<MasterInfo?>(null) }

    // 删除确认对话框
    masterToDelete?.let { master ->
        AlertDialog(
            onDismissRequest = { masterToDelete = null },
            title = { Text("移除管理员") },
            text = { Text("确定要移除「${master.nickname.ifBlank { master.number }}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.removeMaster(master); masterToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("移除") }
            },
            dismissButton = { TextButton(onClick = { masterToDelete = null }) { Text("取消") } },
        )
    }

    if (showAdd) {
        AddMasterDialog(
            onDismiss = { showAdd = false },
            onAdded = { showAdd = false; viewModel.load() },
        )
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text("管理员") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { viewModel.load(showRefreshHint = true) }) { Icon(Icons.Filled.Refresh, "刷新") }
                    IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "添加") }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                LazyColumn(
                    Modifier.fillMaxSize().padding(p),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(ui.masters) { master ->
                        MasterCard(master, onRemove = { masterToDelete = master })
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterCard(master: MasterInfo, onRemove: () -> Unit) {
    GlassCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).themeShadow(4.dp, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Person, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(master.nickname.ifBlank { master.number }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${master.platform} · ${master.number}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, "移除", tint = DangerColor)
            }
        }
    }
}

@Composable
private fun AddMasterDialog(onDismiss: () -> Unit, onAdded: () -> Unit) {
    var platform by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加管理员") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = platform, onValueChange = { platform = it }, label = { Text("平台") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("账号") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (platform.isBlank() || number.isBlank()) return@TextButton
                    loading = true
                    scope.launch {
                        try {
                            RetrofitClient.api.addMaster(MasterAddRequest(platform = platform, number = number))
                            onAdded()
                        } catch (_: Exception) { loading = false }
                    }
                },
                enabled = !loading,
            ) {
                Text(if (loading) "添加中..." else "添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
