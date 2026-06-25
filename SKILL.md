---
name: sillygirl-client-release
description: 发布 sillygirl-client Android 客户端的标准化流程。适用于 wjs876046992/sillygirl-client 仓库。触发条件：用户要求构建 APK、发版、打 tag。
---

# sillygirl-client Release Workflow

## 前置条件

- 工作目录：项目根目录（sillygirl-client 仓库）
- 远程仓库：`https://github.com/wjs876046992/sillygirl-client.git`
- JDK：21
- Android SDK
- 已配置 `gh` CLI 并登录

## 触发约定

| 用户说 | 执行模式 |
|--------|---------|
| `构建 APK`、`打包`、`编译一下` | **模式 A**：本地 assembleDebug |
| `发版`、`发布 v1.1.0`、`发布新版本` | **模式 B**：tag → CI → GitHub Release |
| `预演发布`、`dry-run`、`看看发布流程` | **模式 B dry-run**：只展示流程不执行 |

---

## 模式 A：本地构建

```
./gradlew assembleDebug → 产物：app/build/outputs/apk/debug/app-debug.apk
```

### 步骤

```bash
cd /home/openclaw/sillygirl/sillygirl-client
./gradlew assembleDebug --no-daemon
```

### 输出路径

```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 模式 B：正式发布（GitHub Workflow）

```
git tag → push tag → gh workflow run → 等 CI 完成 → GitHub Release
```

### 步骤

#### 1. 预检

```bash
gh auth status
git status --porcelain        # 确认工作区干净
git fetch --tags origin
```

#### 2. 确定版本（语义化版本）

- `v1.0.0` → 首个正式版本
- `v1.0.1` → Bug 修复
- `v1.1.0` → 新增功能
- `v2.0.0` → 重大重构

```bash
git tag -l "v*" --sort=-v:refname  # 查看现有标签
```

#### 3. 创建 Tag 并推送

```bash
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin v1.1.0
```

#### 4. 触发 CI 构建

```bash
gh workflow run build-apk.yml --ref v1.1.0
```

#### 5. 等待 CI 完成

```bash
RUN_ID=$(gh run list --workflow=build-apk.yml --limit 1 --json databaseId --jq '.[0].databaseId')
# 轮询状态直到完成
gh run watch "$RUN_ID"
```

#### 6. 更新 Release Notes

```bash
gh release edit v1.1.0 --notes-file release_notes.md
```

---

## Release Notes 模板

```markdown
## 版本更新

### ✨ 新增功能
- 描述 (commit_hash)

### 🐛 缺陷修复
- 描述 (commit_hash)

### 🔧 维护更新
- 描述 (commit_hash)

### 📱 安装方式
1. 下载 APK 文件
2. 在 Android 设备上安装（需允许未知来源）

### 📦 支持平台
- Android 8.0+ (API 26)
```

---

## Troubleshooting

| 问题 | 解决 |
|------|------|
| `gh auth expired` | `gh auth login` |
| `Gradle build failed` | `./gradlew clean assembleDebug` |
| CI 未触发 | 确认 tag 已推送：`git push origin <tag>` |

---

## 版本号管理

- `versionCode`: 整数，每次发布递增（`app/build.gradle.kts`）
- `versionName`: 字符串，语义化版本

**发布前**：更新 `app/build.gradle.kts` 中的 `versionCode` 和 `versionName`。

---

*最后更新: 2026-06-25*