package com.sillygirl.client.ui.screens.storage


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.model.StorageBucket
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillygirl.client.ui.components.MiniAppBar
import com.sillygirl.client.ui.theme.themeShadow

data class StorageUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val buckets: List<StorageBucket> = emptyList(),
    val selectedBucket: String? = null,
    val bucketData: Map<String, Any> = emptyMap(),
    val snackbarMessage: String? = null,
)

class StorageViewModel : ViewModel() {
    private val _ui = MutableStateFlow(StorageUiState())
    val ui: StateFlow<StorageUiState> = _ui.asStateFlow()

    private var allBuckets: List<StorageBucket> = emptyList()

    fun loadAllBuckets() {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.searchStorage("")
                if (resp.success) {
                    allBuckets = resp.data ?: emptyList()
                    _ui.value = _ui.value.copy(buckets = allBuckets)
                }
            } catch (_: Exception) {
                // Ignore errors
            }
        }
    }

    fun filterBuckets(query: String) {
        val filtered = if (query.isBlank()) {
            allBuckets
        } else {
            allBuckets.filter {
                it.text.contains(query, ignoreCase = true) ||
                        it.value.contains(query, ignoreCase = true)
            }
        }
        _ui.value = _ui.value.copy(buckets = filtered)
    }

    fun selectBucket(bucketValue: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null, selectedBucket = bucketValue)
            try {
                val resp = RetrofitClient.api.getStorage(bucketValue)
                if (resp.success) {
                    val data = resp.data
                    val mapData = when (data) {
                        is Map<*, *> -> data.mapKeys { it.key.toString() }.mapValues { it.value ?: "" }
                        is String -> try {
                            val type = object : TypeToken<Map<String, Any>>() {}.type
                            Gson().fromJson<Map<String, Any>>(data, type)
                        } catch (_: Exception) {
                            mapOf("value" to data)
                        }
                        else -> mapOf("value" to (Gson().toJson(data) ?: ""))
                    }
                    _ui.value = _ui.value.copy(isLoading = false, bucketData = mapData, error = null)
                } else {
                    _ui.value = _ui.value.copy(isLoading = false, error = resp.errorMessage ?: "加载失败")
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(isLoading = false, error = "加载失败: ${e.message}")
            }
        }
    }

    fun saveBucketValue(bucket: String, key: String, value: String) {
        viewModelScope.launch {
            try {
                val body = mapOf(key to value)
                val resp = RetrofitClient.api.saveStorage(bucket, body)
                if (resp.success) {
                    _ui.value = _ui.value.copy(snackbarMessage = "保存成功")
                    selectBucket(bucket)
                } else {
                    _ui.value = _ui.value.copy(error = resp.errorMessage ?: "保存失败")
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = "保存失败: ${e.message}")
            }
        }
    }

    fun clearSelection() {
        _ui.value = _ui.value.copy(selectedBucket = null, bucketData = emptyMap())
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
    var searchQuery by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }
    var isDropdownDisabled by remember { mutableStateOf(false) }
    var editingValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()

    // Load all buckets on first launch
    LaunchedEffect(Unit) {
        viewModel.loadAllBuckets()
    }

    // Local filter when search query changes
    LaunchedEffect(searchQuery) {
        viewModel.filterBuckets(searchQuery)
    }

    // Update dropdown visibility based on query and filtered results
    LaunchedEffect(searchQuery, ui.buckets) {
        if (!isDropdownDisabled) {
            showDropdown = searchQuery.isNotBlank() && ui.buckets.isNotEmpty()
        }
    }

    // Initialize editing values when bucket data changes
    LaunchedEffect(ui.bucketData) {
        editingValues = ui.bucketData.mapValues { it.value.toString() }
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
                actions = {
                    if (ui.selectedBucket != null) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.Close, "清除选择")
                        }
                    }
                    IconButton(onClick = {
                        if (ui.selectedBucket != null) {
                            viewModel.selectBucket(ui.selectedBucket!!)
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                }
            )
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Bucket Search Input
            Box {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        isDropdownDisabled = false
                    },
                    label = { Text("搜索 Bucket") },
                    placeholder = { Text("输入 bucket 名称进行搜索") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                showDropdown = false
                                isDropdownDisabled = false
                            }) {
                                Icon(Icons.Filled.Clear, "清除")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                // Dropdown for bucket suggestions
                if (showDropdown && ui.buckets.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 56.dp)
                            .themeShadow(6.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                        ) {
                            items(ui.buckets) { bucket ->
                                ListItem(
                                    headlineContent = { Text(bucket.text) },
                                    supportingContent = {
                                        Text(
                                            bucket.value,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    leadingContent = { Icon(Icons.Filled.Storage, null, tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.clickable {
                                        searchQuery = bucket.text
                                        showDropdown = false
                                        isDropdownDisabled = true
                                        viewModel.selectBucket(bucket.value)
                                    },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Loading indicator
            if (ui.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            // Selected bucket info
            if (ui.selectedBucket != null && !ui.isLoading) {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Storage,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    ui.selectedBucket!!,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                "${ui.bucketData.size} 个字段",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        if (ui.bucketData.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "此 Bucket 为空",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            // Form fields for bucket data
                            ui.bucketData.forEach { (key, value) ->
                                OutlinedTextField(
                                    value = editingValues[key] ?: "",
                                    onValueChange = { newValue ->
                                        editingValues = editingValues.toMutableMap().apply {
                                            put(key, newValue)
                                        }
                                    },
                                    label = { Text(key) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    minLines = 1,
                                    maxLines = 5,
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Save button
                            Button(
                                onClick = {
                                    val bucket = ui.selectedBucket ?: return@Button
                                    // Save all changed values
                                    coroutineScope.launch {
                                        editingValues.forEach { (key, value) ->
                                            if (value != ui.bucketData[key].toString()) {
                                                viewModel.saveBucketValue(bucket, key, value)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(Icons.Filled.Save, null)
                                Spacer(Modifier.width(8.dp))
                                Text("保存所有更改")
                            }
                        }
                    }
                }
            }

            // Empty state
            if (ui.selectedBucket == null && !ui.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Storage,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "输入 Bucket 名称进行搜索",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Error card
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
