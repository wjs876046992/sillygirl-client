package com.sillygirl.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.repository.ServerConfig
import com.sillygirl.client.ui.navigation.AppNavGraph
import com.sillygirl.client.ui.theme.SillyGirlTheme

// CompositionLocal for ServerConfig
val LocalServerConfig = staticCompositionLocalOf<ServerConfig> {
    error("LocalServerConfig not provided")
}

class MainActivity : ComponentActivity() {
    private lateinit var serverConfig: ServerConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ServerConfig
        serverConfig = ServerConfig(this)

        // 测试阶段：如果无服务器记录，自动添加测试服务器
        if (serverConfig.getServers().isEmpty()) {
            try {
                serverConfig.addServer(ServerConfig.ServerInfo(
                    url = "http://192.168.1.12:8081",
                    username = "silly",
                    password = "yKAuGG58S4zKHS",
                    alias = "测试服务器(ForTest)",
                ))
            } catch (_: Exception) { }
        }

        // Auto-set server from saved config, restore token
        serverConfig.getDefaultServer()?.let {
            RetrofitClient.setServer(it.url)
        }
        serverConfig.getToken()?.let {
            RetrofitClient.token = it
        }

        enableEdgeToEdge()
        setContent {
            SillyGirlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(LocalServerConfig provides serverConfig) {
                        AppNavGraph()
                    }
                }
            }
        }
    }
}
