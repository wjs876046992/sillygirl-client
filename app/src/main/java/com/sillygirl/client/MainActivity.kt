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
