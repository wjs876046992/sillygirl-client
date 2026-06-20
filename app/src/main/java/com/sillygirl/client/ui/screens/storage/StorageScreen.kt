package com.sillygirl.client.ui.screens.storage

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson

data class StorageUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val keys: List<String> = emptyList(),
    val selectedValue: String? = null,
    val selectedKey: String? = null,
)

class StorageViewModel : ViewModel() {
    private val _ui = MutableStateFlow(StorageUiState())
    val ui: StateFlow<StorageUiState> = _ui.asStateFlow()

    fun loadKeys() { viewModelScope.launch {
        _ui.value = StorageUiState(isLoading = true)
        try {
            val resp = RetrofitClient.api.getStorage("__keys__")
            _ui.value = StorageUiState(
                keys = emptyList(), // will parse from response
                error = if (resp.success) null else "加载失败"
            )
        } catch (e: Exception) {
            // the storage API returns an object, we'll just show raw value below
            _ui.value = StorageUiState(isLoading = false, error = "加载失败: ${e.message}")
        }
    }}

    fun loadValue(key: String) { viewModelScope.launch {
        try {
            val resp = RetrofitClient.api.getStorage(key)
            val value = if (resp.success) {
                Gson().toJson(resp.data)
            } else {
                resp.errorMessage ?: "无数据"
            }
            _ui.value = _ui.value.copy(selectedKey = key, selectedValue = value)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "加载失败: ${e.message}")
        }
    }}

    fun saveValue(key: String, value: String) { viewModelScope.launch {
        try {
            RetrofitClient.api.saveStorage(key, mapOf("value" to value))
            _ui.value = _ui.value.copy(selectedKey = null, selectedValue = null)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "保存失败: ${e.message}")
        }
    }}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    onBack: () -> Unit,
    viewModel: StorageViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var keyInput by remember { mutableStateOf("") }
    var valueInput by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("存储") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { viewModel.loadKeys() }) { Icon(Icons.Filled.Refresh, "刷新") } }
            )
        }
    ) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Key input + search
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("输入 Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        if (keyInput.isNotBlank()) viewModel.loadValue(keyInput.trim())
                    }) { Icon(Icons.Filled.Search, "查询") }
                }
            )

            // Value display
            if (ui.selectedValue != null) {
                OutlinedTextField(
                    value = valueInput.ifBlank { ui.selectedValue!! },
                    onValueChange = { valueInput = it },
                    label = { Text(ui.selectedKey ?: "值") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    maxLines = 20,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        val k = ui.selectedKey ?: return@FilledTonalButton
                        viewModel.saveValue(k, valueInput.ifBlank { ui.selectedValue!! })
                    }) { Text("保存") }
                    OutlinedButton(onClick = { keyInput = ""; valueInput = ""; ui.selectedValue.let { _ -> } }) { Text("清除") }
                }
            }

            if (ui.error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(ui.error!!, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}
