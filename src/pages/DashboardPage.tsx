import React, { useEffect, useState } from 'react'
import { Card, Grid, Tag, Button, Space } from 'antd-mobile'
import { useAppStore } from '../stores/appStore'
import { getCurrentUser, getTasks, getMasters } from '../utils/api'

interface PeriodStat {
  orders: number
  estimate: number
  actual: number
}

interface PlatformStat {
  orders: number
  estimate: number
  actual: number
}

interface DashboardData {
  today: PeriodStat
  yesterday: PeriodStat
  last7days: PeriodStat
  lastMonth: PeriodStat
  platforms: Record<string, PlatformStat>
}

const DashboardPage: React.FC = () => {
  const { currentService, currentUserPlugins, actions } = useAppStore()
  const [serverName, setServerName] = useState('')
  const [installedCount, setInstalledCount] = useState(0)
  const [taskCount, setTaskCount] = useState(0)
  const [masterCount, setMasterCount] = useState(0)
  const [fenyong, setFenyong] = useState<DashboardData | null>(null)

  // 从 store 实时读取 plugins 长度
  useEffect(() => {
    setInstalledCount(currentUserPlugins?.length || 0)
  }, [currentUserPlugins])

  const fetchDashboard = async () => {
    if (!currentService) return
    // 一次性获取 currentUser，缓存 plugins 供全局复用
    try {
      const user = await getCurrentUser()
      if (user.success && user.data) {
        setServerName(user.data.name)
        actions.setCurrentUserPlugins(user.data.plugins)
      }
    } catch {}
    // 后续数据直接读 store 缓存，不再重复请求
    try {
      const tasks = await getTasks(1, 1)
      if (tasks.success && tasks.data) setTaskCount(tasks.data.length)
    } catch {}
    try {
      const masters = await getMasters()
      if (masters.success && masters.data?.masters) setMasterCount(masters.data.masters.length)
    } catch {}
    // 分佣数据
    try {
      const r = await fetch('/api/fenyong/dashboard', { credentials: 'include' })
      const d = await r.json()
      if (d && d.today) {
        setFenyong(d as DashboardData)
      }
    } catch { /* silent */ }
  }

  useEffect(() => {
    if (currentService) {
      fetchDashboard()
      actions.refreshSystemStatus()
    }
  }, [currentService]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="page-container">
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>控制台</h2>
        <p style={{ margin: '8px 0 0 0', color: '#666', fontSize: 14 }}>
          {currentService ? `当前连接: ${currentService.name}` : '未连接到任何服务'}
        </p>
      </div>

      {!currentService ? (
        <div className="empty-container">
          <div style={{ fontSize: 48, marginBottom: 12 }}>📱</div>
          <div style={{ fontSize: 16, marginBottom: 8 }}>暂无连接的服务</div>
          <div style={{ fontSize: 14, color: '#999', marginBottom: 16 }}>
            请前往"服务管理"页面添加并连接服务
          </div>
          <Button color='primary' onClick={() => actions.setActiveTab('service')}>
            去连接服务
          </Button>
        </div>
      ) : (
        <div>
          {/* 服务状态卡片 */}
          <Card title={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <div style={{ fontSize: 16, fontWeight: 500 }}>
                  {serverName || currentService.name}
                </div>
                <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                  {currentService.url}
                </div>
              </div>
              <Tag color={currentService.status === 'online' ? 'success' : 'default'}>
                {currentService.status === 'online' ? '在线' : '离线'}
              </Tag>
            </div>
          }>
            <Space>
              <button 
                className="btn-secondary"
                onClick={() => actions.disconnectService(currentService.id)}
              >
                断开连接
              </button>
              <button 
                className="btn-primary"
                onClick={fetchDashboard}
              >
                刷新状态
              </button>
            </Space>
          </Card>

          {/* 概览统计 */}
          <Card title="概览" style={{ marginTop: 12 }}>
            <Grid columns={3} gap={12}>
              <Grid.Item>
                <div 
                  className="stat-item" 
                  style={{ textAlign: 'center', cursor: 'pointer' }}
                  onClick={() => actions.setActiveTab('myplugins')}
                >
                  <div style={{ fontSize: 12, color: '#666', marginBottom: 4 }}>已安装插件</div>
                  <div style={{ fontSize: 28, fontWeight: 600, color: '#1677ff' }}>{installedCount}</div>
                </div>
              </Grid.Item>
              <Grid.Item>
                <div 
                  className="stat-item" 
                  style={{ textAlign: 'center', cursor: 'pointer' }}
                  onClick={() => actions.setActiveTab('admin')}
                >
                  <div style={{ fontSize: 12, color: '#666', marginBottom: 4 }}>管理员</div>
                  <div style={{ fontSize: 28, fontWeight: 600, color: '#ff9800' }}>{masterCount}</div>
                </div>
              </Grid.Item>
              <Grid.Item>
                <div 
                  className="stat-item" 
                  style={{ textAlign: 'center' }}
                >
                  <div style={{ fontSize: 12, color: '#666', marginBottom: 4 }}>定时任务</div>
                  <div style={{ fontSize: 28, fontWeight: 600, color: '#52c41a' }}>{taskCount}</div>
                  <div style={{ fontSize: 10, color: '#bbb', marginTop: 4 }}>开发中</div>
                </div>
              </Grid.Item>
            </Grid>
          </Card>

          {/* 分佣概览 */}
          {fenyong && (
            <Card 
              title="分佣" 
              style={{ marginTop: 12, cursor: 'pointer' }}
              onClick={() => actions.setActiveTab('fenyong')}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                <div style={{
                  width: 44, height: 44, borderRadius: 12,
                  background: 'linear-gradient(135deg, #667eea, #764ba2)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 22, color: '#fff',
                }}>💰</div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 11, color: '#999' }}>今日预估佣金</div>
                  <div style={{ fontSize: 28, fontWeight: 700, color: '#1677ff' }}>¥{(fenyong.today.estimate || 0).toFixed(2)}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 11, color: '#999' }}>今日订单</div>
                  <div style={{ fontSize: 18, fontWeight: 600 }}>{(fenyong.today.orders || 0).toString()} 单</div>
                </div>
              </div>
              {/* 时间对比 */}
              <div style={{
                display: 'grid', gridTemplateColumns: '1fr 1fr 1fr',
                gap: 8, paddingTop: 8, borderTop: '1px solid #f0f0f0',
              }}>
                {[
                  { label: '昨日', stat: fenyong.yesterday },
                  { label: '近7日', stat: fenyong.last7days },
                  { label: '近月', stat: fenyong.lastMonth },
                ].map(p => {
                  const $ = (n?: number) => n == null ? '—' : `¥${n.toFixed(2)}`
                  return (
                    <div key={p.label} style={{ textAlign: 'center' }}>
                      <div style={{ fontSize: 11, color: '#999' }}>{p.label}</div>
                      <div style={{ fontSize: 16, fontWeight: 600, color: '#333' }}>{$(p.stat.estimate)}</div>
                      <div style={{ fontSize: 11, color: '#999' }}>{(p.stat.orders || 0).toString()}单</div>
                    </div>
                  )
                })}
              </div>
              {/* 平台收益 */}
              <div style={{ marginTop: 12 }}>
                {Object.entries(fenyong.platforms || {}).slice(0, 3).map(([site, stat]) => {
                  const PLATFORM_NAME: Record<string, string> = { jd: '京东', tb: '淘宝', pdd: '拼多多' }
                  const PLATFORM_BG: Record<string, string> = { jd: '#e84118', tb: '#ff6b81', pdd: '#2ed573' }
                  const $ = (n?: number) => n == null ? '—' : `¥${n.toFixed(2)}`
                  return (
                    <div key={site} style={{ display: 'flex', alignItems: 'center', padding: '6px 0' }}>
                      <div style={{ width: 8, height: 8, borderRadius: 4, background: PLATFORM_BG[site] || '#ccc', marginRight: 8 }} />
                      <div style={{ flex: 1, fontSize: 14 }}>{PLATFORM_NAME[site] || site}</div>
                      <div style={{ fontSize: 12, color: '#999', marginRight: 8 }}>{(stat.orders || 0).toString()}单</div>
                      <div style={{ fontSize: 14, fontWeight: 600, color: '#1677ff' }}>{$(stat.estimate)}</div>
                    </div>
                  )
                })}
              </div>
            </Card>
          )}

          {/* 快速操作 */}
          <Card title="快速操作" style={{ marginTop: 12 }}>
            <Grid columns={3} gap={8}>
              <Grid.Item>
                <button 
                  className="btn-secondary"
                  style={{ width: '100%', height: 48 }}
                  onClick={() => actions.setActiveTab('plugin')}
                >
                  插件管理
                </button>
              </Grid.Item>
              <Grid.Item>
                <button 
                  className="btn-secondary"
                  style={{ width: '100%', height: 48 }}
                  onClick={() => actions.setActiveTab('admin')}
                >
                  管理员
                </button>
              </Grid.Item>
              <Grid.Item>
                <button 
                  className="btn-secondary"
                  style={{ width: '100%', height: 48 }}
                  onClick={() => actions.setActiveTab('settings')}
                >
                  更多功能
                </button>
              </Grid.Item>
            </Grid>
          </Card>
        </div>
      )}
    </div>
  )
}

export default DashboardPage
