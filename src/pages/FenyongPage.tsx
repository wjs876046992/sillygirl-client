import React, { useEffect, useState, useCallback } from 'react'
import { Card, SearchBar, Loading, Tag } from 'antd-mobile'

function formatOrderTime(ts: number | string | undefined | null): string {
  if (ts == null || ts === 0 || ts === '0' || ts === '') return '—'
  const d = new Date(Number(ts) * 1000)
  if (isNaN(d.getTime())) return '—'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

import { useAppStore } from '../stores/appStore'

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
  total_settled: number
  total_unsettled: number
  total_orders: number
}

interface OrderItem {
  sku_name: string
  image: string
  status: string
  content: Array<{ label: string; value: any; status?: string }>
  site: string
  created_time: number
  order_id: string
}

export const EMPTY_DASHBOARD: DashboardData = {
  today: { orders: 0, estimate: 0, actual: 0 },
  yesterday: { orders: 0, estimate: 0, actual: 0 },
  last7days: { orders: 0, estimate: 0, actual: 0 },
  lastMonth: { orders: 0, estimate: 0, actual: 0 },
  platforms: {},
  total_settled: 0,
  total_unsettled: 0,
  total_orders: 0,
}

export const PLATFORM_BG: Record<string, string> = { jd: '#e84118', tb: '#ff6b81', pdd: '#2ed573' }
export const PLATFORM_NAME: Record<string, string> = { jd: '京东', tb: '淘宝', pdd: '拼多多' }

const $ = (n?: number) => {
  if (n === undefined || n === null) return '—'
  return '¥' + n.toFixed(2)
}

interface FenyongSummaryProps {
  data: DashboardData
}

export const FenyongSummary: React.FC<FenyongSummaryProps> = ({ data }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {/* 今日预览 */}
      <Card>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
          <div style={{
            width: 44, height: 44, borderRadius: 12,
            background: 'linear-gradient(135deg, #667eea, #764ba2)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 22, color: '#fff',
          }}>💰</div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 11, color: '#999' }}>今日预估佣金</div>
            <div style={{ fontSize: 28, fontWeight: 700, color: '#1677ff' }}>{$(data.today.estimate)}</div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 11, color: '#999' }}>今日订单</div>
            <div style={{ fontSize: 18, fontWeight: 600 }}>{(data.today.orders || 0).toString()} 单</div>
          </div>
        </div>
        {/* 时间对比 */}
        <div style={{
          display: 'grid', gridTemplateColumns: '1fr 1fr 1fr',
          gap: 8, marginTop: 8, paddingTop: 12,
          borderTop: '1px solid #f0f0f0',
        }}>
          {[
            { label: '昨日', stat: data.yesterday },
            { label: '近7日', stat: data.last7days },
            { label: '近月', stat: data.lastMonth },
          ].map(p => (
            <div key={p.label} style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 11, color: '#999' }}>{p.label}</div>
              <div style={{ fontSize: 16, fontWeight: 600, color: '#333' }}>{$(p.stat.estimate)}</div>
              <div style={{ fontSize: 11, color: '#999' }}>{(p.stat.orders || 0).toString()}单</div>
            </div>
          ))}
        </div>
      </Card>

      {/* 平台收益对比 */}
      <Card title='平台收益（今日）'>
        {Object.keys(PLATFORM_NAME).map(site => {
          const stat = data.platforms?.[site]
          return (
            <div key={site} style={{ display: 'flex', alignItems: 'center', padding: '8px 0', borderBottom: '1px solid #f5f5f5' }}>
              <div style={{ width: 8, height: 8, borderRadius: 4, background: PLATFORM_BG[site] || '#ccc', marginRight: 8 }} />
              <div style={{ flex: 1, fontSize: 14 }}>{PLATFORM_NAME[site]}</div>
              <div style={{ fontSize: 12, color: '#999', marginRight: 8 }}>{(stat?.orders || 0).toString()}单</div>
              <div style={{ fontSize: 14, fontWeight: 600, color: '#1677ff' }}>{$(stat?.estimate)}</div>
            </div>
          )
        })}
      </Card>
    </div>
  )
}

const FenyongPage: React.FC = () => {
  const { currentService } = useAppStore()
  const [orders, setOrders] = useState<OrderItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(false)

  const loadOrders = useCallback(async (p: number, searchKeyword?: string) => {
    setLoading(true)
    setPage(p)
    const kw = searchKeyword !== undefined ? searchKeyword : keyword
    try {
      const q = `page=${p}&pageSize=20${kw ? '&keyword=' + encodeURIComponent(kw) : ''}`
      const r = await fetch(`/api/fenyong/orders?${q}`, { credentials: 'include' })
      const d = await r.json()
      if (d?.success) {
        setOrders(d.data || [])
        setTotal(d.total || 0)
      }
    } catch { /* silent */ } finally {
      setLoading(false)
    }
  }, [keyword])

  useEffect(() => { loadOrders(1) }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => loadOrders(1, keyword)
  const handleClear = () => { setKeyword(''); loadOrders(1, '') }
  const handlePrev = () => { if (page > 1) loadOrders(page - 1) }
  const handleNext = () => { if (page * 20 < total) loadOrders(page + 1) }

  if (!currentService) {
    return (
      <div className="page-container" style={{ textAlign: 'center', padding: 48, color: '#999' }}>
        <div style={{ fontSize: 48, marginBottom: 12 }}>📊</div>
        <div>未连接服务</div>
      </div>
    )
  }

  return (
    <div className="page-container" style={{ maxWidth: 500, margin: '0 auto' }}>
      <h2 style={{ margin: '0 0 12px 0', fontSize: 20, fontWeight: 600 }}>分佣系统</h2>

      {loading && <div style={{ textAlign: 'center', padding: 24 }}><Loading color='primary' /></div>}

      <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div>
          <SearchBar
            placeholder='搜索SKU/订单号'
            value={keyword}
            onChange={setKeyword}
            onClear={handleClear}
            onSearch={handleSearch}
            style={{ marginBottom: 8 }}
          />
          {!loading && orders.length === 0 && (
            <div style={{ textAlign: 'center', padding: 32, color: '#999', fontSize: 14 }}>暂无订单</div>
          )}
            {orders.map((row, i) => (
              <Card key={row.order_id || row.sku_name || i} style={{ marginBottom: 0 }}>
                <div style={{ display: 'flex', gap: 10 }}>
                  <div style={{
                    width: 68, height: 68, borderRadius: 8,
                    background: '#f5f5f5', flexShrink: 0,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 28, overflow: 'hidden',
                  }}>
                    {row.image
                      ? <img src={row.image} alt='' style={{ width: 40, height: 40, objectFit: 'contain' }} />
                      : '🛒'}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{
                      fontSize: 14, fontWeight: 500,
                      wordBreak: 'break-all',
                      lineHeight: 1.5,
                    }}>{row.sku_name || '—'}</div>
                    <div style={{ fontSize: 11, color: '#999', marginTop: 2, display: 'flex', alignItems: 'center', gap: 4, flexWrap: 'wrap' }}>
                      {row.site && PLATFORM_NAME[row.site] && (
                        <Tag color='primary' fill='outline' style={{ fontSize: 9 }}>{PLATFORM_NAME[row.site]}</Tag>
                      )}
                      {row.site && !PLATFORM_NAME[row.site] && (
                        <Tag color='default' fill='outline' style={{ fontSize: 9 }}>{row.site}</Tag>
                      )}
                      <span>{row.status?.split(' ').slice(-1)[0]}</span>
                      {row.created_time > 0 && (
                        <span style={{ marginLeft: 'auto', fontSize: 10, color: '#bbb' }}>{formatOrderTime(row.created_time)}</span>
                      )}
                    </div>
                  </div>
                </div>
                {/* 金额三合一 */}
                <div style={{
                  display: 'flex', gap: 8, marginTop: 8,
                  padding: '8px 0', borderTop: '1px solid #f5f5f5',
                }}>
                  {[{ label: '订单金额', ci: 1 }, { label: '预估佣金', ci: 2 }, { label: '实际佣金', ci: 3 }].map(({ label, ci }) => {
                    const item = row.content[ci]
                    if (!item) return null
                    return (
                      <div key={label} style={{ flex: 1, textAlign: 'center' }}>
                        <div style={{ fontSize: 10, color: '#999' }}>{label}</div>
                        <div style={{
                          fontSize: 14, fontWeight: 600,
                          color: ci === 2 ? '#1677ff' : ci === 3 ? '#52c41a' : '#333',
                        }}>¥{item.value ?? '—'}</div>
                      </div>
                    )
                  })}
                </div>
              </Card>
            ))}
          </div>

          {/* 分页 */}
          {total > 20 && (
            <div style={{ display: 'flex', justifyContent: 'center', gap: 12, padding: 16 }}>
              <div onClick={handlePrev} style={{
                padding: '6px 16px', borderRadius: 8,
                background: page > 1 ? '#1677ff' : '#f0f0f0',
                color: page > 1 ? '#fff' : '#ccc',
                fontSize: 13, cursor: page > 1 ? 'pointer' : 'default',
              }}>上一页</div>
              <div style={{ padding: '6px 0', fontSize: 13, color: '#666', display: 'flex', alignItems: 'center' }}>
                {page} / {Math.ceil(total / 20)}
              </div>
              <div onClick={handleNext} style={{
                padding: '6px 16px', borderRadius: 8,
                background: page * 20 < total ? '#1677ff' : '#f0f0f0',
                color: page * 20 < total ? '#fff' : '#ccc',
                fontSize: 13, cursor: page * 20 < total ? 'pointer' : 'default',
              }}>下一页</div>
            </div>
          )}
        </div>
    </div>
  )
}

export default FenyongPage
