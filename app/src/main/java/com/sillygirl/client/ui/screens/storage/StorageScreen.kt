package com.sillygirl.client.ui.screens.storage


import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import com.sillygirl.client.data.api.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

data class StorageUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val keys: List<String> = emptyList(),
    val selectedValue: String? = null,
    val selectedKey: String? = null,
    val snackbarMessage: String? = null,
)

class StorageViewModel : ViewModel() {
    private val _ui = MutableStateFlow(StorageUiState())
    val ui: StateFlow<StorageUiState> = _ui.asStateFlow()

    fun loadKeys(showRefreshHint: Boolean = false) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            try {
                val resp = RetrofitClient.api.getStorage("__keys__")
                if (resp.success) {
                    _ui.value = _ui.value.copy(isLoading = false, keys = (try { @Suppress("UNCHECKED_CAST") (resp.data as List<String>) } catch (_: Exception) { emptyList() }), error = null, snackbarMessage = if (showRefreshHint) "已刷新" else null)
                } else {
                    _ui.value = _ui.value.copy(isLoading = false, error = resp.errorMessage ?: "加载失败")
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(isLoading = false, error = "加载失败: ${e.message}")
            }
        }
    }

    fun loadValue(key: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.getStorage(key)
                val value = if (resp.success) Gson().toJson(resp.data) else resp.errorMessage ?: "无数据"
                _ui.value = _ui.value.copy(selectedKey = key, selectedValue = value, isLoading = false)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = "加载失败: ${e.message}")
            }
        }
    }

    fun saveValue(key: String, value: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.saveStorage(key, mapOf("value" to value))
                _ui.value = _ui.value.copy(selectedKey = null, selectedValue = null, snackbarMessage = "保存成功", error = null)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = "保存失败: ${e.message}")
            }
        }
    }

    fun clearSnackbar() {
        _ui.value = _ui.value.copy(snackbarMessage = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    onBack: () -> Unit,
    viewModel: StorageViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var keyInput by remember { mutableStateOf("") }
    var valueInput by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }

    // 保存确认对话框
    if (showSaveDialog) {
        val key = ui.selectedKey
        val value = valueInput.ifBlank { ui.selectedValue }
        if (key != null && value != null) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("保存确认") },
                text = {
                    Column {
                        Text("确定要保存以下键值吗？")
                        Spacer(Modifier.height(8.dp))
                        Text("Key: $key", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Value: ${value.take(100)}${if (value.length > 100) "..." else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 5,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.saveValue(key, value)
                        showSaveDialog = false
                    }) { Text("保存") }
                },
                dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("取消") } },
            )
        }
    }

    LaunchedEffect(ui.snackbarMessage) {
        val msg = ui.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text("存储管理") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { viewModel.loadKeys(showRefreshHint = true) }) { Icon(Icons.Filled.Refresh, "刷新") } }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("输入 Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                FilledTonalButton(
                    onClick = {
                        if (keyInput.isNotBlank()) viewModel.loadValue(keyInput.trim())
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp),
                ) {
                    Icon(Icons.Filled.Search, "搜索")
                }
            }

            if (ui.selectedValue != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(ui.selectedKey ?: "unknown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = valueInput.ifBlank { ui.selectedValue!! },
                            onValueChange = { valueInput = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            maxLines = 20,
                            shape = RoundedCornerShape(10.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { showSaveDialog = true },
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("保存") }
                            OutlinedButton(
                                onClick = { keyInput = ""; valueInput = "" },
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("清除") }
                        }
                    }
                }
            }

            if (ui.error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(ui.error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
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
