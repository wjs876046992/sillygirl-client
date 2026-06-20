package com.sillygirl.client.ui.screens.fenyong

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

// ===== 颜色 =====
private val BlueColor = Color(0xFF1890FF)
private val BlueDark = Color(0xFF1677FF)
private val GreenColor = Color(0xFF52C41A)
private val RedColor = Color(0xFFE60012)
private val OrangeColor = Color(0xFFFF5000)
private val PddColor = Color(0xFFE02A24)
private val PurpleColor = Color(0xFF722ED1)
private val GrayText = Color(0xFF999999)
private val DarkGray = Color(0xFF333333)
private val LightBgGray = Color(0xFFF5F5F5)

// ===== 平台名称 =====
private data class PlatformInfo(
    val code: String,
    val name: String,
    val color: Color,
)

private val PLATFORMS = listOf(
    PlatformInfo("jd", "京东", RedColor),
    PlatformInfo("tb", "淘宝", OrangeColor),
    PlatformInfo("pdd", "拼多多", PddColor),
)

private fun getPlatform(code: String): PlatformInfo? {
    return PLATFORMS.find { it.code == code.lowercase() }
}

private fun getPlatformColor(code: String): Color {
    return getPlatform(code)?.color ?: GrayText
}

private fun getPlatformName(code: String): String {
    return getPlatform(code)?.name ?: code
}

private fun formatOrderTime(ts: Long?): String {
    if (ts == null || ts == 0L) return "—"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        sdf.format(Date(ts * 1000))
    } catch (_: Exception) {
        "—"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FenyongScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: FenyongViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分佣系统") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "退出登录")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                uiState.isLoading && uiState.dashboard == null && uiState.orders.isEmpty() -> {
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
                            OutlinedButton(onClick = { viewModel.loadData() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        // 1. Dashboard Summary
                        uiState.dashboard?.let { dash ->
                            item {
                                FenyongSummaryCard(dash)
                            }
                        }

                        // 2. Search bar
                        item {
                            SearchBarWidget(
                                keyword = uiState.keyword,
                                onKeywordChange = viewModel::setKeyword,
                                onSearch = { viewModel.loadOrders(1) },
                                onClear = { viewModel.clearSearch() },
                            )
                        }

                        // 3. Orders list
                        if (uiState.orders.isEmpty() && !uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("暂无订单", color = GrayText)
                                }
                            }
                        } else {
                            items(uiState.orders) { order ->
                                OrderCard(order)
                            }
                        }

                        // 4. Pagination
                        if (uiState.total > 20) {
                            item {
                                PaginationBar(
                                    currentPage = uiState.page,
                                    total = uiState.total,
                                    onPageChange = viewModel::loadOrders,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== FenyongSummaryCard =====

@Composable
fun FenyongSummaryCard(data: com.sillygirl.client.data.model.FenyongDashboardResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Today's estimated + orders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF667EEA),
                                    Color(0xFF764BA2),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("💰", fontSize = 22.sp)
                }

                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        "今日预估佣金",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayText,
                    )
                    Text(
                        "¥${formatMoney(data.today.estimate)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = BlueDark,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "今日订单",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayText,
                    )
                    Text(
                        "${data.today.orders} 单",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkGray,
                    )
                }
            }

            // Comparison row: yesterday / 7 days / month
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = LightBgGray)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ComparisonItem("昨日", data.yesterday.estimate, data.yesterday.orders)
                ComparisonItem("近7日", data.last7days.estimate, data.last7days.orders)
                ComparisonItem("近月", data.lastMonth.estimate, data.lastMonth.orders)
            }

            // Platform revenue
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = LightBgGray)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "平台收益（今日）",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))

            PLATFORMS.forEach { platform ->
                val stat = data.platforms[platform.code]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(platform.color),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        platform.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${stat?.orders ?: 0}单",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "¥${formatMoney(stat?.estimate ?: 0.0)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BlueDark,
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonItem(label: String, estimate: Double, orders: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = GrayText)
        Text(
            "¥${formatMoney(estimate)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DarkGray,
        )
        Text(
            "${orders}单",
            style = MaterialTheme.typography.labelSmall,
            color = GrayText,
        )
    }
}

// ===== SearchBarWidget =====

@Composable
fun SearchBarWidget(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = keyword,
        onValueChange = onKeywordChange,
        placeholder = { Text("搜索SKU/订单号") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = {
            if (keyword.isNotBlank()) {
                IconButton(onClick = {
                    onClear()
                    onSearch()
                }) {
                    Text("✕", color = GrayText)
                }
            }
        },
        supportingText = {
            TextButton(onClick = {
                onSearch()
            }) {
                Text("搜索")
            }
        },
    )
}

// ===== OrderCard =====

@Composable
fun OrderCard(order: com.sillygirl.client.data.model.FenyongOrder) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Image + Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Image
                if (order.image.isNotBlank()) {
                    AsyncImage(
                        model = order.image,
                        contentDescription = null,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(LightBgGray),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🛒", fontSize = 28.sp)
                    }
                }

                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // SKU name
                    Text(
                        order.skuName.ifBlank { order.name },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Tags row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Platform tag
                        order.site.takeIf { it.isNotBlank() }?.let { site ->
                            AssistChip(
                                onClick = {},
                                label = { Text(getPlatformName(site)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = getPlatformColor(site).copy(alpha = 0.15f),
                                    labelColor = getPlatformColor(site),
                                ),
                            )
                        }

                        // Status
                        order.status.takeIf { it.isNotBlank() }?.let { status ->
                            Text(
                                status.split(" ").last(),
                                style = MaterialTheme.typography.labelSmall,
                                color = GrayText,
                            )
                        }

                        // Time (right aligned)
                        order.createdTime.takeIf { it > 0 }?.let { time ->
                            Text(
                                formatOrderTime(time),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFBDBDBD),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Three values below divider
            if (order.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = LightBgGray)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    // 订单金额 (content[1])
                    order.content.firstOrNull { it.label == "订单金额" }?.let { item ->
                        AmountItem(
                            label = "订单金额",
                            value = item.value?.toString() ?: "—",
                            color = DarkGray,
                        )
                    }

                    // 预估佣金 (content[2])
                    order.content.firstOrNull { it.label == "预估佣金" }?.let { item ->
                        AmountItem(
                            label = "预估佣金",
                            value = item.value?.toString() ?: "—",
                            color = BlueDark,
                        )
                    }

                    // 实际佣金 (content[3])
                    order.content.firstOrNull { it.label == "实际佣金" }?.let { item ->
                        AmountItem(
                            label = "实际佣金",
                            value = item.value?.toString() ?: "—",
                            color = GreenColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AmountItem(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = GrayText)
        Text(
            if (value != "—") "¥$value" else value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

// ===== PaginationBar =====

@Composable
fun PaginationBar(
    currentPage: Int,
    total: Int,
    onPageChange: (Int) -> Unit,
) {
    val totalPages = (total + 19) / 20

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
            enabled = currentPage > 1,
        ) {
            Text("上一页")
        }

        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "$currentPage / $totalPages",
            style = MaterialTheme.typography.bodySmall,
            color = GrayText,
        )
        Spacer(modifier = Modifier.width(12.dp))

        TextButton(
            onClick = { onPageChange(currentPage + 1) },
            enabled = currentPage * 20 < total,
        ) {
            Text("下一页")
        }
    }
}

// ===== 工具函数 =====

private fun formatMoney(value: Double): String =
    String.format("%.2f", value)
