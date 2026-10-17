import { useState, useEffect, useCallback } from 'react'
import PageHeader from '../components/PageHeader'
import GasChart from '../components/GasChart'
import {
  getSensorTrends,
  getRiskDistribution,
  getAlertTimeline,
  getAlertsBySensor,
  getValveActivityStats,
} from '../api/leaksenseApi'
import { useData } from '../context/DataContext'
import {
  ResponsiveContainer,
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ReferenceLine,
} from 'recharts'
import {
  BoltIcon,
  TrendingUpIcon,
  RefreshIcon,
  GaugeIcon,
  WindIcon,
  TargetIcon,
  BatteryIcon,
  ShieldIcon,
  CalendarIcon,
  MicroscopeIcon,
  ValveIcon,
  LockIcon,
  UnlockIcon,
} from '../components/icons/AppIcons'

const RISK_COLORS = {
  SAFE: '#22c55e',
  LOW: '#818cf8',
  MODERATE: '#f59e0b',
  HIGH: '#f97316',
  CRITICAL: '#ef4444',
}

const SENSOR_TYPE_COLORS = {
  device: '#4F46E5',    // Corporate Primary Indigo
  methane: '#ef4444',   // Critical Red
  sensor: '#f59e0b',    // Warning Amber
  pressure: '#10b981',  // Emerald Green
  valve: '#8b5cf6',     // Violet / Purple
  zone: '#ec4899',      // Pink / Magenta
  oxygen: '#0d9488',    // Deep Atmospheric Teal (Uniquely distinct from device indigo)
  battery: '#eab308',   // Yellow
  hardware: '#64748b',  // Slate
}

const PIE_COLORS = [
  '#4F46E5', // Corporate Primary Indigo (Device)
  '#ef4444', // Red (Methane)
  '#f59e0b', // Amber (Sensor)
  '#10b981', // Emerald (Pressure)
  '#8b5cf6', // Violet (Valve)
  '#ec4899', // Pink (Zone)
  '#0d9488', // Deep Teal (Oxygen)
  '#f97316', // Orange
  '#4338CA', // Indigo hover
  '#6366f1', // Indigo
]

const getSensorSliceColor = (sensorName, index) => {
  const key = String(sensorName || '').trim().toLowerCase()
  if (SENSOR_TYPE_COLORS[key]) {
    return SENSOR_TYPE_COLORS[key]
  }
  return PIE_COLORS[index % PIE_COLORS.length]
}

export default function AnalyticsPage() {
  const { nodes, readings, live } = useData()

  // Active view tab: 'historical' | 'live'
  const [activeTab, setActiveTab] = useState('historical')

  // Trend Controls
  const [selectedNodeId, setSelectedNodeId] = useState('')
  const [trendHours, setTrendHours] = useState(48)
  const [trendMetric, setTrendMetric] = useState('all') // 'all', 'methane', 'pressure', 'oxygen', 'risk'

  // Timeline Controls
  const [timelineDays, setTimelineDays] = useState(7)

  // Data States
  const [trends, setTrends] = useState([])
  const [riskData, setRiskData] = useState(null)
  const [timeline, setTimeline] = useState([])
  const [sensorDist, setSensorDist] = useState([])
  const [valveStats, setValveStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadAllAnalytics = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [trendsRes, riskRes, timelineRes, sensorRes, valveRes] = await Promise.all([
        getSensorTrends(selectedNodeId || undefined, trendHours),
        getRiskDistribution(),
        getAlertTimeline(timelineDays),
        getAlertsBySensor(),
        getValveActivityStats(),
      ])

      setTrends(trendsRes.data || [])
      setRiskData(riskRes.data || null)
      setTimeline(timelineRes.data || [])
      setSensorDist(sensorRes.data || [])
      setValveStats(valveRes.data || null)
    } catch {
      setError('Unable to load analytics data. Please verify the backend connection.')
    } finally {
      setLoading(false)
    }
  }, [selectedNodeId, trendHours, timelineDays])

  useEffect(() => {
    loadAllAnalytics()
  }, [loadAllAnalytics])

  const formattedTrends = trends.map((item) => {
    let timeLabel
    try {
      const d = new Date(item.recordedAt)
      const now = new Date()
      const isToday = d.toDateString() === now.toDateString()
      if (isToday) {
        timeLabel = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      } else {
        timeLabel = `${d.toLocaleDateString([], { month: 'short', day: 'numeric' })} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
      }
    } catch {
      timeLabel = item.recordedAt
    }
    return {
      ...item,
      timeLabel,
      methane: Number(item.methanePpm?.toFixed(2) || 0),
      pressure: Number(item.pressureKpa?.toFixed(2) || 0),
      oxygen: Number(item.oxygenPercent?.toFixed(2) || 0),
      riskScore: Number(item.riskScore?.toFixed(1) || 0),
    }
  })

  // Format Risk Distribution for Chart
  const readingRiskChartData = riskData?.readingRiskCounts
    ? Object.entries(riskData.readingRiskCounts).map(([key, count]) => ({
        name: key,
        count: Number(count),
        color: RISK_COLORS[key] || '#94a3b8',
      }))
    : []

  const zoneRiskChartData = riskData?.zoneRiskCounts
    ? Object.entries(riskData.zoneRiskCounts).map(([key, count]) => ({
        zone: key,
        count: Number(count),
      }))
    : []

  const latestReading = readings && readings.length > 0 ? readings[0] : null

  return (
    <div>
      <PageHeader
        eyebrow="INTELLIGENCE & TELEMETRY"
        title="Pipeline Telemetry & Safety Analytics"
        subtitle="Real-time sensor streams, historical trend analysis, risk modeling, and actuator incident logs"
      />

      {/* Tabs Bar: Live Stream vs Historical Analytics */}
      <div
        style={{
          display: 'flex',
          gap: '12px',
          borderBottom: '1px solid var(--border)',
          marginBottom: '20px',
        }}
      >
        <button
          onClick={() => setActiveTab('historical')}
          style={{
            background: 'transparent',
            border: 'none',
            borderBottom: activeTab === 'historical' ? '2px solid var(--accent)' : '2px solid transparent',
            color: activeTab === 'historical' ? 'var(--accent)' : 'var(--text-muted)',
            fontWeight: activeTab === 'historical' ? '700' : '500',
            fontSize: '15px',
            padding: '10px 16px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <TrendingUpIcon size={16} />
          <span>Historical Trends & Risk Modeling</span>
        </button>

        <button
          onClick={() => setActiveTab('live')}
          style={{
            background: 'transparent',
            border: 'none',
            borderBottom: activeTab === 'live' ? '2px solid var(--accent)' : '2px solid transparent',
            color: activeTab === 'live' ? 'var(--accent)' : 'var(--text-muted)',
            fontWeight: activeTab === 'live' ? '700' : '500',
            fontSize: '15px',
            padding: '10px 16px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <BoltIcon size={16} />
          <span>Live Real-Time Stream</span>
          <span
            style={{
              background: live ? 'rgba(34, 197, 94, 0.2)' : 'rgba(148, 163, 184, 0.2)',
              color: live ? '#22c55e' : 'var(--text-faint)',
              borderRadius: '999px',
              padding: '1px 8px',
              fontSize: '11px',
              fontWeight: 700,
            }}
          >
            {live ? 'LIVE' : 'OFFLINE'}
          </span>
        </button>
      </div>

      {error && (
        <div style={{ color: '#ef4444', marginBottom: '16px', background: 'rgba(239, 68, 68, 0.1)', padding: '10px 16px', borderRadius: '8px' }}>
          {error}
        </div>
      )}

      {/* TAB 1: LIVE STREAM VIEW */}
      {activeTab === 'live' && (
        <div>
          {/* Live Stream Quick KPI Ribbon */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '14px',
              marginBottom: '20px',
            }}
          >
            <div className="panel" style={{ padding: '16px', borderRadius: '12px' }}>
              <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600 }}>
                STREAM CONNECTION
              </div>
              <div
                style={{
                  fontSize: '20px',
                  fontWeight: 800,
                  marginTop: '6px',
                  color: live ? '#22c55e' : 'var(--text-faint)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: live ? '#22c55e' : 'var(--text-faint)' }} />
                <span>{live ? 'STOMP Active' : 'Offline'}</span>
              </div>
              <div style={{ fontSize: '11px', color: 'var(--text-faint)', marginTop: '4px' }}>
                Subscribed to /topic/readings
              </div>
            </div>

            <div className="panel" style={{ padding: '16px', borderRadius: '12px' }}>
              <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600 }}>
                LATEST METHANE READING
              </div>
              <div style={{ fontSize: '20px', fontWeight: 800, marginTop: '6px', color: '#ef4444' }}>
                {latestReading ? `${latestReading.methanePpm?.toFixed(2)} ppm` : '—'}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--text-faint)', marginTop: '4px' }}>
                Node: {latestReading?.node?.nodeCode || 'N/A'}
              </div>
            </div>

            <div className="panel" style={{ padding: '16px', borderRadius: '12px' }}>
              <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600 }}>
                LATEST LINE PRESSURE
              </div>
              <div style={{ fontSize: '20px', fontWeight: 800, marginTop: '6px', color: '#4F46E5' }}>
                {latestReading ? `${latestReading.pressureKpa?.toFixed(2)} kPa` : '—'}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--text-faint)', marginTop: '4px' }}>
                Zone: {latestReading?.node?.zone?.name || 'All'}
              </div>
            </div>

            <div className="panel" style={{ padding: '16px', borderRadius: '12px' }}>
              <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600 }}>
                LIVE RISK SCORE
              </div>
              <div
                style={{
                  fontSize: '20px',
                  fontWeight: 800,
                  marginTop: '6px',
                  color: (latestReading?.riskScore || 0) > 50 ? '#ef4444' : '#22c55e',
                }}
              >
                {latestReading ? `${latestReading.riskScore?.toFixed(1)} / 100` : '—'}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--text-faint)', marginTop: '4px' }}>
                {readings?.length || 0} buffered points
              </div>
            </div>
          </div>

          {/* Embedded Real-Time Gas Chart */}
          <div style={{ marginBottom: '20px' }}>
            <GasChart readings={readings} />
          </div>

          {/* Recent Live Data Stream Table */}
          <div
            className="panel"
            style={{
              borderRadius: '12px',
              padding: '20px',
              border: '1px solid var(--border-soft)',
            }}
          >
            <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <BoltIcon size={16} />
              <span>Incoming Live Reading Packets</span>
            </h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '12px', marginBottom: '14px' }}>
              Continuous real-time stream received from field ESP32 telemetry nodes.
            </p>

            <div style={{ overflowX: 'auto' }}>
              <table className="data-table" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px', textAlign: 'left' }}>
                <thead>
                  <tr style={{ background: 'var(--panel-2)' }}>
                    <th style={{ padding: '10px 12px', color: 'var(--text-faint)' }}>Timestamp</th>
                    <th style={{ padding: '10px 12px', color: 'var(--text-faint)' }}>Node</th>
                    <th style={{ padding: '10px 12px', color: 'var(--text-faint)' }}>Zone</th>
                    <th style={{ padding: '10px 12px', color: 'var(--text-faint)' }}>Methane (ppm)</th>
                    <th style={{ padding: '10px 12px', color: 'var(--text-faint)' }}>Pressure (kPa)</th>
                    <th style={{ padding: '10px 12px', color: 'var(--text-faint)' }}>Oxygen (%)</th>
                    <th style={{ padding: '10px 12px', color: 'var(--text-faint)' }}>Risk Score</th>
                  </tr>
                </thead>
                <tbody>
                  {readings.length === 0 ? (
                    <tr>
                      <td colSpan="7" style={{ textAlign: 'center', padding: '24px', color: 'var(--text-muted)' }}>
                        Waiting for sensor telemetry packets...
                      </td>
                    </tr>
                  ) : (
                    readings.slice(0, 15).map((r, i) => (
                      <tr key={r.id || i} style={{ borderBottom: '1px solid var(--border-soft)' }}>
                        <td style={{ padding: '10px 12px' }}>
                          {r.recordedAt ? new Date(r.recordedAt).toLocaleTimeString() : '—'}
                        </td>
                        <td style={{ padding: '10px 12px', fontWeight: 600 }}>
                          {r.node?.nodeCode || 'N/A'}
                        </td>
                        <td style={{ padding: '10px 12px' }}>
                          {r.node?.zone?.name || 'Zone'}
                        </td>
                        <td style={{ padding: '10px 12px', color: '#ef4444', fontWeight: 600 }}>
                          {r.methanePpm?.toFixed(2)} ppm
                        </td>
                        <td style={{ padding: '10px 12px', color: '#4F46E5' }}>
                          {r.pressureKpa?.toFixed(2)} kPa
                        </td>
                        <td style={{ padding: '10px 12px', color: '#22c55e' }}>
                          {r.oxygenPercent?.toFixed(2)}%
                        </td>
                        <td style={{ padding: '10px 12px' }}>
                          <span
                            style={{
                              background: r.riskScore > 50 ? 'rgba(239, 68, 68, 0.15)' : 'rgba(34, 197, 94, 0.15)',
                              color: r.riskScore > 50 ? '#ef4444' : '#22c55e',
                              padding: '2px 8px',
                              borderRadius: '4px',
                              fontWeight: 700,
                              fontSize: '11px',
                            }}
                          >
                            {r.riskScore?.toFixed(1)} / 100
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: HISTORICAL ANALYTICS & RISK MODELING */}
      {activeTab === 'historical' && (
        <div>
          {/* Top Analytics KPI Bar */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '14px',
              marginBottom: '20px',
            }}
          >
            <div className="panel" style={{ padding: '16px', borderRadius: '12px' }}>
              <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600 }}>
                SYSTEM HEALTH STATUS
              </div>
              <div
                style={{
                  fontSize: '20px',
                  fontWeight: 800,
                  marginTop: '6px',
                  color: riskData?.overallSystemStatus === 'OPTIMAL' ? '#22c55e' : '#f59e0b',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: riskData?.overallSystemStatus === 'OPTIMAL' ? '#22c55e' : '#f59e0b' }} />
                <span>{riskData?.overallSystemStatus || 'OPTIMAL'}</span>
              </div>
              <div style={{ fontSize: '11px', color: 'var(--text-faint)', marginTop: '4px' }}>
                Continuous multi-zone assessment
              </div>
            </div>

            <div className="panel" style={{ padding: '16px', borderRadius: '12px' }}>
              <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600 }}>
                AVERAGE RISK INDEX
              </div>
              <div
                style={{
                  fontSize: '20px',
                  fontWeight: 800,
                  marginTop: '6px',
                  color: (riskData?.averageRiskScore || 0) > 50 ? '#ef4444' : '#4F46E5',
                }}
              >
                {riskData?.averageRiskScore != null ? `${riskData.averageRiskScore.toFixed(1)} / 100` : '0 / 100'}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--text-faint)', marginTop: '4px' }}>
                Weighted risk score across active nodes
              </div>
            </div>

            <div className="panel" style={{ padding: '16px', borderRadius: '12px' }}>
              <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600 }}>
                TOTAL VALVE ACTIONS
              </div>
              <div style={{ fontSize: '20px', fontWeight: 800, marginTop: '6px', color: 'var(--accent)' }}>
                {valveStats?.totalActions || 0} Events
              </div>
              <div style={{ fontSize: '11px', color: 'var(--text-faint)', marginTop: '4px' }}>
                {valveStats?.autoCount || 0} automated safety trips · {valveStats?.manualCount || 0} manual
              </div>
            </div>

            <div className="panel" style={{ padding: '16px', borderRadius: '12px' }}>
              <div style={{ color: 'var(--text-muted)', fontSize: '12px', fontWeight: 600 }}>
                ANOMALY DETECTION
              </div>
              <div style={{ fontSize: '20px', fontWeight: 800, marginTop: '6px', color: '#22c55e' }}>
                Active AI Sentry
              </div>
              <div style={{ fontSize: '11px', color: 'var(--text-faint)', marginTop: '4px' }}>
                Spike, flat-line, and creep engines online
              </div>
            </div>
          </div>

          {/* Sensor Trends Section */}
          <div
            className="panel"
            style={{
              borderRadius: '12px',
              padding: '20px',
              marginBottom: '20px',
              border: '1px solid var(--border-soft)',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '12px',
                marginBottom: '16px',
              }}
            >
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <TrendingUpIcon size={16} />
                  <span>Multi-Sensor Telemetry Trends</span>
                </h3>
                <p style={{ color: 'var(--text-muted)', fontSize: '12px', margin: '4px 0 0' }}>
                  Continuous time-series tracking of gas ppm, line pressure, oxygen displacement, and risk score.
                </p>
              </div>

              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
                {/* Node Select */}
                <select
                  value={selectedNodeId}
                  onChange={(e) => setSelectedNodeId(e.target.value)}
                  style={compactSelect}
                >
                  <option value="">All Sensor Nodes</option>
                  {nodes?.map((n) => (
                    <option key={n.id} value={n.id}>
                      {n.nodeCode} ({n.zone?.name || 'Unassigned'})
                    </option>
                  ))}
                </select>

                {/* Hours Select */}
                <select
                  value={trendHours}
                  onChange={(e) => setTrendHours(Number(e.target.value))}
                  style={compactSelect}
                >
                  <option value="6">Last 6 Hours</option>
                  <option value="12">Last 12 Hours</option>
                  <option value="24">Last 24 Hours</option>
                  <option value="48">Last 48 Hours</option>
                  <option value="168">Last 7 Days</option>
                  <option value="720">Last 30 Days</option>
                </select>

                {/* Metric Filter */}
                <select
                  value={trendMetric}
                  onChange={(e) => setTrendMetric(e.target.value)}
                  style={compactSelect}
                >
                  <option value="all">Show All Metrics</option>
                  <option value="methane">Methane (ppm) Only</option>
                  <option value="pressure">Pressure (kPa) Only</option>
                  <option value="oxygen">Oxygen (%) Only</option>
                  <option value="risk">Risk Score Only</option>
                </select>

                <button
                  onClick={loadAllAnalytics}
                  disabled={loading}
                  style={{
                    background: 'var(--panel-2)',
                    color: 'var(--text)',
                    border: '1px solid var(--border-input)',
                    borderRadius: '6px',
                    padding: '6px 12px',
                    fontSize: '12px',
                    cursor: loading ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                  }}
                  title="Refresh telemetry analytics"
                >
                  <RefreshIcon size={13} />
                  <span>{loading ? 'Updating...' : 'Refresh'}</span>
                </button>
              </div>
            </div>

            <div style={{ width: '100%', height: 320 }}>
              {formattedTrends.length === 0 ? (
                <div style={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '10px', color: 'var(--text-muted)' }}>
                  <div>No telemetry readings recorded in the last {trendHours >= 24 ? `${trendHours / 24}d` : `${trendHours}h`}.</div>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button
                      onClick={() => setTrendHours(48)}
                      style={{
                        background: 'transparent',
                        color: 'var(--accent)',
                        border: '1.5px solid var(--accent)',
                        borderRadius: '6px',
                        padding: '6px 14px',
                        fontSize: '12px',
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                      onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(79, 70, 229, 0.08)' }}
                      onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent' }}
                    >
                      View Last 48 Hours
                    </button>
                    <button
                      onClick={() => setTrendHours(168)}
                      style={{
                        background: 'transparent',
                        color: 'var(--text)',
                        border: '1.5px solid var(--border)',
                        borderRadius: '6px',
                        padding: '6px 14px',
                        fontSize: '12px',
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                      onMouseEnter={(e) => { e.currentTarget.style.background = 'var(--hover)' }}
                      onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent' }}
                    >
                      View Last 7 Days
                    </button>
                  </div>
                </div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={formattedTrends} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border-soft)" />
                    <XAxis dataKey="timeLabel" stroke="var(--text-faint)" fontSize={11} />
                    <YAxis stroke="var(--text-faint)" fontSize={11} />
                    <Tooltip
                      contentStyle={{
                        background: 'var(--panel)',
                        border: '1px solid var(--border)',
                        borderRadius: '8px',
                        color: 'var(--text)',
                        fontSize: '12px',
                      }}
                    />
                    <Legend wrapperStyle={{ fontSize: '12px' }} />
                    {(trendMetric === 'all' || trendMetric === 'methane') && (
                      <ReferenceLine
                        y={200}
                        stroke="#f59e0b"
                        strokeDasharray="4 4"
                        label={{ value: 'Warn: 200 ppm', fill: '#f59e0b', fontSize: 10, position: 'insideTopRight' }}
                      />
                    )}
                    {(trendMetric === 'all' || trendMetric === 'methane') && (
                      <ReferenceLine
                        y={350}
                        stroke="#ef4444"
                        strokeDasharray="4 4"
                        label={{ value: 'Crit: 350 ppm', fill: '#ef4444', fontSize: 10, position: 'insideTopRight' }}
                      />
                    )}
                    {trendMetric === 'pressure' && (
                      <ReferenceLine
                        y={420}
                        stroke="#ef4444"
                        strokeDasharray="4 4"
                        label={{ value: 'Low Crit: 420 kPa', fill: '#ef4444', fontSize: 10, position: 'insideTopRight' }}
                      />
                    )}
                    {trendMetric === 'pressure' && (
                      <ReferenceLine
                        y={440}
                        stroke="#f59e0b"
                        strokeDasharray="4 4"
                        label={{ value: 'Low Warn: 440 kPa', fill: '#f59e0b', fontSize: 10, position: 'insideTopRight' }}
                      />
                    )}
                    {trendMetric === 'oxygen' && (
                      <ReferenceLine
                        y={19.5}
                        stroke="#ef4444"
                        strokeDasharray="4 4"
                        label={{ value: 'Min Safe: 19.5%', fill: '#ef4444', fontSize: 10, position: 'insideTopRight' }}
                      />
                    )}
                    {(trendMetric === 'all' || trendMetric === 'methane') && (
                      <Line type="monotone" dataKey="methane" name="Methane (ppm)" stroke="#ef4444" strokeWidth={2} dot={false} />
                    )}
                    {(trendMetric === 'all' || trendMetric === 'pressure') && (
                      <Line type="monotone" dataKey="pressure" name="Pressure (kPa)" stroke="#6366f1" strokeWidth={2} dot={false} />
                    )}
                    {(trendMetric === 'all' || trendMetric === 'oxygen') && (
                      <Line type="monotone" dataKey="oxygen" name="Oxygen (%)" stroke="#22c55e" strokeWidth={2} dot={false} />
                    )}
                    {(trendMetric === 'all' || trendMetric === 'risk') && (
                      <Line type="monotone" dataKey="riskScore" name="Risk Score (0-100)" stroke="#f59e0b" strokeWidth={2} strokeDasharray="4 4" dot={false} />
                    )}
                  </LineChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>

          {/* Dedicated Sub-Charts: Pressure & Oxygen Channels */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
              gap: '20px',
              marginBottom: '20px',
            }}
          >
            {/* Dedicated Pressure Trend Chart */}
            <div className="panel" style={{ borderRadius: '12px', padding: '20px', border: '1px solid var(--border-soft)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <h3 style={{ fontSize: '15px', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <GaugeIcon size={16} />
                  <span>Dedicated Line Pressure Channel (kPa)</span>
                </h3>
                <span style={{ fontSize: '11px', color: '#4F46E5', fontWeight: 700 }}>MPX5700DP / HX710B</span>
              </div>
              <p style={{ color: 'var(--text-muted)', fontSize: '12px', marginBottom: '14px' }}>
                Line pressure decay tracking with calibrated Warning (440 kPa) & Critical (420 kPa) isolation limits.
              </p>
              <div style={{ width: '100%', height: 220 }}>
                {formattedTrends.length === 0 ? (
                  <div style={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '8px', color: 'var(--text-muted)' }}>
                    <span>No pressure telemetry in active timeframe.</span>
                    <button
                      onClick={() => setTrendHours(48)}
                      style={{
                        background: 'transparent',
                        color: 'var(--accent)',
                        border: '1.5px solid var(--accent)',
                        borderRadius: '6px',
                        padding: '4px 10px',
                        fontSize: '11px',
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                      onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(79, 70, 229, 0.08)' }}
                      onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent' }}
                    >
                      Switch to 48 Hours
                    </button>
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={formattedTrends} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border-soft)" />
                      <XAxis dataKey="timeLabel" stroke="var(--text-faint)" fontSize={11} />
                      <YAxis stroke="var(--text-faint)" domain={['dataMin - 10', 'dataMax + 10']} fontSize={11} />
                      <Tooltip contentStyle={{ background: 'var(--panel)', border: '1px solid var(--border)', borderRadius: '8px', fontSize: '12px' }} />
                      <ReferenceLine y={440} stroke="#f59e0b" strokeDasharray="3 3" label={{ value: 'Warn: 440 kPa', fill: '#f59e0b', fontSize: 10, position: 'insideTopRight' }} />
                      <ReferenceLine y={420} stroke="#ef4444" strokeDasharray="3 3" label={{ value: 'Crit: 420 kPa', fill: '#ef4444', fontSize: 10, position: 'insideTopRight' }} />
                      <Line type="monotone" dataKey="pressure" name="Pressure (kPa)" stroke="#6366f1" strokeWidth={2.2} dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </div>
            </div>

            {/* Dedicated Oxygen Trend Chart */}
            <div className="panel" style={{ borderRadius: '12px', padding: '20px', border: '1px solid var(--border-soft)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <h3 style={{ fontSize: '15px', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <WindIcon size={16} />
                  <span>Atmospheric Oxygen Displacement (%)</span>
                </h3>
                <span style={{ fontSize: '11px', color: '#22c55e', fontWeight: 700 }}>ME2-O2 Sensor</span>
              </div>
              <p style={{ color: 'var(--text-muted)', fontSize: '12px', marginBottom: '14px' }}>
                Ambient oxygen fraction monitoring with OSHA asphyxiation threshold reference (19.5% minimum safe).
              </p>
              <div style={{ width: '100%', height: 220 }}>
                {formattedTrends.length === 0 ? (
                  <div style={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '8px', color: 'var(--text-muted)' }}>
                    <span>No oxygen telemetry in active timeframe.</span>
                    <button
                      onClick={() => setTrendHours(48)}
                      style={{
                        background: 'transparent',
                        color: 'var(--accent)',
                        border: '1.5px solid var(--accent)',
                        borderRadius: '6px',
                        padding: '4px 10px',
                        fontSize: '11px',
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                      onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(79, 70, 229, 0.08)' }}
                      onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent' }}
                    >
                      Switch to 48 Hours
                    </button>
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={formattedTrends} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border-soft)" />
                      <XAxis dataKey="timeLabel" stroke="var(--text-faint)" fontSize={11} />
                      <YAxis stroke="var(--text-faint)" domain={[15, 25]} fontSize={11} />
                      <Tooltip contentStyle={{ background: 'var(--panel)', border: '1px solid var(--border)', borderRadius: '8px', fontSize: '12px' }} />
                      <ReferenceLine y={19.5} stroke="#ef4444" strokeDasharray="3 3" label={{ value: 'Min Safe: 19.5%', fill: '#ef4444', fontSize: 10, position: 'insideTopRight' }} />
                      <Line type="monotone" dataKey="oxygen" name="Oxygen (%)" stroke="#22c55e" strokeWidth={2.2} dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </div>
            </div>
          </div>

          {/* Row: Zone Risk Gauges & Battery Trend */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
              gap: '20px',
              marginBottom: '20px',
            }}
          >
            {/* Zone Risk Gauges */}
            <div className="panel" style={{ borderRadius: '12px', padding: '20px', border: '1px solid var(--border-soft)' }}>
              <h3 style={{ fontSize: '15px', fontWeight: 700, marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <TargetIcon size={16} />
                <span>Zone Dynamic Risk Gauges</span>
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '12px', marginBottom: '16px' }}>
                Automated multi-factor risk scoring per physical pipeline sector.
              </p>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '12px' }}>
                {nodes?.reduce((acc, n) => {
                  const zName = n.zone?.name || 'Unassigned'
                  if (!acc.find(item => item.name === zName)) {
                    acc.push({ name: zName, riskLevel: n.zone?.riskLevel || 'SAFE' })
                  }
                  return acc
                }, []).map((z) => {
                  const riskColor = RISK_COLORS[z.riskLevel] || '#22c55e'
                  const riskScoreEstimate = z.riskLevel === 'CRITICAL' ? 95 : z.riskLevel === 'HIGH' ? 75 : z.riskLevel === 'MODERATE' ? 45 : z.riskLevel === 'LOW' ? 25 : 8
                  return (
                    <div
                      key={z.name}
                      style={{
                        background: 'var(--panel-2)',
                        border: `1px solid ${riskColor}40`,
                        borderRadius: '10px',
                        padding: '14px 10px',
                        textAlign: 'center',
                      }}
                    >
                      <div style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text)' }}>{z.name}</div>
                      <div style={{ fontSize: '22px', fontWeight: 900, color: riskColor, margin: '6px 0' }}>
                        {riskScoreEstimate}%
                      </div>
                      <div
                        style={{
                          display: 'inline-block',
                          fontSize: '10px',
                          fontWeight: 700,
                          color: riskColor,
                          background: `${riskColor}20`,
                          padding: '2px 8px',
                          borderRadius: '999px',
                        }}
                      >
                        {z.riskLevel}
                      </div>
                      <div style={{ width: '100%', height: '6px', background: 'rgba(255,255,255,0.08)', borderRadius: '3px', marginTop: '10px', overflow: 'hidden' }}>
                        <div style={{ width: `${riskScoreEstimate}%`, height: '100%', background: riskColor, borderRadius: '3px' }} />
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>

            {/* Battery Trend & Node Health */}
            <div className="panel" style={{ borderRadius: '12px', padding: '20px', border: '1px solid var(--border-soft)' }}>
              <h3 style={{ fontSize: '15px', fontWeight: 700, marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <BatteryIcon size={16} />
                <span>Node Battery Health & Charge Monitor</span>
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '12px', marginBottom: '14px' }}>
                LiFePO4 field battery telemetry with low (20%) and critical (10%) threshold alerts.
              </p>
              <div style={{ width: '100%', height: 200 }}>
                {nodes?.length === 0 ? (
                  <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
                    No sensor nodes registered.
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      data={nodes?.map(n => ({
                        nodeCode: n.nodeCode,
                        battery: n.batteryLevel != null ? n.batteryLevel : 100,
                      }))}
                      margin={{ top: 10, right: 20, left: 0, bottom: 0 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border-soft)" />
                      <XAxis dataKey="nodeCode" stroke="var(--text-faint)" fontSize={11} />
                      <YAxis stroke="var(--text-faint)" domain={[0, 100]} fontSize={11} unit="%" />
                      <Tooltip contentStyle={{ background: 'var(--panel)', border: '1px solid var(--border)', borderRadius: '8px', fontSize: '12px' }} />
                      <ReferenceLine y={20} stroke="#f59e0b" strokeDasharray="3 3" label={{ value: 'Low: 20%', fill: '#f59e0b', fontSize: 10, position: 'insideTopRight' }} />
                      <ReferenceLine y={10} stroke="#ef4444" strokeDasharray="3 3" label={{ value: 'Crit: 10%', fill: '#ef4444', fontSize: 10, position: 'insideTopRight' }} />
                      <Bar dataKey="battery" name="Battery %" radius={[4, 4, 0, 0]}>
                        {nodes?.map((entry, index) => {
                          const val = entry.batteryLevel != null ? entry.batteryLevel : 100
                          const color = val <= 10 ? '#ef4444' : val <= 20 ? '#f59e0b' : '#22c55e'
                          return <Cell key={`battery-cell-${index}`} fill={color} />
                        })}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </div>
            </div>
          </div>

          {/* Row: Risk Distribution & Daily Alerts Timeline */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
              gap: '20px',
              marginBottom: '20px',
            }}
          >
            {/* Risk Level Distribution */}
            <div className="panel" style={{ borderRadius: '12px', padding: '20px', border: '1px solid var(--border-soft)' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ShieldIcon size={16} />
                <span>Reading Risk Category Distribution</span>
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '12px', marginBottom: '14px' }}>
                Distribution of telemetry data across automated risk classification bands.
              </p>

              <div style={{ width: '100%', height: 260 }}>
                {readingRiskChartData.length === 0 ? (
                  <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
                    No risk data recorded.
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={readingRiskChartData} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border-soft)" />
                      <XAxis dataKey="name" stroke="var(--text-faint)" fontSize={11} />
                      <YAxis stroke="var(--text-faint)" fontSize={11} />
                      <Tooltip
                        contentStyle={{
                          background: 'var(--panel)',
                          border: '1px solid var(--border)',
                          borderRadius: '8px',
                          color: 'var(--text)',
                          fontSize: '12px',
                        }}
                      />
                      <Bar dataKey="count" name="Readings Count" radius={[4, 4, 0, 0]}>
                        {readingRiskChartData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </div>

              {zoneRiskChartData.length > 0 && (
                <div style={{ marginTop: '14px', paddingTop: '10px', borderTop: '1px solid var(--border-soft)' }}>
                  <div style={{ fontSize: '11px', color: 'var(--text-faint)', marginBottom: '6px' }}>
                    Zone Risk Occurrences:
                  </div>
                  <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                    {zoneRiskChartData.map((z) => (
                      <span
                        key={z.zone}
                        style={{
                          background: 'var(--panel-2)',
                          padding: '3px 8px',
                          borderRadius: '6px',
                          fontSize: '11px',
                          color: 'var(--text-muted)',
                          border: '1px solid var(--border-soft)',
                        }}
                      >
                        {z.zone}: <strong style={{ color: 'var(--text)' }}>{z.count}</strong>
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Daily Alerts Timeline */}
            <div className="panel" style={{ borderRadius: '12px', padding: '20px', border: '1px solid var(--border-soft)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <h3 style={{ fontSize: '16px', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <CalendarIcon size={16} />
                  <span>Alert Frequency Timeline</span>
                </h3>
                <select
                  value={timelineDays}
                  onChange={(e) => setTimelineDays(Number(e.target.value))}
                  style={compactSelect}
                >
                  <option value="7">Last 7 Days</option>
                  <option value="14">Last 14 Days</option>
                  <option value="30">Last 30 Days</option>
                </select>
              </div>
              <p style={{ color: 'var(--text-muted)', fontSize: '12px', marginBottom: '14px' }}>
                Daily critical, warning, and normal operational events across all zones.
              </p>

              <div style={{ width: '100%', height: 260 }}>
                {timeline.length === 0 ? (
                  <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
                    No alerts recorded in this timeline.
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={timeline} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border-soft)" />
                      <XAxis dataKey="date" stroke="var(--text-faint)" fontSize={11} />
                      <YAxis stroke="var(--text-faint)" fontSize={11} />
                      <Tooltip
                        contentStyle={{
                          background: 'var(--panel)',
                          border: '1px solid var(--border)',
                          borderRadius: '8px',
                          color: 'var(--text)',
                          fontSize: '12px',
                        }}
                      />
                      <Legend wrapperStyle={{ fontSize: '12px' }} />
                      <Bar dataKey="criticalCount" name="Critical" fill="#ef4444" stackId="a" radius={[0, 0, 0, 0]} />
                      <Bar dataKey="warningCount" name="Warning" fill="#f59e0b" stackId="a" radius={[0, 0, 0, 0]} />
                      <Bar dataKey="infoCount" name="Normal" fill="#4F46E5" stackId="a" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </div>
            </div>
          </div>

          {/* Row: Alerts by Sensor & Valve Safety Operations */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
              gap: '20px',
            }}
          >
            {/* Sensor Share Pie Chart */}
            <div className="panel" style={{ borderRadius: '12px', padding: '20px', border: '1px solid var(--border-soft)' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <MicroscopeIcon size={16} />
                <span>Incident Breakdown by Sensor Type</span>
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '12px', marginBottom: '14px' }}>
                Share of incidents triggered by gas, pressure, oxygen, or battery telemetry.
              </p>

              <div style={{ width: '100%', height: 260 }}>
                {sensorDist.length === 0 ? (
                  <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
                    No sensor incident statistics available.
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={sensorDist}
                        dataKey="count"
                        nameKey="sensor"
                        cx="50%"
                        cy="50%"
                        outerRadius={80}
                        innerRadius={45}
                        paddingAngle={3}
                        label={({ sensor, percentage }) => `${sensor}: ${percentage}%`}
                      >
                        {sensorDist.map((entry, index) => (
                          <Cell key={`pie-cell-${index}`} fill={getSensorSliceColor(entry.sensor, index)} />
                        ))}
                      </Pie>
                      <Tooltip
                        contentStyle={{
                          background: 'var(--panel)',
                          border: '1px solid var(--border)',
                          borderRadius: '8px',
                          color: 'var(--text)',
                          fontSize: '12px',
                        }}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </div>

              {/* Sensor Legend Badges */}
              {sensorDist.length > 0 && (
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', justifyContent: 'center', marginTop: '14px', paddingTop: '10px', borderTop: '1px solid var(--border-soft)' }}>
                  {sensorDist.map((entry, index) => {
                    const color = getSensorSliceColor(entry.sensor, index)
                    return (
                      <span
                        key={entry.sensor}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '6px',
                          fontSize: '11px',
                          color: 'var(--text-muted)',
                          background: 'var(--panel-2)',
                          padding: '3px 8px',
                          borderRadius: '6px',
                          border: '1px solid var(--border-soft)',
                        }}
                      >
                        <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: color }} />
                        <span>{entry.sensor}: <strong style={{ color: 'var(--text)' }}>{entry.percentage}%</strong></span>
                      </span>
                    )
                  })}
                </div>
              )}
            </div>

            {/* Valve Activity & Incident Isolation */}
            <div className="panel" style={{ borderRadius: '12px', padding: '20px', border: '1px solid var(--border-soft)' }}>
              <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ValveIcon size={16} />
                <span>Valve Isolation & Actuator Stats</span>
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '12px', marginBottom: '14px' }}>
                Automated emergency trips vs manual supervisor actions.
              </p>

              {/* Mini metrics bar */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(4, 1fr)',
                  gap: '8px',
                  marginBottom: '16px',
                  textAlign: 'center',
                }}
              >
                <div style={statBox}>
                  <div style={{ fontSize: '11px', color: 'var(--text-faint)' }}>Total</div>
                  <div style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text)' }}>
                    {valveStats?.totalActions || 0}
                  </div>
                </div>
                <div style={statBox}>
                  <div style={{ fontSize: '11px', color: 'var(--text-faint)' }}>Auto Close</div>
                  <div style={{ fontSize: '16px', fontWeight: 700, color: '#ef4444' }}>
                    {valveStats?.autoCount || 0}
                  </div>
                </div>
                <div style={statBox}>
                  <div style={{ fontSize: '11px', color: 'var(--text-faint)' }}>Manual</div>
                  <div style={{ fontSize: '16px', fontWeight: 700, color: '#4F46E5' }}>
                    {valveStats?.manualCount || 0}
                  </div>
                </div>
                <div style={statBox}>
                  <div style={{ fontSize: '11px', color: 'var(--text-faint)' }}>Open/Close</div>
                  <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text)' }}>
                    {valveStats?.openCount || 0}/{valveStats?.closeCount || 0}
                  </div>
                </div>
              </div>

              {/* Recent Operations Log */}
              <div style={{ maxHeight: '180px', overflowY: 'auto' }}>
                <h4 style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '8px', textTransform: 'uppercase' }}>
                  Recent Actuator Audits
                </h4>
                {(!valveStats?.recentActions || valveStats.recentActions.length === 0) ? (
                  <p style={{ color: 'var(--text-faint)', fontSize: '12px' }}>No recent valve actuations recorded.</p>
                ) : (
                  valveStats.recentActions.slice(0, 5).map((act) => (
                    <div
                      key={act.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '8px 0',
                        borderBottom: '1px solid var(--border-soft)',
                        fontSize: '12px',
                      }}
                    >
                      <div>
                        <strong style={{ color: act.action === 'CLOSE' ? '#ef4444' : '#22c55e', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                          {act.action === 'CLOSE' ? <LockIcon size={12} color="#ef4444" /> : <UnlockIcon size={12} color="#22c55e" />}
                          <span>{act.action === 'CLOSE' ? 'CLOSED' : 'OPENED'}</span>
                        </strong>{' '}
                        <span>{act.zoneName}</span>
                        <div style={{ fontSize: '11px', color: 'var(--text-faint)' }}>
                          {act.reason || 'Safety response'}
                        </div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <span
                          style={{
                            background: act.triggeredBy === 'SYSTEM' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(79, 70, 229, 0.15)',
                            color: act.triggeredBy === 'SYSTEM' ? '#ef4444' : '#4F46E5',
                            padding: '2px 6px',
                            borderRadius: '4px',
                            fontSize: '10px',
                            fontWeight: 600,
                          }}
                        >
                          {act.triggeredBy}
                        </span>
                        <div style={{ fontSize: '10px', color: 'var(--text-faint)', marginTop: '2px' }}>
                          {act.performedAt ? new Date(act.performedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

const compactSelect = {
  background: 'var(--panel-2)',
  color: 'var(--text)',
  border: '1px solid var(--border-input)',
  borderRadius: '6px',
  padding: '6px 10px',
  fontSize: '12px',
}

const statBox = {
  background: 'var(--panel-2)',
  borderRadius: '8px',
  padding: '8px 4px',
  border: '1px solid var(--border-soft)',
}
