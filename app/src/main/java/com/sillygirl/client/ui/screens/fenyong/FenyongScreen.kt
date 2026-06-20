package com.sillygirl.client.ui.screens.fenyong

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FenyongScreen(
    onBack: () -> Unit = {},
    viewModel: FenyongViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分佣") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData(uiState.activeTab) }) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            // Tab bar
            if (uiState.tabs.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.tabs.indexOfFirst { it.key == uiState.activeTab }
                        .coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 16.dp,
                ) {
                    uiState.tabs.forEach { tab ->
                        Tab(
                            selected = tab.key == uiState.activeTab,
                            onClick = { viewModel.switchTab(tab.key) },
                            text = { Text(tab.title) },
                        )
                    }
                }
            }

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.orders.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { viewModel.loadData(uiState.activeTab) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Stats summary
                        uiState.dashboard?.let { dash ->
                            item {
                                StatsSummaryCard(dash)
                            }
                        }

                        // Order list
                        items(uiState.orders) { order ->
                            OrderCard(order)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsSummaryCard(dashboard: com.sillygirl.client.data.model.FenyongDashboardResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("数据统计", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem("今日", "¥${formatMoney(dashboard.today.actual)}")
                StatItem("昨日", "¥${formatMoney(dashboard.yesterday.actual)}")
                StatItem("7天", "¥${formatMoney(dashboard.last7days.actual)}")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem("总订单", dashboard.totalOrders.toString())
                StatItem("已结算", "¥${formatMoney(dashboard.totalSettled)}")
                StatItem("未结算", "¥${formatMoney(dashboard.totalUnsettled)}")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun OrderCard(order: com.sillygirl.client.data.model.FenyongOrder) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        order.name.take(1),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(order.name, style = MaterialTheme.typography.bodyMedium)
                order.skuName.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AssistChip(
                onClick = {},
                label = { Text(order.status) },
            )
        }
    }
}

private fun formatNumber(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.CHINA).format(value)

private fun formatMoney(value: Double): String =
    String.format("%.2f", value)
