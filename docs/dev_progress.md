# SillyGirl Client 开发进度

## 最近更新 (2026-06-24)

### 🔐 自动登录优化

**问题描述：**
- 原流程需要手动输入用户名密码登录
- 杀掉 App 后 cookie/token 丢失，需要重新登录

**解决方案：**
1. **自动登录流程**
   - 添加服务器时保存用户名密码到 SharedPreferences
   - 选择服务器后自动登录，无需手动输入
   - App 启动时自动用保存的凭证登录

2. **Token 持久化修复**
   - 启动时先调用 `RetrofitClient.setServer()` 恢复服务器地址
   - 再恢复 token，验证会话有效性
   - 如果 token 过期，自动用保存的凭证重新登录

**修改文件：**
- `AppNavGraph.kt` - 添加自动登录路由和启动逻辑
- `LoginScreen.kt` - 添加 AutoLoginScreen 组件
- `ServiceScreen.kt` - 切换服务器时自动登录
- `gradle.properties` - 修复 Java 路径

---

## 功能清单

### ✅ 已完成
- [x] 服务器管理（添加/切换/删除服务器）
- [x] 自动登录（选择服务器后自动登录）
- [x] Token 持久化（杀掉 App 后保持登录状态）
- [x] 启动时自动验证和登录
- [x] 会话过期自动跳转登录
- [x] Dashboard 主页
- [x] 插件管理（我的插件/插件市场）
- [x] 分用管理
- [x] 主人管理
- [x] 定时任务
- [x] 服务管理
- [x] 存储管理

### 🚧 待优化
- [ ] 服务器配置云端同步
- [ ] 导出/导入功能
- [ ] 多语言支持
- [ ] 深色模式

---

## 架构说明

### 数据持久化
```
SharedPreferences (sillygirl_servers)
├── servers: "url|username|password|alias;;..."
├── default_index: 0
└── token: "xxx"
```

### 登录流程
```
App 启动
  ↓
读取 SharedPreferences
  ↓
设置服务器地址 → 恢复 token
  ↓
验证 token
  ├─ 有效 → Dashboard
  └─ 无效 → 自动登录 → Dashboard
```

---

## 构建命令

```bash
# Debug 构建
./gradlew assembleDebug

# 输出路径
app/build/outputs/apk/debug/app-debug.apk

# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
