package com.sillygirl.client.ui.screens.login

import androidx.compose.foundation.background
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
import com.sillygirl.client.data.api.RetrofitClient
import com.sillygirl.client.data.repository.AuthRepository
import com.sillygirl.client.data.repository.ServerConfig
import com.sillygirl.client.ui.theme.PrimaryGradientColors
import com.sillygirl.client.ui.theme.themeShadow
import com.sillygirl.client.ui.screens.login.LoginViewModelFactory
import kotlinx.coroutines.launch

/**
 * 手动登录页面 - 保留作为备用
 * 主流程已改为选择服务器后自动登录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(LocalServerConfig.current)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo area: gradient glow + icon as a unified group
            Box(contentAlignment = Alignment.Center) {
                // Decorative gradient glow behind icon
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .themeShadow(30.dp, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(
                            brush = Brush.horizontalGradient(PrimaryGradientColors),
                            shape = RoundedCornerShape(50),
                        ),
                )
                // Icon card
                Card(
                    modifier = Modifier
                        .size(72.dp)
                        .themeShadow(12.dp, RoundedCornerShape(20.dp)),
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
                    .themeShadow(8.dp, RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.horizontalGradient(PrimaryGradientColors),
                        shape = RoundedCornerShape(14.dp),
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
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

/**
 * 自动登录页面 - 选择服务器后自动用保存的凭证登录
 * 显示加载动画，登录成功后跳转到 Dashboard
 */
@Composable
fun AutoLoginScreen(
    serverUrl: String,
    username: String,
    password: String,
    onLoginSuccess: () -> Unit,
    onLoginFailed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val authRepo = remember { AuthRepository() }
    val serverConfig = LocalServerConfig.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            errorMessage = null

            val result = authRepo.login(serverUrl, username, password)
            result.fold(
                onSuccess = {
                    // 登录成功，保存 token
                    RetrofitClient.token?.let { token ->
                        serverConfig.saveToken(token)
                    }
                    isLoading = false
                    onLoginSuccess()
                },
                onFailure = { e ->
                    isLoading = false
                    errorMessage = e.message ?: "登录失败"
                    // 延迟后返回服务器列表
                    kotlinx.coroutines.delay(2000)
                    onLoginFailed()
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .themeShadow(24.dp, RoundedCornerShape(40))
                        .clip(RoundedCornerShape(40))
                        .background(
                            brush = Brush.horizontalGradient(PrimaryGradientColors),
                            shape = RoundedCornerShape(40),
                        ),
                )
                Text("🤖", fontSize = 48.sp)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "SillyGirl",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "正在登录...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    serverUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (errorMessage != null) {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    errorMessage ?: "登录失败",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "正在返回...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
