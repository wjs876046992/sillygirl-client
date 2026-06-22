package com.sillygirl.client.ui.screens.fenyong

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
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
    viewModel: FenyongViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        val msg = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

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
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
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
                            items(uiState.orders, key = { it.orderId }) { order ->
                                OrderItemCard(order)
                            }
                        }

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

// ===== 订单卡片 =====
@Composable
fun OrderItemCard(order: com.sillygirl.client.data.model.FenyongOrder) {
    val titleText = order.skuName.ifBlank { order.name }
    val platformText = order.site.takeIf { it.isNotBlank() }?.let { getPlatformName(it) }
    val timeText = order.createdTime.takeIf { it > 0 }?.let { formatOrderTime(it) }
    val orderAmount = order.content.firstOrNull { it.label == "订单金额" }?.value?.toString() ?: "—"
    val estimateAmount = order.content.firstOrNull { it.label == "预估佣金" }?.value?.toString() ?: "0.00"
    val actualAmount = order.content.firstOrNull { it.label == "实际佣金" }?.value?.toString() ?: "0.00"

    GlassCard(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
    ) {
        // 上半部分：商品图 + 标题/标签
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // 商品图（固定尺寸）
            val hasImage = order.image.isNotBlank() && 
                !order.image.endsWith(".ico", ignoreCase = true) &&
                order.image.startsWith("http")
            if (hasImage) {
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
                    val placeholderIcon = when (order.site.lowercase()) {
                        "jd" -> "🔴"  // 京东红
                        "tb" -> "🟠"  // 淘宝橙
                        "pdd" -> "🔴"  // 拼多多红
                        else -> "🛒"
                    }
                    Text(placeholderIcon, fontSize = 28.sp)
                }
            }

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
                    platformText?.let { SiteChip(it, getPlatformColor(it)) }
                    timeText?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = GrayText)
                    }
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
