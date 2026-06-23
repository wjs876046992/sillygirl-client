# sillygirl-client 开发进展

## 2026-06-23 修复插件管理功能

### 问题描述

用户反馈新版本存在以下问题：
1. 首页分佣模块不见了
2. 插件列表没有显示新加的icon等内容
3. 插件详情页的调试模式、禁用模式没有功能
4. 表单功能缺失

### 问题分析

1. **分佣模块不显示** - 后端MongoDB未配置，导致API返回空数据
2. **插件字段缺失** - `getCurrentUser` API返回的Route结构体缺少debug/disable/hasForm等字段
3. **调试/禁用模式** - 前端UI未正确连接到后端API
4. **表单功能** - 前端缺少表单编辑UI组件

### 修改内容

#### 1. PluginScreens.kt
- 修改`PluginDetailScreen`，从`plugins/list.json` API获取完整插件信息
- 添加禁用模式Switch UI
- 添加`PluginFormCard`组件（支持text/number/switch/select类型）
- 修改`PluginInfoCard`，支持emoji图标显示

#### 2. PluginViewModels.kt
- 添加`toggleDisable()`函数
- 添加`loadPluginDetail()`函数
- 更新`PluginDetailUiState`，添加pluginDetail和formFields字段

#### 3. PluginRepository.kt
- 添加`togglePluginDisable()` API调用
- 添加`getPluginDetail()`函数，从plugins/list.json获取完整插件信息

#### 4. Models.kt
- `PluginRoute`添加`formFields`字段
- `PluginInfo`添加`hasForm`字段

### 编译状态

✅ 编译成功（2026-06-23 17:30）

### 待办事项

- [ ] 配置测试服务器的MongoDB（分佣功能依赖）
- [ ] 推送代码到GitHub
- [ ] 构建Release APK
- [ ] 测试插件调试/禁用功能
- [ ] 测试表单配置功能

### 相关文件

- 后端API: `../sillyGirl/core/plugin_subscribe.go`
- 后端分佣: `../sillyGirl/core/fenyong_api.go`
- 测试服务器: `192.168.1.12` (pagermaid用户)
- 部署目录: `/home/pagermaid/docker/sillyplus`

---

## 2026-06-23 部署最新版本到测试服务器

### 操作步骤

1. 从GitHub下载最新linux amd64二进制文件
2. SCP上传到测试服务器`/tmp/`
3. 停止pm2进程
4. 备份旧文件
5. 替换`sillyplus`二进制文件
6. 重启pm2服务

### 部署命令

```bash
# 下载最新版本
curl -L -o sillyGirl_linux_amd64 https://github.com/wjs876046992/sillyGirl/releases/latest/download/sillyGirl_linux_amd64

# 上传到测试服务器
scp sillyGirl_linux_amd64 pagermaid@192.168.1.12:/tmp/

# 部署（需要SSH到服务器执行）
ssh pagermaid@192.168.1.12
cd /home/pagermaid/docker/sillyplus
/home/pagermaid/.pm2/modules/pm2-logrotate/node_modules/pm2/bin/pm2 stop sillyplus
cp sillyplus sillyplus_bak_$(date +%Y%m%d_%H%M%S)
cp /tmp/sillyGirl_linux_amd64 sillyplus
chmod +x sillyplus
/home/pagermaid/.pm2/modules/pm2-logrotate/node_modules/pm2/bin/pm2 start sillyplus
```

### 部署状态

✅ 部署成功（2026-06-23 17:25）
- 进程ID: 2445487
- 状态: online

---

## 技术笔记

### PM2命令（测试服务器）

由于pm2不在PATH中，需要使用完整路径：
```bash
/home/pagermaid/.pm2/modules/pm2-logrotate/node_modules/pm2/bin/pm2 [command]
```

### 后端插件数据结构

`plugins/list.json` API返回完整插件信息：
- `id` - 插件UUID
- `icon` - 图标（emoji或URL）
- `debug` - 调试模式状态
- `disable` - 禁用状态
- `has_form` - 是否有配置表单
- `running` - 运行状态
- `classes` - 分类标签

`getCurrentUser` API返回的plugins列表缺少部分字段，需要通过`plugins/list.json`补充。

### 分佣功能依赖

分佣功能需要MongoDB，配置项：
- `fanli.mongodb` - MongoDB连接字符串
- 格式: `mongodb://username:password@host:port/database`
