package com.sillygirl.client.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sillygirl.client.LocalServerConfig
import com.sillygirl.client.ui.theme.PrimaryGradientColors
import com.sillygirl.client.ui.screens.login.LoginViewModelFactory

// 测试配置（仅 debug 阶段使用）
private val TEST_SERVER_URL = "http://192.168.1.12:8081"
private val TEST_USERNAME = "silly"
private val TEST_PASSWORD = "yKAuGG58S4zKHS"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(LocalServerConfig.current)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    // 自动填充测试配置
    LaunchedEffect(Unit) {
        viewModel.updateServerUrl(TEST_SERVER_URL)
        viewModel.updateUsername(TEST_USERNAME)
        viewModel.updatePassword(TEST_PASSWORD)
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // 顶部渐变装饰（暗色模式下更柔和）
        val gradientAlpha = if (isSystemInDarkTheme()) 0.35f else 0.6f
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .align(Alignment.TopCenter)
                .offset(y = (-30).dp)
                .size(240.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    brush = Brush.horizontalGradient(PrimaryGradientColors),
                    shape = RoundedCornerShape(50),
                )
                .shadow(40.dp, RoundedCornerShape(50)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo / App name
            Card(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(PrimaryGradientColors), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🤖", fontSize = 36.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "SillyGirl",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                "管理你的傻妞机器人",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(36.dp))

            // Server URL
            OutlinedTextField(
                value = uiState.serverUrl,
                onValueChange = viewModel::updateServerUrl,
                label = { Text("服务器地址") },
                placeholder = { Text("http://192.168.1.18:3000") },
                leadingIcon = { Icon(Icons.Filled.Language, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(Modifier.height(14.dp))

            // Username
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::updateUsername,
                label = { Text("用户名") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(Modifier.height(14.dp))

            // Password
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::updatePassword,
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )

            if (uiState.error != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        "⚠ ${uiState.error}",
                        modifier = Modifier.padding(10.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Login button
            Button(
                onClick = viewModel::login,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                    )
                } else {
                    Text("登 录", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
    }
}
