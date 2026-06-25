package com.sillygirl.client.ui.screens.fenyong

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillygirl.client.ui.components.*
import com.sillygirl.client.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import com.sillygirl.client.ui.components.MiniAppBar

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
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        sdf.format(java.util.Date(ts * 1000))
    } catch (_: Exception) { "—" }
}

@Composable
private fun getStatusColor(status: String): Color = when {
    status.contains("结算") -> SuccessColor
    status.contains("退款") || status.contains("失效") -> DangerColor
    status.contains("待") || status.contains("审核") -> WarningColor
    status.isBlank() -> GrayText
    else -> MaterialTheme.colorScheme.primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FenyongScreen(
    onBack: () -> Unit = {},
    viewModel: FenyongViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        val msg = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    Scaffold(
        topBar = {
            MiniAppBar(
                title = { Text("分佣系统") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData(showRefreshHint = true) }) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            // 快捷筛选栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = uiState.filterActualGtZero,
                    onClick = { viewModel.toggleFilterActualGtZero() },
                    label = { Text("实际佣金 > 0", style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = if (uiState.filterActualGtZero) {
                        { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                    } else null,
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SuccessColor.copy(alpha = 0.15f),
                        selectedLabelColor = SuccessColor,
                        selectedLeadingIconColor = SuccessColor,
                    ),
                )
                if (uiState.filterActualGtZero) {
                    val filteredCount = uiState.orders.count {
                        (it.content.firstOrNull { c -> c.label == "实际佣金" }?.value?.toString()?.toDoubleOrNull() ?: 0.0) > 0
                    }
                    Text(
                        "${filteredCount}/${uiState.orders.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayText,
                    )
                }
            }

            when {
                uiState.isLoading && uiState.orders.isEmpty() -> {
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
                    val displayedOrders = if (uiState.filterActualGtZero) {
                        uiState.orders.filter { order ->
                            val actual = order.content.firstOrNull { it.label == "实际佣金" }?.value?.toString()?.toDoubleOrNull() ?: 0.0
                            actual > 0
                        }
                    } else {
                        uiState.orders
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                        ) {
                            if (displayedOrders.isEmpty() && !uiState.isLoading) {
                                item {
                                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, Modifier.size(48.dp), tint = GrayText)
                                            Spacer(Modifier.height(8.dp))
                                            Text("暂无订单", color = GrayText)
                                        }
                                    }
                                }
                            } else {
                                items(displayedOrders, key = { it.orderId }) { order ->
                                    OrderItemCard(order)
                                }
                            }
                        }

                        if (uiState.total > 20) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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

@Composable
fun OrderItemCard(order: com.sillygirl.client.data.model.FenyongOrder) {
    val titleText = order.skuName.ifBlank { order.name }
    val platformText = order.site.takeIf { it.isNotBlank() }?.let { getPlatformName(it) }
    val timeText = order.createdTime.takeIf { it > 0 }?.let { formatOrderTime(it) }
    val orderAmount = order.content.firstOrNull { it.label == "订单金额" }?.value?.toString() ?: "—"
    val estimateAmount = order.content.firstOrNull { it.label == "预估佣金" }?.value?.toString() ?: "0.00"
    val actualAmount = order.content.firstOrNull { it.label == "实际佣金" }?.value?.toString() ?: "0.00"

    GlassCard(
        modifier = Modifier.fillMaxWidth().themeShadow(4.dp, RoundedCornerShape(16.dp)),
    ) {
        // 上半部分：商品图 + 标题/标签
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            OrderItemImage(order.image, order.site)

            // 信息（固定高度列）
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    titleText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 平台标签
                    if (order.site.isNotBlank()) {
                        val siteColor = getPlatformColor(order.site)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = siteColor.copy(alpha = 0.12f),
                        ) {
                            Text(
                                getPlatformName(order.site),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = siteColor,
                            )
                        }
                    }
                    // 状态标签
                    if (order.status.isNotBlank()) {
                        val sc = getStatusColor(order.status)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = sc.copy(alpha = 0.12f),
                        ) {
                            Text(
                                order.status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = sc,
                            )
                        }
                    }
                    // 绑定用户信息
                    if (order.bind != null && order.bind.userId.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                val platformLabel = order.bind.platform.takeIf { it.isNotBlank() }
                                    ?.let { getPlatformName(it) } ?: ""
                                val userName = order.bind.userName.takeIf { it.isNotBlank() }
                                    ?: order.bind.userId
                                Text(
                                    if (platformLabel.isNotBlank()) "$platformLabel $userName" else userName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                timeText?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = GrayText)
                }
            }
        }

        // 分隔线
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // 下半部分：金额行（固定布局）
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text("订单", style = MaterialTheme.typography.labelSmall, color = GrayText)
                Text(orderAmount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text("预估", style = MaterialTheme.typography.labelSmall, color = GrayText)
                Text("¥$estimateAmount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text("实际", style = MaterialTheme.typography.labelSmall, color = GrayText)
                Text("¥$actualAmount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SuccessColor)
            }
        }
    }
}

// ===== 商品图（Coil AsyncImage） =====
@Composable
private fun OrderItemImage(url: String, site: String) {
    val hasImage = url.isNotBlank() && !url.endsWith(".ico", ignoreCase = true)
    val platformColor = getPlatformColor(site)

    Box(
        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // 底层始终绘制平台色占位（图片加载失败时可见）
        Box(
            modifier = Modifier.fillMaxSize().background(platformColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(getPlatformName(site).first().toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = platformColor)
        }

        if (hasImage) {
            val context = androidx.compose.ui.platform.LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
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
