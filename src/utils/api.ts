// sillyGirl API 调用层
//
// 认证策略（双通道）:
//   cookie: credentials: 'include' 始终发送（Vite proxy 模式天然同域可用）
//   X-Token header: 始终发送（跨域 + 新版服务器）
//
// 两种模式:
//   1. Vite proxy 模式（开发）：请求走相对路径 /api/... → proxy 到 sillyGirl
//      浏览器同域，cookie 自动生效。登录时从 Set-Cookie 提取 token。
//   2. 直接模式（生产/APK）：请求走 {serverUrl}/api/...
//      依赖 X-Token header 或 CORS + cookie。

let _token = ''           // X-Token 值（持久化的）
let _serverUrl = ''       // 用户添加的服务器地址（仅用于显示/登录，非请求）

export function setToken(token: string) { _token = token }
export function getToken() { return _token }

export function setServerUrl(url: string) { _serverUrl = url.replace(/\/+$/, '') }
export function getServerUrl() { return _serverUrl }

export function clearConnection() { _token = ''; _serverUrl = '' }
export function isConnected() { return !!_token }

interface ApiResult<T = any> {
  success: boolean
  data?: T
  error?: string
  authFailed?: boolean
}

async function request<T = any>(
  method: string,
  path: string,
  body?: any,
  extraHeaders?: Record<string, string>
): Promise<ApiResult<T>> {
  const headers: Record<string, string> = { ...extraHeaders }
  if (_token) headers['X-Token'] = _token
  if (body && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }

  try {
    const res = await fetch(path, {
      method,
      headers,
      credentials: 'include',
      body: body ? JSON.stringify(body) : undefined,
    })

    const contentType = res.headers.get('content-type') || ''
    if (!contentType.includes('application/json')) {
      const text = await res.text()
      if (text.includes('<!DOCTYPE html>') || text.includes('<html')) {
        return { success: false, error: '会话已过期，请重新登录', authFailed: true }
      }
      return { success: false, error: `非 JSON 响应: ${text.slice(0, 200)}` }
    }

    const data = await res.json()
    if (data?.errorCode === '401') {
      return { success: false, error: data.errorMessage || '请先登录', authFailed: true }
    }
    return { success: true, data }
  } catch (e: any) {
    return { success: false, error: e.message || '请求失败' }
  }
}

// ========== 认证 ==========

export async function login(url: string, username: string, password: string): Promise<{ ok: boolean }> {
  try {
    // 登录始终用相对路径（Vite proxy 处理）
    const res = await fetch('/api/login/account', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ username, password }),
    })
    const data = await res.json()
    if (data.status === 'ok' && data.currentAuthority === 'admin') {
      setServerUrl(url)
      // 提取 token：新版服务 body → 旧版 Set-Cookie fallback
      if (data.token) {
        setToken(data.token)
      } else {
        const cookieHeader = res.headers.get('set-cookie') || ''
        const match = cookieHeader.match(/token=([^;]+)/)
        if (match) setToken(match[1])
      }
      return { ok: true }
    }
    return { ok: false }
  } catch {
    return { ok: false }
  }
}

// 验证 session 是否仍有效
// 1. 优先用保存的 token 验证（X-Token header）
// 2. token 为空时，尝试仅用 cookie（proxy 模式）
export async function verifySession(): Promise<boolean> {
  if (_token) {
    const r = await request('GET', '/api/currentUser')
    if (r.success) return true
    // authFailed 说明 token 已过期，不再尝试 cookie 模式
    if (r.authFailed) return false
  }
  // token 无效或为空：尝试仅用 cookie（proxy 模式）
  // 注意：不清除 _token，避免后续请求丢失认证
  const r = await request('GET', '/api/currentUser')
  return r.success
}

export async function logout() {
  try {
    await fetch('/api/login/outLogin', {
      method: 'POST',
      headers: { 'X-Token': _token },
      credentials: 'include',
    })
  } catch { /* ignore */ }
  clearConnection()
}

// ========== 用户信息 ==========

// currentUser 返回的 Route 对象
interface PluginRoute {
  path: string
  name: string
  component: string
  create_at?: string
}

export interface CurrentUser {
  name: string
  avatar: string
  plugins: PluginRoute[]
}

export async function getCurrentUser(): Promise<ApiResult<CurrentUser>> {
  const r = await request('GET', '/api/currentUser')
  if (r.authFailed) return r
  if (r.success && r.data?.success && r.data?.data) {
    return { success: true, data: r.data.data }
  }
  return { success: false, error: '获取用户信息失败' }
}

// 从 currentUser.plugins 解析为本地插件信息（含模块标识）
// 傻妞 currentUser 返回的 plugins 是全量 Functions 列表，包含所有 goja 模块
// 需要额外从 plugin_list API 获取公共插件的 metadata
export async function parseCurrentUserPlugins(routes: PluginRoute[]): Promise<MyPlugin[]> {
  // 提取 UUID 列表，批量查询公共插件的详细信息
  const uuids = routes.map(r => r.path.replace('/script/', '')).filter(Boolean)

  // 批量获取插件元信息（通过 list.json 的已安装 tab 匹配 UUID）
  const allPublicInfo = await getAllPublicPluginInfo(uuids)

  return routes.map(route => {
    const uuid = route.path.replace('/script/', '')
    const name = route.name
    // 去掉傻妞加的标记后缀（🔧 💫 🔒 👑）
    // 去掉傻妞加的后缀标记（🔧=模块 💫=自启 🔒=加密 👑=公共）
    const cleanName = name.replace(/[^\x00-\xff]+$/, '').trim()
    const isModule = name.includes('🔧')
    const isOnStart = name.includes('💫')
    const isEncrypt = name.includes('🔒')
    const isPublic = name.includes('👑')

    // 查找匹配的公共插件信息
    const publicInfo = allPublicInfo[uuid]

    return {
      id: uuid,
      name: cleanName,
      version: publicInfo?.version || '',
      description: publicInfo?.description || '',
      author: publicInfo?.author || '',
      enabled: publicInfo ? !publicInfo.disable : true,
      public: isPublic,
      running: publicInfo?.running ?? false,
      disable: publicInfo?.disable ?? false,
      category: (publicInfo?.classes && publicInfo.classes[0]) || '未分类',
      downloads: publicInfo?.downloads || 0,
      icon: publicInfo?.icon || '',
      debug: publicInfo?.debug ?? false,
      has_form: publicInfo?.has_form ?? false,
      status: publicInfo?.status || 2,
      on_start: isOnStart,
      encrypt: isEncrypt,
      module: isModule,
      organization: publicInfo?.organization || '',
      address: publicInfo?.address || '',
      create_at: route.create_at || '',
    }
  })
}

// 批量从公共插件列表获取插件元信息
async function getAllPublicPluginInfo(uuids: string[]): Promise<Record<string, Partial<LocalPlugin>>> {
  const result: Record<string, Partial<LocalPlugin>> = {}
  const uuidSet = new Set(uuids)

  // 分页拉取所有已安装插件（tab1）+ 未安装（tab2）+ 可更新（tab3）
  const pageSize = 100
  try {
    const resultApi = await getPluginList({ current: 1, pageSize: pageSize, activeKey: 'tab1' })
    if (resultApi.success && resultApi.data?.data) {
      for (const p of resultApi.data.data) {
        if (uuidSet.has(p.id)) {
          result[p.id] = p
        }
      }
    }
  } catch { /* ignore */ }

  return result
}

export interface MyPlugin {
  id: string
  name: string
  version: string
  description: string
  author: string
  enabled: boolean
  public: boolean
  running: boolean
  disable: boolean
  category: string
  downloads: number
  icon: string
  debug: boolean
  has_form: boolean
  status: number
  on_start: boolean
  encrypt: boolean
  module: boolean
  organization: string
  address: string
  create_at: string
}

// ========== 插件 ==========

export interface LocalPlugin {
  id: string
  title: string
  type: string
  suffix: string
  description: string
  public: boolean
  icon: string
  version: string
  author: string
  status: number
  create_at: string
  module: boolean
  encrypt: boolean
  on_start: boolean
  running: boolean
  disable: boolean
  debug: boolean
  has_form: boolean
  downloads: number
  carry: boolean
  messages: any[]
  classes: string[]
  organization: string
  address: string
  identified: boolean
}

export interface PluginListResult {
  success: boolean
  data: LocalPlugin[]
  total: number
  page: number
  tab1: number
  tab2: number
  tab3: number
  tab: string
  classes: Record<string, number>
  origins: Record<string, string>
}

export async function getPluginList(params: {
  current?: number
  pageSize?: number
  activeKey?: string
  keyword?: string
  class?: string
  init?: string
}): Promise<ApiResult<PluginListResult>> {
  const q = new URLSearchParams()
  if (params.current) q.set('current', String(params.current))
  if (params.pageSize) q.set('pageSize', String(params.pageSize))
  if (params.activeKey) q.set('activeKey', params.activeKey)
  if (params.keyword) q.set('keyword', params.keyword)
  if (params.class) q.set('class', params.class)
  if (params.init) q.set('init', params.init)
  const r = await request('GET', `/api/plugins/list.json?${q.toString()}`)
  if (r.authFailed) return r
  if (r.data?.success) {
    return {
      success: true,
      data: {
        success: true,
        data: r.data.data || [],
        total: r.data.total || 0,
        page: r.data.page || 1,
        tab1: r.data.tab1 || 0,
        tab2: r.data.tab2 || 0,
        tab3: r.data.tab3 || 0,
        tab: r.data.tab || '',
        classes: r.data.classes || {},
        origins: r.data.origins || {},
      }
    }
  }
  return { success: false, error: '获取插件列表失败' }
}

// ========== 定时任务 ==========

export interface TaskItem {
  id: number
  task_id: string
  title: string
  schedule: string
  command: string
  scripts: string[]
  senders: { chat_id: string; user_id: string; platform: string; bot_id: string }[]
  created_at: number
  remark: string
  enable: boolean
  icons: { link: string; title: string }[]
}

export async function getTasks(current = 1, pageSize = 20): Promise<ApiResult<TaskItem[]>> {
  const r = await request('GET', `/api/tasks?current=${current}&pageSize=${pageSize}`)
  if (r.authFailed) return r
  if (r.data?.success) {
    return { success: true, data: r.data.data || [] }
  }
  return { success: false, error: '获取任务列表失败' }
}

export async function updateTask(task: Partial<TaskItem>): Promise<ApiResult> {
  return request('POST', '/api/tasks', task)
}

export async function deleteTask(task_id: string): Promise<ApiResult> {
  return request('DELETE', '/api/tasks', { task_id })
}

export async function runTask(task_id: string): Promise<ApiResult> {
  return request('GET', `/api/tasks/run?task_id=${task_id}`)
}

// ========== 管理员 ==========

export interface MasterItem {
  id: number
  platform: string
  nickname: string
  number: string
  unix: number
}

export interface MastersResult {
  masters: MasterItem[]
  platforms: { label: string; value: string }[]
}

export async function getMasters(): Promise<ApiResult<MastersResult>> {
  const r = await request('GET', '/api/master/list')
  if (r.authFailed) return r
  if (r.data?.success) {
    return {
      success: true,
      data: {
        masters: r.data.data || [],
        platforms: r.data.platforms || [],
      }
    }
  }
  return { success: false, error: '获取管理员列表失败' }
}

export async function addMaster(master: { platform: string; ID: string }): Promise<ApiResult> {
  const r = await request('POST', '/api/master', master)
  return r
}

export async function removeMaster(master: { platform: string; ID: string }): Promise<ApiResult> {
  const r = await request('DELETE', '/api/master', master)
  return r
}

// ========== 分佣系统 ==========

export interface FenyongStat {
  order_num: number | string
  user_num: number | string
  total_actual: number | string
  total_estimate: number | string
  total_rake_actual: number | string
  total_rake_estimate: number | string
  total_irake_actual: number | string
  total_irake_estimate: number | string
  total_irake_actual_pct: number | string
  total_irake_estimate_pct: number | string
  total_iactual: number | string
  total_iestimate: number | string
}

export interface FenyongTab {
  key: string
  title: string
  value: string
}

export interface FenyongRow {
  name: string
  image: string
  sku_name: string
  status: string
  content?: { label: string; status: string; value: string }[]
  bind?: boolean
}

export interface FenyongResult {
  tongji: FenyongStat
  tabs: FenyongTab[]
  data: FenyongRow[]
  total: number
}

export async function getFenyong(params: {
  init?: boolean
  user?: string
  startTime?: number
  endTime?: number
  activeKey?: string
  current?: number
  pageSize?: number
}): Promise<ApiResult<FenyongResult>> {
  const qs = new URLSearchParams()
  if (params.init) qs.append('init', 'true')
  if (params.user) qs.append('user', params.user)
  if (params.startTime) qs.append('startTime', String(params.startTime))
  if (params.endTime) qs.append('endTime', String(params.endTime))
  if (params.activeKey) qs.append('activeKey', params.activeKey)
  if (params.current) qs.append('current', String(params.current))
  if (params.pageSize) qs.append('pageSize', String(params.pageSize))
  const r = await request('GET', `/api/fenyong?${qs.toString()}`)
  if (r.authFailed) return r
  if (r.data?.success) {
    return { success: true, data: r.data as unknown as FenyongResult }
  }
  return { success: false, error: '获取分佣数据失败' }
}

export async function saveFenyoySettings(values: Record<string, any>): Promise<ApiResult> {
  const r = await request('PUT', '/api/storage', values)
  return r
}

// ========== 存储 ==========

export interface StorageItem {
  bucket: string
  key: string
  value: string
  index: string
}

export async function getStorageList(keys: string, current = 1, pageSize = 20): Promise<ApiResult<StorageItem[]>> {
  const r = await request('GET', `/api/storage/list?keys=${encodeURIComponent(keys)}&current=${current}&pageSize=${pageSize}`)
  if (r.authFailed) return r
  if (r.data?.success) {
    return { success: true, data: r.data.data || [] }
  }
  return { success: false, error: '获取存储数据失败' }
}

export async function getStorage(keys: string, search?: string): Promise<ApiResult> {
  let path = `/api/storage?keys=${encodeURIComponent(keys)}`
  if (search) path += `&search=${encodeURIComponent(search)}`
  const r = await request('GET', path)
  if (r.authFailed) return r
  if (r.data?.success) {
    return r.data
  }
  return { success: false }
}

// ========== 载波 ==========

export interface CarryGroup {
  id: number
  in: boolean
  out: boolean
  from: string[] | null
  chat_id: string
  user_id: string
  chat_name: string
  platform: string
  enable: boolean
  remark: string
  created_at: number
}

export async function getCarryGroups(): Promise<ApiResult<CarryGroup[]>> {
  const r = await request('GET', '/api/carry/groups')
  if (r.authFailed) return r
  if (r.data?.success) {
    return { success: true, data: r.data.data || [] }
  }
  return { success: false }
}

// ========== 自动回复 ==========

export interface ReplyItem {
  id: number
  keyword: string
  reply: string
  platform: string
  enable: boolean
}

export async function getReplyList(): Promise<ApiResult<ReplyItem[]>> {
  const r = await request('GET', '/api/reply/list')
  if (r.authFailed) return r
  if (r.data?.success) {
    return { success: true, data: r.data.data || [] }
  }
  return { success: false }
}

// ========== 插件操作 ==========
// 傻妞通过 plugins bucket 管理插件：
//   plugins.{uuid} = 脚本源码 → 触发加载/重载
//   plugins.{uuid} = "" → 卸载
//   plugins.{uuid} = "install" → 安装（标记）
//   plugin_debug.{uuid} = "b:true" → 开启调试
//   plugin_debug.{uuid} = "b:false" → 关闭调试

// 切换插件启用/禁用（通过 toggle disable key）
// disable 状态通过 PUT storage plugins.disable.{uuid} 控制
export async function togglePlugin(uuid: string, disabled: boolean): Promise<ApiResult> {
  const key = `plugins.disable.${uuid}`
  return request('PUT', `/api/storage?uuid=${uuid}`, { [key]: disabled }, { 'Content-Type': 'application/json' })
}

// 获取插件源码
// 傻妞的 GET /api/storage?keys=plugins.{uuid} 返回 bucket 数据 + checkFilePlugin 补充的源码
// 但傻妞的 bucket 里可能没有这个 key，或者 checkFilePlugin 找不到对应的 UUID
// 所以需要尝试直接获取
export async function getPluginSource(uuid: string): Promise<ApiResult<string>> {
  const r = await request('GET', `/api/storage?keys=plugins.${uuid}`)
  if (r.authFailed) return r
  if (!r.success) return { success: false, error: '获取插件源码失败' }
  const data = r.data as any
  const source = data?.data?.[`plugins.${uuid}`]
  if (source && typeof source === 'string' && !source.startsWith('非法操作') && !source.startsWith('非法访问')) {
    return { success: true, data: source }
  }
  return { success: false, error: '获取插件源码失败（插件可能不支持在线编辑）' }
}

// 编辑插件源码（保存即触发重载）
export async function savePluginSource(uuid: string, source: string): Promise<ApiResult> {
  return request('PUT', `/api/storage?uuid=${uuid}`, { [`plugins.${uuid}`]: source })
}

// 卸载插件（将 plugins.{uuid} 设为空）
export async function uninstallPlugin(uuid: string): Promise<ApiResult> {
  return request('PUT', `/api/storage?uuid=${uuid}`, { [`plugins.${uuid}`]: '' })
}

// 删除插件（不重装，直接删除）
export async function deletePluginSource(uuid: string): Promise<ApiResult> {
  return request('PUT', `/api/storage?uuid=${uuid}`, { [`plugins.${uuid}`]: '' })
}

// 重载插件（触发 reload）
export async function reloadPlugin(uuid: string): Promise<ApiResult> {
  // 直接设置值为 'reload' 触发重载
  const r = await request('PUT', `/api/storage?uuid=${uuid}`, { [`plugins.${uuid}`]: 'reload' })
  if (!r.success) {
    // 编译型插件可能返回错误，转换为明确信息
    if (r.error?.includes('expected') || r.error?.includes('reload') || r.error?.includes('not found')) {
      return { success: false, error: '该插件不支持通过 API 重载（可能是编译型插件）', authFailed: r.authFailed }
    }
    return { success: false, error: r.error || '重载失败' }
  }
  return r
}

// 获取插件 form 字段已保存的值
// 傻妞 @form key 格式: "dingtalk.client_id"（完整 storage key = bucket.key）
// 傻妞管理页面把 form key + plugin_debug.{uuid} + plugin_disable.{uuid} 一起传入
// 返回格式: { success: true, data: { "bucket.key": transformedValue, ... } }
export async function getPluginFormValues(uuid: string, formSchema: any[]): Promise<ApiResult<Record<string, any>>> {
  if (formSchema.length === 0) {
    return { success: true, data: {} }
  }
  // 构建 keys 参数: dingtalk.client_id,dingtalk.client_secret,...,plugin_debug.{uuid},plugin_disable.{uuid}
  const keys = [...formSchema.map(f => f.key), `plugin_debug.${uuid}`, `plugin_disable.${uuid}`].join(',')
  const r = await request('GET', `/api/storage?keys=${keys}`)
  if (r.authFailed) return r
  if (!r.success) return { success: false, error: '获取 form 值失败' }
  // r.data 是完整响应 {success, data: {...}}，需要取内层 data
  const raw = r.data as any
  const data = raw?.data as Record<string, any> || {}
  // 按 @form schema 返回 form 字段的值
  // TransformBucketKeyValue 已做转换: b:true → true(boolean), d:123 → 123(number), f:1.5 → 1.5(number), 纯字符串原样返回
  const values: Record<string, any> = {}
  for (const field of formSchema) {
    const val = data[field.key]
    // 非 null/undefined 就使用（布尔值 false 也是有效值）
    if (val !== undefined && val !== null) {
      values[field.key] = val
    }
  }
  // 注入 debug 和 disable 状态（TransformBucketKeyValue 已将 b:true/b:false 转为 boolean）
  const debugKey = `plugin_debug.${uuid}`
  values['__debug__'] = data[debugKey] === true
  const disableKey = `plugin_disable.${uuid}`
  values['__disable__'] = data[disableKey] === false
  return { success: true, data: values }
}

// 切换调试模式 — 保存一组 key-value 到傻妞 storage
export async function savePlugin(uuid: string, updates: Record<string, any>): Promise<ApiResult> {
  return request('PUT', `/api/storage?uuid=${uuid}`, updates)
}

// 获取插件列表（用于安装市场）
export async function getAvailablePlugins(origin?: string): Promise<ApiResult<LocalPlugin[]>> {
  const q = new URLSearchParams()
  if (origin) q.set('origin[]', origin)
  // 获取未安装和可更新的插件
  const r = await request('GET', `/api/plugins/list.json?current=1&pageSize=100&activeKey=tab2&${q.toString()}`)
  if (r.authFailed) return r
  if (r.data?.success) {
    return { success: true, data: r.data.data || [] }
  }
  return { success: false, error: '获取插件列表失败' }
}

// 安装插件（从外部源）
export async function installPlugin(uuid: string, source: string): Promise<ApiResult> {
  return request('PUT', `/api/storage?uuid=${uuid}`, { [`plugins.${uuid}`]: source })
}

// 更新插件
export async function updatePlugin(uuid: string, source: string): Promise<ApiResult> {
  return savePluginSource(uuid, source)
}

// 傻妞 bucket 值类型转换（对应 Go 端的 TransformBucketKeyValue）
export function TransformBucketKeyValue(v: string): any {
  if (v.startsWith('f:')) {
    return parseFloat(v.slice(2))
  }
  if (v.startsWith('d:')) {
    return parseInt(v.slice(2), 10)
  }
  if (v.startsWith('b:')) {
    return v.slice(2) === 'true'
  }
  if (v.startsWith('o:')) {
    try {
      return JSON.parse(v.slice(2))
    } catch {
      return v.slice(2)
    }
  }
  if (v === '') {
    return null
  }
  return v
}

// ========== WebSocket ==========

export function createWebSocket(baseUrl: string): WebSocket {
  const wsUrl = baseUrl.replace(/^http/, 'ws')
  return new WebSocket(wsUrl)
}
