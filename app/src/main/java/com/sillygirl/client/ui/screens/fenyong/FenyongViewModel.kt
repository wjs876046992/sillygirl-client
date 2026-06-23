package com.sillygirl.client.ui.screens.fenyong

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillygirl.client.data.model.FenyongOrder
import com.sillygirl.client.data.repository.FenyongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private val TAG = "FenyongViewModel"

/**
 * 订单图片加载状态缓存
 */
object ImageCache {
    private val cache = mutableMapOf<String, BitmapPainter>()

    fun get(url: String): BitmapPainter? = synchronized(cache) { cache[url] }

    fun put(url: String, bitmap: ImageBitmap) {
        synchronized(cache) { cache[url] = BitmapPainter(bitmap) }
    }

    fun preload(urls: List<String>) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        urls.filter { it.isNotBlank() && !it.endsWith(".ico", ignoreCase = true) }.forEach { url ->
            // 跳过已缓存的
            if (get(url) != null) return@forEach

            try {
                val response = client.newCall(
                    Request.Builder()
                        .url(url)
                        .header("Referer", "https://www.jd.com/")
                        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                        .build()
                ).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    val bitmap = bytes?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
                    if (bitmap != null) {
                        put(url, bitmap.asImageBitmap())
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d(TAG, "Preload failed: $url")
            }
        }
    }
}

data class FenyongUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val keyword: String = "",
    val orders: List<FenyongOrder> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val filterActualGtZero: Boolean = false,
)

class FenyongViewModel : ViewModel() {
    private val repository = FenyongRepository()
    private val _uiState = MutableStateFlow(FenyongUiState())
    val uiState: StateFlow<FenyongUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadOrders(1)
    }

    /**
     * 加载订单列表（带搜索和分页）
     */
    fun loadOrders(page: Int = 1) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val keyword = _uiState.value.keyword

            try {
                val response = repository.getOrders(
                    keyword = if (keyword.isNotBlank()) keyword else null,
                    page = page,
                    pageSize = 20,
                )

                response.onSuccess { result ->
                    val orders = result.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        orders = orders,
                        page = result.page,
                        total = result.total,
                        error = null,
                    )
                    // 预加载所有订单图片
                    preloadImages(orders)
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载订单失败",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载订单失败",
                )
            }
        }
    }

    private fun preloadImages(orders: List<FenyongOrder>) {
        viewModelScope.launch(Dispatchers.IO) {
            ImageCache.preload(orders.map { it.image })
            android.util.Log.d(TAG, "Preloaded ${orders.size} images")
        }
    }

    fun setKeyword(keyword: String) {
        _uiState.value = _uiState.value.copy(keyword = keyword)
    }

    fun loadData() {
        loadOrders(1)
    }

    fun toggleFilterActualGtZero() {
        _uiState.value = _uiState.value.copy(
            filterActualGtZero = !_uiState.value.filterActualGtZero
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
