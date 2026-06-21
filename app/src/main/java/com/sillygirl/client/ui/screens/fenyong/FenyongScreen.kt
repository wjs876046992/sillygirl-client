package com.sillygirl.client.ui.screens.fenyong

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.weight
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
import androidx.compose.ui.draw.shadow
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
import com.sillygirl.client.ui.components.*
import com.sillygirl.client.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val RedColor = Color(0xFFE60012)
private val OrangeColor = Color(0xFFFF5000)
private val PddColor = Color(0xFFE02A24)
private val GrayText = Color(0xFF999999)

private fun getPlatformColor(code: String): Color = when (code.lowercase()) {
    "jd" -> RedColor
    "tb" -> OrangeColor
    "pdd" -> PddColor
    else -> GrayText
}

private fun getPlatformName(code: String): String = when (code.lowercase()) {
    "jd" -> "京东"
    "tb" -> "淘宝"
    "pdd" -> "拼多多"
    else -> code
}

private fun formatOrderTime(ts: Long?): String {
    if (ts == null || ts == 0L) return "—"
    return try {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
        sdf.format(java.util.Date(ts * 1000))
    } catch (_: Exception) { "—" }
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
            CenterAlignedTopAppBar(
                title = { Text("分佣系统") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "退出登录")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                uiState.isLoading && uiState.dashboard == null && uiState.orders.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.orders.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { viewModel.loadData() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        // 1. 分佣概览大卡
                        uiState.dashboard?.let { dash ->
                            item {
                                FenyongHeroCard(dash)
                            }
                        }

                        // 2. 搜索栏
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = uiState.keyword,
                                    onValueChange = viewModel::setKeyword,
                                    placeholder = { Text("搜索SKU/订单号") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    trailingIcon = {
                                        if (uiState.keyword.isNotBlank()) {
                                            IconButton(onClick = { viewModel.clearSearch(); viewModel.loadOrders(1) }) {
                                                Icon(Icons.Filled.Close, "清除", tint = GrayText)
                                            }
                                        }
                                    },
                                )
                                FilledTonalButton(
                                    onClick = { viewModel.loadOrders(1) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(48.dp),
                                ) {
                                    Text("搜索")
                                }
                            }
                        }

                        // 3. 订单列表
                        if (uiState.orders.isEmpty() && !uiState.isLoading) {
                            item {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.ReceiptLong, null, Modifier.size(48.dp), tint = GrayText)
                                        Spacer(Modifier.height(8.dp))
                                        Text("暂无订单", color = GrayText)
                                    }
                                }
                            }
                        } else {
                            items(uiState.orders) { order ->
                                OrderItemCard(order)
                            }
                        }

                        // 4. 分页
                        if (uiState.total > 20) {
                            item {
                                PaginationWidget(
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

// ===== 分佣 Hero 卡片 =====
@Composable
fun FenyongHeroCard(data: com.sillygirl.client.data.model.FenyongDashboardResponse) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(PrimaryGradientColors), RoundedCornerShape(22.dp))
        ) {
            // 装饰圆
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 16.dp, y = (-16).dp)
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-8).dp, y = 20.dp)
                    .size(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
            )

            Column(modifier = Modifier.padding(20.dp)) {
                // 今日收入
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("今日预估", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                }
                BigMoneyTextWithLabel(data.today.estimate)

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))

                // 对比行
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    ComparisonChip("昨日", data.yesterday.estimate, data.yesterday.orders)
                    ComparisonChip("7天", data.last7days.estimate, data.last7days.orders)
                    ComparisonChip("本月", data.lastMonth.estimate, data.lastMonth.orders)
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(8.dp))

                // 平台收益
                Text("平台收益（今日）", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(6.dp))
                PlatformRow(data, "jd")
                PlatformRow(data, "tb")
                PlatformRow(data, "pdd")
            }
        }
    }
}

@Composable
private fun BigMoneyTextWithLabel(value: Double) {
    Text(
        "¥${formatMoney(value)}",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
}

@Composable
private fun ComparisonChip(label: String, estimate: Double, orders: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
        Text("¥${formatMoney(estimate)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text("$orders 单", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
    }
}

@Composable
private fun PlatformRow(data: com.sillygirl.client.data.model.FenyongDashboardResponse, code: String) {
    val stat = data.platforms[code]
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(getPlatformColor(code)),
        )
        Spacer(Modifier.width(8.dp))
        Text(getPlatformName(code), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
        Text("${stat?.orders ?: 0}单", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Text("¥${formatMoney(stat?.estimate ?: 0.0)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ===== 订单卡片 =====
@Composable
fun OrderItemCard(order: com.sillygirl.client.data.model.FenyongOrder) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // 商品图
            if (order.image.isNotBlank()) {
                AsyncImage(
                    model = order.image,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🛒", fontSize = 28.sp)
                }
            }

            // 信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    order.skuName.ifBlank { order.name },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // 标签行
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    order.site.takeIf { it.isNotBlank() }?.let { SiteChip(getPlatformName(it), getPlatformColor(it)) }
                    order.createdTime.takeIf { it > 0 }?.let {
                        Text(formatOrderTime(it), style = MaterialTheme.typography.labelSmall, color = GrayText)
                    }
                }
            }
        }

        // 金额行
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AmountCell("订单", order.content.firstOrNull { it.label == "订单金额" }?.value?.toString() ?: "—", Color(0xFF333333))
            AmountCell("预估", "¥${order.content.firstOrNull { it.label == "预估佣金" }?.value?.toString() ?: "0.00"}", MaterialTheme.colorScheme.primary)
            AmountCell("实际", "¥${order.content.firstOrNull { it.label == "实际佣金" }?.value?.toString() ?: "0.00"}", SuccessColor)
        }
    }
}

@Composable
private fun AmountCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = GrayText)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

// ===== 分页 =====
@Composable
private fun PaginationWidget(currentPage: Int, total: Int, onPageChange: (Int) -> Unit) {
    val totalPages = (total + 19) / 20

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
            enabled = currentPage > 1,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(36.dp),
        ) {
            Text("上一页", style = MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.width(16.dp))
        Text(
            "第 $currentPage / $totalPages 页",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(16.dp))

        OutlinedButton(
            onClick = { onPageChange(currentPage + 1) },
            enabled = currentPage * 20 < total,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(36.dp),
        ) {
            Text("下一页", style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun formatMoney(value: Double): String = String.format("%.2f", value)
