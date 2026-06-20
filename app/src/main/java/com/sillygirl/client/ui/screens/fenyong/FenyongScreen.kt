package com.sillygirl.client.ui.screens.fenyong

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.sillygirl.client.data.model.FenyongOrder
import com.sillygirl.client.data.model.FenyongTab
import com.sillygirl.client.data.model.FenyongTimeRange
import com.sillygirl.client.data.model.FenyongTongjiData
import java.text.SimpleDateFormat
import java.util.*

// ===== 颜色 =====
private val BlueColor = Color(0xFF1890FF)
private val BlueLightBg = Color(0xFFE6F7FF)
private val GreenColor = Color(0xFF52C41A)
private val GreenLightBg = Color(0xFFF6FFED)
private val PurpleColor = Color(0xFF722ED1)
private val OrangeColor = Color(0xFFFF8C1A)
private val GrayTextColor = Color(0xFF00000073)
private val GrayDarkText = Color(0xFF000000D9)
private val LightBg = Color(0xFFFFF0F6)
private val LightBgGray = Color(0xFFF5F5F5)

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
                title = { Text("分佣系统") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (uiState.isRefreshing || uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = { viewModel.refreshAll() }) {
                            Icon(Icons.Filled.Refresh, "刷新")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                uiState.isLoading && uiState.orders.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.orders.isEmpty() -> {
                    ErrorState(
                        message = uiState.error ?: "加载失败",
                        onRetry = { viewModel.refreshAll() },
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 1. 时间范围 + 用户筛选
                        item {
                            FilterBar(uiState, viewModel)
                        }

                        // 2. 统计数据卡片（12 项指标）
                        uiState.tongji?.let { tongji ->
                            item {
                                StatisticsCard(tongji)
                            }
                        }

                        // 3. 订单 Tab 切换
                        if (uiState.tabs.isNotEmpty()) {
                            item {
                                TabBar(
                                    tabs = uiState.tabs,
                                    activeTab = uiState.activeTab,
                                    onTabChange = viewModel::changeTab,
                                )
                            }
                        }

                        // 4. 订单列表
                        if (uiState.orders.isEmpty() && !uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("暂无订单数据", color = GrayTextColor)
                                }
                            }
                        } else {
                            items(uiState.orders) { order ->
                                OrderCard(order)
                            }
                        }

                        // 5. 加载更多
                        if (uiState.hasMore) {
                            item {
                                LoadMoreButton { viewModel.loadMore() }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== FilterBar: 时间范围 + 用户选择 =====

@Composable
fun FilterBar(
    uiState: FenyongUiState,
    viewModel: FenyongViewModel,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 时间范围选择
            Text("时间范围：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(FenyongTimeRange.options) { option ->
                    TimeRangeChip(
                        label = option.label,
                        selected = uiState.timeRange == option.value,
                        onClick = { viewModel.changeTimeRange(option.value) },
                    )
                }
            }

            // 用户选择
            val users = uiState.tongji?.results?.filter { it.value.isNotEmpty() && it.label.isNotEmpty() }
                ?.sortedByDescending { it.count } ?: emptyList()

            if (users.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("选择用户：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // "全部" chip
                    item {
                        UserChip(
                            label = "--全部--",
                            count = uiState.tongji?.orderNum ?: 0,
                            selected = uiState.activeUser == "#",
                            onClick = { viewModel.changeUser("#") },
                        )
                    }
                    // "已绑" / "未绑" chips
                    items(users) { user ->
                        UserChip(
                            label = user.label,
                            count = user.count,
                            selected = uiState.activeUser == user.value,
                            onClick = { viewModel.changeUser(user.value) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeRangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (selected) BlueColor else Color(0xFFD9D9D9)
    val textColor = if (selected) Color.White else Color.DarkGray

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun UserChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) BlueLightBg else LightBgGray,
        ),
        onClick = onClick,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) BlueColor else GrayTextColor,
                maxLines = 1,
            )
            Badge() {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) BlueColor else GrayTextColor,
                )
            }
        }
    }
}

// ===== StatisticsCard: 12 项统计指标（2 列网格） =====

@Composable
fun StatisticsCard(tongji: FenyongTongjiData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "统计数据",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 2 列网格
            GridStatItem("订单总数", tongji.orderNum.toString())
            GridStatItem("用户总数", tongji.userNum.toString())
            GridStatItem("实际总收入(含返佣)", "$${formatMoney(tongji.totalActual)}")
            GridStatItem("预估总收入(含返佣)", "$${formatMoney(tongji.totalEstimate)}")
            GridStatItem("实际返佣金额", "$${formatMoney(tongji.totalRakeActual)}")
            GridStatItem("预估返佣金额", "$${formatMoney(tongji.totalRakeEstimate)}")
            GridStatItem("实际返佣收益", "$${formatMoney(tongji.totalIrakeActual)}")
            GridStatItem("预估返佣收益", "$${formatMoney(tongji.totalIrakeEstimate)}")
            GridStatItem(
                "实际返佣收益率",
                "${String.format("%.1f%%", tongji.totalIrakeActualPct * 100)}",
            )
            GridStatItem(
                "预估返佣收益率",
                "${String.format("%.1f%%", tongji.totalIrakeEstimatePct * 100)}",
            )
            GridStatItem("实际总收入(不含返佣)", "$${formatMoney(tongji.totalIactual)}")
            GridStatItem("预估总收入(不含返佣)", "$${formatMoney(tongji.totalIestimate)}")
        }
    }
}

@Composable
fun GridStatItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = GrayTextColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

// ===== TabBar: 订单 Tab 切换 =====

@Composable
fun TabBar(
    tabs: List<FenyongTab>,
    activeTab: String,
    onTabChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { tab ->
            val selected = activeTab == tab.key
            TabChip(
                label = tab.title,
                count = 0, // 从 tongji 中获取不太方便，用 0 占位
                selected = selected,
                onClick = { onTabChange(tab.key) },
            )
        }
    }
}

@Composable
fun TabChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) BlueColor else LightBgGray,
        ),
        onClick = onClick,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else GrayTextColor,
            )
            if (count > 0) {
                Badge(
                    containerColor = if (selected) Color.White else BlueLightBg,
                ) {
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) BlueColor else BlueColor,
                    )
                }
            }
        }
    }
}

// ===== OrderCard: 订单卡片 =====

@Composable
fun OrderCard(order: FenyongOrder) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top row: 平台标签 + 标题 + 订单时间 + 已绑用户
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 平台标签
                if (order.site.isNotBlank()) {
                    val (siteText, siteColor) = getSiteInfo(order.site)
                    AssistChip(
                        onClick = {},
                        label = { Text(siteText) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = siteColor.copy(alpha = 0.15f),
                            labelColor = siteColor,
                        ),
                    )
                }

                // 订单时间
                if (order.createdTime > 0) {
                    Text(
                        formatTime(order.createdTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayTextColor,
                    )
                }

                // 已绑用户标签
                order.bind?.let { bind ->
                    AssistChip(
                        onClick = {},
                        label = { Text("用户") },
                        leadingIcon = {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(android.R.drawable.presence_online),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = PurpleColor.copy(alpha = 0.1f),
                            labelColor = PurpleColor,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 主内容区域：图片 + 标题 + 金额
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // 商品图片
                if (order.image.isNotBlank()) {
                    AsyncImage(
                        model = order.image,
                        contentDescription = null,
                        modifier = Modifier
                            .width(80.dp)
                            .height(80.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("📦", fontSize = 28.sp)
                    }
                }

                // 标题 + 详情
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 商品名称
                    Text(
                        order.name.ifBlank { order.skuName },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // 订单 ID + SKU ID
                    if (order.orderId.isNotBlank()) {
                        Text(
                            "订单号: ${order.orderId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // SKU 信息
                    if (order.skuName.isNotBlank()) {
                        Text(
                            order.skuName,
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // 金额区域
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (order.estimate != 0.0) {
                        Text(
                            "预估 ¥${formatMoney(order.estimate)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BlueColor,
                        )
                    }
                    if (order.actual != 0.0) {
                        Text(
                            "实得 ¥${formatMoney(order.actual)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenColor,
                        )
                    }
                }
            }

            // 订单内容详情（订单时间、订单金额、预估/实际佣金）
            if (order.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                OrderContentRow(order)
            }

            // 状态
            if (order.status.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(order.status) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun OrderContentRow(order: FenyongOrder) {
    // 显示 content 中的关键信息（订单时间、订单金额、预估佣金、实际佣金）
    val rows = order.content.filter { it.label.isNotEmpty() }
    if (rows.isEmpty()) return

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(rows) { item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = GrayTextColor,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (item.status == "success") {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(GreenColor),
                        )
                    }
                    Text(
                        item.value?.toString() ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = GrayDarkText,
                    )
                }
            }
        }
    }
}

// ===== Load More =====

@Composable
fun LoadMoreButton(onLoad: () -> Unit) {
    TextButton(
        onClick = onLoad,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("加载更多")
    }
}

// ===== Error State =====

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry) {
            Text("重试")
        }
    }
}

// ===== 工具函数 =====

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    return sdf.format(Date(timestamp * 1000))
}

private fun formatMoney(value: Double): String =
    String.format("%.2f", value)

private fun getSiteInfo(site: String): Pair<String, Color> {
    return when (site.lowercase()) {
        "jd" -> "京东" to Color(0xFFE60012)
        "tb" -> "淘宝" to Color(0xFFFF5000)
        "pdd" -> "拼多多" to Color(0xFFE02A24)
        else -> site to GrayTextColor
    }
}
